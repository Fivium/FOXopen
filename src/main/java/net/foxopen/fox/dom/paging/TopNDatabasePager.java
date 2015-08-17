package net.foxopen.fox.dom.paging;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.database.sql.QueryResultDeliverer;
import net.foxopen.fox.database.sql.bind.CachedBindObjectProvider;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dbinterface.StatementExecutionBindOptions;
import net.foxopen.fox.dbinterface.deliverer.InterfaceQueryResultDeliverer;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.style.PageControlStyle;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBFlashback;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.track.Track;

/**
 * A Pager which runs Top-N queries.
 */
public class TopNDatabasePager
extends DatabasePager {

  private static final ParsedStatement SCN_STATEMENT = StatementParser.parseSafely("SELECT current_scn FROM V$DATABASE", "Get SCN");

  private final int mLookAheadPages;

  private final String mInterfaceName;
  private final String mQueryName;

  //TODO make configurable
  private final boolean mCacheBinds = true;

  private CachedBindObjectProvider mCachedBindObjectProvider = null;

  /** Will be true if the SCN needs to be bound in to the paged query. */
  private final boolean mSCNRequired;
  /** SCN for AS OF queries. This is set to the current SCN on first run and remains the same on subsequent page gets to provide read consistency for the pager. */
  private String mAsOfSCN = "";

  TopNDatabasePager(String pPagerKey, String pModuleCallId, EvaluatedPagerSetup pEvalPagerSetup, InterfaceQuery pInterfaceQuery, Mod pModule) {
    super(pPagerKey, pModuleCallId, pEvalPagerSetup);
    mInterfaceName = pInterfaceQuery.getDBInterfaceName();
    mQueryName = pInterfaceQuery.getStatementName();

    mSCNRequired = !XFUtil.isNull(pInterfaceQuery.getTopNPaginationConfig().getSCNBindName());

    PageControlStyle lControlStyle = getPageControlStyle(pModule);
    mLookAheadPages = lControlStyle.getPageDisplayScope();
  }

  @Override
  protected void populatePage(ActionRequestContext pRequestContext, DOM pTargetNode) {

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Get page for query " + mQueryName);
    try {
      //Locate the match node
      DOM lMatchNode = pRequestContext.getContextUElem().getElemByRef(mMatchFoxId);

      InterfaceQuery lInterfaceQuery = pRequestContext.getCurrentModule().getDatabaseInterface(mInterfaceName).getInterfaceQuery(mQueryName);

      //Run the query to get the desired page
      try {
        try {
          setupAndExecute(pRequestContext, lInterfaceQuery, lMatchNode, lUCon);
        }
        catch (ExDBFlashback e) {
          //Snapshot too old/not defined: re-run with the latest SCN
          Track.alert("TopNFlashback", "TopN flashback failed for query " + mQueryName + "; refreshing SCN");
          setSystemChangeNumber(lUCon);
          setupAndExecute(pRequestContext, lInterfaceQuery, lMatchNode, lUCon);
        }
      }
      catch (ExDB e) {
        throw new ExInternal("Failed to get page for query " + mQueryName, e);
      }

      //No rows returned - paged data has mutated on database. Recursively seek the nearest available page.
      while(lMatchNode.getChildElements().size() == 0 && getCurrentPage() > 1) {
        Track.pushInfo("TopNPagerPageSeek", "Seeking populated page - trying page " + (getCurrentPage() - 1));
        try {
          setCurrentPage(getCurrentPage() - 1);
          populatePage(pRequestContext, lMatchNode);
          //Adjust row count - new row count is number of rows on previous pages plus rows on this page
          setRowCount(getCurrentPageStartRowNum() - 1 + lMatchNode.getChildElements().size());
        }
        finally {
          Track.pop("TopNPagerPageSeek");
        }
      }
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Get page for query " + mQueryName);
    }
  }

  private void setupAndExecute(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, DOM pMatchNode, UCon pUCon)
  throws ExDB {
    //Set up result deliverer and bind provider
    QueryResultDeliverer lDeliverer = TopNPaginationResultDeliverer.createResultDeliverer(pRequestContext, pInterfaceQuery, pMatchNode, this, false);

    StatementExecutionBindOptions lBindOptions = new StatementExecutionBindOptions() {
      @Override public CachedBindObjectProvider getCachedBindObjectProvider() { return mCachedBindObjectProvider; }
      @Override public DecoratingBindObjectProvider getDecoratingBindObjectProvider() { return getDecoratingBindProviderOrNull(pInterfaceQuery); }
    };

    //Run the query
    pInterfaceQuery.executeStatement(pRequestContext, pMatchNode, pUCon, lBindOptions, lDeliverer);
  }

  @Override
  protected void resetPagerInternal(ActionRequestContext pRequestContext, DOM pMatchDOM) {
  }

  @Override
  public InterfaceQueryResultDeliverer createResultDeliverer(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, DOM pMatchNode) {
    return TopNPaginationResultDeliverer.createResultDeliverer(pRequestContext, pInterfaceQuery, pMatchNode, this, true);
  }

  @Override
  public DecoratingBindObjectProvider getDecoratingBindProviderOrNull(InterfaceQuery pInterfaceQuery) {
    return new TopNPaginationBindProvider(pInterfaceQuery.getTopNPaginationConfig(), this);
  }

  @Override
  public boolean allowsCachedBindVariables() {
    return mCacheBinds;
  }

  @Override
  public void setCachedBindVariables(PersistenceContext pPersistenceContext, CachedBindObjectProvider pCachedBindVariables) {
    if(pCachedBindVariables == null) {
      throw new ExInternal("Cannot set cached bind objects to a null value");
    }

    mCachedBindObjectProvider = pCachedBindVariables;

    pPersistenceContext.requiresPersisting(this, PersistenceMethod.UPDATE);
  }

  public int getQueryBindRowStart() {
    return getCurrentPageStartRowNum();
  }

  public int getQueryBindRowEnd() {
    return getCurrentPageEndRowNum() + getLookAheadRows();
  }

  public int getLookAheadRows() {
    return mLookAheadPages * getPageSize() + 1;
  }

  /**
   * Sets the AS OF SCN on this pager to the current SCN, if required.
   * @param pUCon UCon to read SCN with.
   */
  void setSystemChangeNumber(UCon pUCon) {
    if(mSCNRequired) {
      try {
        mAsOfSCN = pUCon.queryScalarString(SCN_STATEMENT);
      }
      catch (ExDB e) {
        throw new ExInternal("Pager for query " + mQueryName +  " failed to retrieve System Change Number", e);
      }
    }
  }

  public String getAsOfSCN() {
    return mAsOfSCN;
  }
}
