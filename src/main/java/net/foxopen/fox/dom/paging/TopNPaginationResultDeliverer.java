package net.foxopen.fox.dom.paging;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.PreExecutionDeliveryHandler;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dbinterface.TopNPaginationConfig;
import net.foxopen.fox.dbinterface.deliverer.AddToRowProvider;
import net.foxopen.fox.dbinterface.deliverer.InterfaceQueryResultDeliverer;
import net.foxopen.fox.dbinterface.deliverer.QueryRowProvider;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

/**
 * TopN result deliverer. This restricts the amount of rows read by the parent class and also "looks ahead" across
 * a set number of rows, so the user can have an indication of how many further pages are available in the paged query.
 */
public class TopNPaginationResultDeliverer
extends InterfaceQueryResultDeliverer
implements PreExecutionDeliveryHandler<ExecutableQuery> {

  private final DOM mMatchNode;
  private final TopNDatabasePager mPager;
  private final TopNPaginationConfig mPaginationConfig;

  /** True if this is the first time this pager is executing a query, rather than selecting a new page (i.e. called from fm:run-query). */
  private final boolean mInitialQuery;

  private int mLastReadRowCountColumn = -1;

  public static TopNPaginationResultDeliverer createResultDeliverer(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery,
                                                                    DOM pMatchNode, TopNDatabasePager pPager, boolean pInitialQuery) {
    //Note chicken/egg problem with row provider and deliverer - row provider must be given to the deliverer's super constructor but also needs a reference to the deliverer
    TopNRowProvider lRowProvider = new TopNRowProvider(pInterfaceQuery, pMatchNode);
    TopNPaginationResultDeliverer lDeliverer = new TopNPaginationResultDeliverer(pRequestContext, pInterfaceQuery, lRowProvider, pMatchNode, pPager, pInitialQuery);
    lRowProvider.mResultDeliverer = lDeliverer;
    return lDeliverer;
  }

  private TopNPaginationResultDeliverer(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, QueryRowProvider pRowProvider,
                                        DOM pMatchNode, TopNDatabasePager pPager, boolean pInitialQuery) {
    super(pRequestContext, pInterfaceQuery, pRowProvider);
    mMatchNode = pMatchNode;
    mPager = pPager;
    mInitialQuery = pInitialQuery;
    mPaginationConfig = pInterfaceQuery.getTopNPaginationConfig();
  }

  @Override
  protected void performPreDeliveryProcessing() {
    if(mInitialQuery) {
      //Only run pre action here when the query is initially run (pager go to page logic handles other use case)
      mPager.runPrePageAction(mRequestContext, mMatchNode);
    }
  }

  @Override
  protected void performPostDeliveryProcessing(int pFinalRowCount, ResultSet pResultSet) {
    //Work out the 1-based row number of the last row to have been read by the main loop
    int lLastRowJustRead = mPager.getCurrentPageStartRowNum() + pFinalRowCount - 1;

    if(mLastReadRowCountColumn != -1) {
      //Query reported the row count for us - this takes precedence
      Track.info("TopNPager", "Setting pager row count to " + mLastReadRowCountColumn + " based on result from " + mPaginationConfig.getRowCountColumnName() + " column");
      mPager.setRowCount(mLastReadRowCountColumn);
    }
    else if(lLastRowJustRead + mPager.getLookAheadRows() >= mPager.getRowCount()) {
      //Look forward over additional rows if this is the furthest this pager has been
      int lLookaheadRows = 0;
      try {
        while (pResultSet.next()) {
          lLookaheadRows++;
        }
      }
      catch (SQLException e) {
        throw new ExInternal("Failed to read lookahead rows on TopN deliverer", e);
      }

      int lNewRowCount = lLastRowJustRead + lLookaheadRows;
      if(lNewRowCount > mPager.getRowCount()) {
        Track.info("TopNPagerAdditionalRows", "Found " + lLookaheadRows + ", new row count is " + lNewRowCount);
        //Adjust the row count to reflect the newly found rows
        mPager.setRowCount(lNewRowCount);
      }
    }

    if(mInitialQuery) {
      //Only run post page action here when the query is initially run (pager go to page logic handles other use case)
      mPager.runPostPageAction(mRequestContext, mMatchNode);
    }
  }

  @Override
  public int getMaxRows() {
    return mPager.getPageSize();
  }

  private void setLastReadRowCountColumn(int pRowCount, int pRowNum) {
    //Check the reported row count is the same on every row
    if(mLastReadRowCountColumn == -1) {
      mLastReadRowCountColumn = pRowCount;
    }
    else {
      if(pRowCount != mLastReadRowCountColumn) {
        throw new ExInternal("Row count column cannot change across rows. Value for row number " + pRowNum + " was " + pRowCount + " but " +
                             mLastReadRowCountColumn +  " was already reported by a previous row.");
      }
    }
  }

  @Override
  public void processBeforeExecution(UCon pUCon, ExecutableQuery pExecutableStatement) {
    if(mInitialQuery) {
      //If this is the first time the paged query has been run, we may need to make a note of the current SCN
      mPager.setSystemChangeNumber(pUCon);
    }
  }

  /**
   * RowProvider which reads the value of a previously specified "row count" column, if defined, then removes it from
   * the DOM.
   */
  private static class TopNRowProvider
  extends AddToRowProvider {

    private TopNPaginationResultDeliverer mResultDeliverer;

    private TopNRowProvider(InterfaceQuery pInterfaceQuery, DOM pMatchNode) {
      super(pInterfaceQuery, pMatchNode);
    }

    @Override
    public void finaliseRow(int pRowNumber, DOM pRow) {
      //Read the "row count" column if it was defined then remove it from the DOM
      String lRowCountCol = mResultDeliverer.mPaginationConfig.getRowCountColumnName();
      if(!XFUtil.isNull(lRowCountCol)) {
        DOM lColDOM = pRow.get1EOrNull(lRowCountCol);
        if(lColDOM != null) {
          //Read the string and parse to an int if not null
          String lColVal = lColDOM.get1SNoEx(".");
          if(!XFUtil.isNull(lColVal)) {
            try {
              mResultDeliverer.setLastReadRowCountColumn(Integer.parseInt(lColVal), pRowNumber);
            }
            catch (NumberFormatException e) {
              throw new ExInternal("Value for column " + lRowCountCol + " must be a valid integer", e);
            }
          }
          //Clear the column from the DOM
          lColDOM.remove();
        }
        else {
          throw new ExInternal("Top-N pagination row count column " + lRowCountCol + " was not found in query result set");
        }
      }
    }
  }
}
