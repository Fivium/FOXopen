package net.foxopen.fox.dom.paging;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.bind.CachedBindObjectProvider;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dbinterface.deliverer.InterfaceQueryResultDeliverer;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.track.Track;

/**
 * A pager which performs paging on the database and does not require all paged data to be read into a DOM.
 */
public abstract class DatabasePager
extends Pager {

  /**
   * Creates a database pager. If the given InterfaceQuery is configured for Top-N paging a Top-N Pager is returned.
   * Otherwise a caching database pager is returned.
   * @param pPagerKey Key of the new pager.
   * @param pModuleCallId Module call ID which the pager is associated with.
   * @param pEvalPagerSetup Evaluated match ID/page size etc.
   * @param pInterfaceQuery Query being run in a paged manner.
   * @param pModule Current module.
   * @return A new DatabasePager.
   */
  public static DatabasePager createDatabasePager(String pPagerKey, String pModuleCallId, EvaluatedPagerSetup pEvalPagerSetup,
                                                  InterfaceQuery pInterfaceQuery, Mod pModule) {
    if(pInterfaceQuery.getTopNPaginationConfig() != null) {
      return new TopNDatabasePager(pPagerKey, pModuleCallId, pEvalPagerSetup, pInterfaceQuery, pModule);
    }
    else {
      return new CachingDatabasePager(pPagerKey, pModuleCallId, pEvalPagerSetup);
    }
  }

  protected DatabasePager(String pPagerKey, String pModuleCallId, EvaluatedPagerSetup pEvalPagerSetup) {
    super(pPagerKey, pModuleCallId, pEvalPagerSetup);
    if(XFUtil.isNull(mMatchFoxId)) {
      throw new ExInternal("Match ID cannot be null for a DatabasePager");
    }
  }

  /**
   * Creates a ResultDeliverer to handle query result processing for this pager.
   * @param pRequestContext
   * @param pInterfaceQuery Query to be executed.
   * @param pMatchNode Query match node.
   * @return
   */
  public abstract InterfaceQueryResultDeliverer createResultDeliverer(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, DOM pMatchNode);

  /**
   * Creates a BindObjectProvider which will be used to decorate the standard dbinterface query bind provider. If this
   * pager implementation does not provide any special binds, this method should return null.
   * @param pInterfaceQuery Query to be executed.
   * @return Bind provider or null.
   */
  public abstract DecoratingBindObjectProvider getDecoratingBindProviderOrNull(InterfaceQuery pInterfaceQuery);

  /**
   * Default action run before switching page. Removes all children from the match node.
   * @param pMatchDOM matched DOM node
   */
  protected void prePageDefaultAction(DOM pMatchDOM) {
    pMatchDOM.removeAllChildren();
  }

  /**
   * Default action run after switching page.
   * @param pMatchDOM matched DOM node
   */
  @Override
  protected void postPageDefaultAction(DOM pMatchDOM) {
  }

  protected abstract void populatePage(ActionRequestContext pRequestContext, DOM pTargetNode);

  @Override
  protected void goToPageInternal(ActionRequestContext pRequestContext, int pPageNum, DOM pMatchDOM) {

    Track.pushInfo("ReadPage", "Page number " + pPageNum);
    try {
      populatePage(pRequestContext, pMatchDOM);
    }
    finally {
      Track.pop("ReadPage");
    }
  }

  @Override
  public void runPostPageAction(ActionRequestContext pRequestContext, DOM pMatchDOM) {
    //Overload the post page action to update the sys dom
    updateSysDOM(pRequestContext);
    super.runPostPageAction(pRequestContext, pMatchDOM);
  }

  /**
   * Tests if this pager allows bind variables to be cached on it.
   * @return True if bind variable caching is allowed.
   */
  public boolean allowsCachedBindVariables() {
    return false;
  }

  /**
   * Sets a CachedBindObjectProvider for this pager, which can be used when the page is changed to re-run the query
   * with the original bind variables. This method throws an exception if it is invoked when {@link #allowsCachedBindVariables}
   * returns false.
   * @param pPersistenceContext Required for marking the pager as modified.
   * @param pCachedBindVariables Non-null CachedBindObjectProvider.
   */
  public void setCachedBindVariables(PersistenceContext pPersistenceContext, CachedBindObjectProvider pCachedBindVariables) {
    throw new ExInternal("Cannot set cached bind variables on a " + getClass().getName());
  }

  private void updateSysDOM(ActionRequestContext pRequestContext) {
    pRequestContext.addSysDOMInfo("sqlquery/paging/totalrowcount", String.valueOf(getRowCount()));
    pRequestContext.addSysDOMInfo("sqlquery/paging/retrievedrowcount", String.valueOf(getCurrentPage() * getPageSize() > getRowCount() ? getRowCount() % getPageSize() : getPageSize()));
    pRequestContext.addSysDOMInfo("sqlquery/paging/pagecount", String.valueOf(getPageCount()));
    pRequestContext.addSysDOMInfo("sqlquery/paging/pagesize", String.valueOf(getPageSize()));
  }
}
