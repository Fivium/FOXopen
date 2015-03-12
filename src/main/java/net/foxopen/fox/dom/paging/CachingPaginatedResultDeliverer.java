package net.foxopen.fox.dom.paging;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.sql.ExecutableBatchAPI;
import net.foxopen.fox.database.sql.out.JDBCResultAdaptor;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dbinterface.deliverer.AddToRowProvider;
import net.foxopen.fox.dbinterface.deliverer.InterfaceQueryResultDeliverer;
import net.foxopen.fox.dbinterface.deliverer.QueryRowProvider;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.ActionRequestContext;


public class CachingPaginatedResultDeliverer
extends InterfaceQueryResultDeliverer {

  private static final String INSERT_SQL_FILENAME = "InsertPage.sql";

  private final DOM mMatchNode;
  private final Pager mPager;

  //These aren't set up until delivery processing commences.
  private UCon mUCon;
  private ExecutableBatchAPI mBatchAPI;


  public static CachingPaginatedResultDeliverer createNew(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, DOM pMatchNode, Pager pPager) {
    PaginatedRowProvider lRowProvider = new PaginatedRowProvider(new AddToRowProvider(pInterfaceQuery, pMatchNode));
    CachingPaginatedResultDeliverer lDeliverer = new CachingPaginatedResultDeliverer(pRequestContext, pInterfaceQuery, pMatchNode, pPager, lRowProvider);
    lRowProvider.mDeliverer = lDeliverer;

    return lDeliverer;
  }

  private CachingPaginatedResultDeliverer(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, DOM pMatchNode, Pager pPager, QueryRowProvider pRowProvider) {
    super(pRequestContext, pInterfaceQuery, pRowProvider);
    mMatchNode = pMatchNode;
    mPager = pPager;
  }

  @Override
  protected void performPreDeliveryProcessing() {
    //Grab a UCon reference
    mUCon = mRequestContext.getContextUCon().getUCon(mInterfaceQuery.getQualifiedName() + " paginated results");

    //Set up API for inserts
    try {
      mBatchAPI = ExecutableBatchAPI.createAndPrepare(SQLManager.instance().getStatement(INSERT_SQL_FILENAME, getClass()), ExecutableBatchAPI.DEFAULT_BATCH_SIZE, mUCon);
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to prepare pagination insert statement", e);
    }

    //Clean up the pagination cache, reset counters etc
    mPager.resetPager(mRequestContext, mMatchNode);

    //Run pre-page action
    mPager.runPrePageAction(mRequestContext, mMatchNode);
  }

  @Override
  protected void performPostDeliveryProcessing(int pFinalRowCount, ResultSet mResultSet){
    try {
      mBatchAPI.finaliseAndClose();

      mPager.setRowCount(pFinalRowCount);

      mPager.runPostPageAction(mRequestContext, mMatchNode);
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to close batch API", e);
    }
    finally {
      mRequestContext.getContextUCon().returnUCon(mUCon, mInterfaceQuery.getQualifiedName() + " paginated results");
    }
  }

  private void insertPageRow(int pRowNumber, DOM pRow) {

    try {
      UConBindMap lBindMap = new UConBindMap()
        .defineBind(":call_id", mRequestContext.getCurrentCallId())
        .defineBind(":match_id", mMatchNode.getFoxId())
        .defineBind(":invoke_name", mPager.getInvokeName())
        .defineBind(":row_num", pRowNumber)
        .defineBind(":page_xml", pRow);

      mBatchAPI.addBatch(mUCon, lBindMap);
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to add batch for insert page row", e);
    }
  }

  //Nested class has to be static because it needs passing to the parent constructor, so "this" is not available when it needs constructing
  /**
   * Decorator around an AddToRowProvider which removes rows when they exceed page boundaries.
   */
  private static class PaginatedRowProvider implements QueryRowProvider {

    private final AddToRowProvider mTargetDOMRowProvider;
    private CachingPaginatedResultDeliverer mDeliverer;

    private PaginatedRowProvider(AddToRowProvider pTargetDOMRowProvider) {
      mTargetDOMRowProvider = pTargetDOMRowProvider;
    }

    @Override
    public void prepareForDelivery(ResultSetMetaData pResultSetMeta) {
    }

    @Override
    public DOM getTargetRow(JDBCResultAdaptor pResultSet) throws ExTooMany {
      //Always create DOM in correct position so implicit datatype lookups work based on node absolute path
      return mTargetDOMRowProvider.getTargetRow();
    }

    @Override
    public void finaliseRow(int pRowNumber, DOM pRow) {

      mDeliverer.insertPageRow(pRowNumber, pRow);

      //Remove the node from its destination position if we've exceeded the page size
      if(pRowNumber > mDeliverer.mPager.getPageSize()) {
        pRow.remove();
      }
    }
  }
}
