package net.foxopen.fox.dom.paging;

import java.util.List;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dbinterface.deliverer.InterfaceQueryResultDeliverer;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * Pager which caches all rows from the query in a special cache table, for retrieval when the user changes page.
 */
public class CachingDatabasePager
extends DatabasePager {

  private static final String SELECT_PAGE_FILENAME = "SelectPage.sql";
  private static final String DELETE_PAGES_FILENAME = "DeletePages.sql";

  CachingDatabasePager(String pPagerKey, String pModuleCallId, EvaluatedPagerSetup pEvalPagerSetup) {
    super(pPagerKey, pModuleCallId, pEvalPagerSetup);
  }

  @Override
  protected void populatePage(ActionRequestContext pRequestContext, DOM pTargetNode) {

    UConBindMap lBindMap = new UConBindMap()
      .defineBind(":call_id", getModuleCallId())
      .defineBind(":match_id", mMatchFoxId)
      .defineBind(":invoke_name", getInvokeName())
      .defineBind(":row_start", getCurrentPageStartRowNum())
      .defineBind(":row_end", getCurrentPageEndRowNum());

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Select Page");
    try {
      List<UConStatementResult> lList = lUCon.queryMultipleRows(SQLManager.instance().getStatement(SELECT_PAGE_FILENAME, getClass()), lBindMap);
      for(UConStatementResult lPageRow : lList) {
        lPageRow.getDOMFromSQLXML("PAGE_XML").copyToParent(pTargetNode);
      }
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to run select page query", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Select Page");
    }
  }

  @Override
  protected void resetPagerInternal(ActionRequestContext pRequestContext, DOM pMatchDOM) {
    UCon lUCon = pRequestContext.getContextUCon().getUCon("Reset page cache");
    try {
      UConBindMap lBindMap = new UConBindMap()
        .defineBind(":call_id", getModuleCallId())
        .defineBind(":match_id", mMatchFoxId)
        .defineBind(":invoke_name", getInvokeName());

      lUCon.executeAPI(SQLManager.instance().getStatement(DELETE_PAGES_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to clear page cache", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Reset page cache");
    }
  }

  @Override
  public InterfaceQueryResultDeliverer createResultDeliverer(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, DOM pMatchNode) {
    return CachingPaginatedResultDeliverer.createNew(pRequestContext, pInterfaceQuery, pMatchNode, this);
  }

  @Override
  public DecoratingBindObjectProvider getDecoratingBindProviderOrNull(InterfaceQuery pInterfaceQuery) {
    return null;
  }
}
