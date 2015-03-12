package net.foxopen.fox.dbinterface.deliverer;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoIsolatedRunner;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.QueryResultDeliverer;
import net.foxopen.fox.database.sql.out.JDBCResultAdaptor;
import net.foxopen.fox.database.sql.out.ResultSetAdaptor;
import net.foxopen.fox.dbinterface.DOMDataType;
import net.foxopen.fox.dbinterface.InterfaceParameter;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dbinterface.QueryMode;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.DatabasePager;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;


/**
 * Abstract deliverer for populating the match node of a run-query command with the query results. Core behaviour (i.e.
 * datatype conversion, row looping) is defined in this class; subclasses provide behavioural tweaks as required.
 */
public abstract class InterfaceQueryResultDeliverer
implements QueryResultDeliverer {

  protected final ActionRequestContext mRequestContext;
  protected final InterfaceQuery mInterfaceQuery;
  protected final QueryRowProvider mRowProvider;

  //Fields below are populated on call to deliver()

  private ResultSet mResultSet;
  private ResultSetMetaData mResultSetMeta;

  /**
   * Constructs a new ResultDeliverer for a query based on the mode specified. If pQueryMode is null, the query definition
   * itself is examined for a mode attribute. If that is null then the default of ADD-TO is assumed.
   * @param pRequestContext Current request context.
   * @param pInterfaceQuery Query to be run.
   * @param pQueryMode Run mode requested. Can be null if not specified.
   * @param pMatchNode Node the query is being executed against.
   * @param pOptionalPager Optional pager for paging results. If this is supplied a PaginatedResultDeliverer will be returned.
   * @return A new ResultDeliverer for delivering the results of the given query to a DOM.
   */
  public static InterfaceQueryResultDeliverer getDeliverer(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, QueryMode pQueryMode, DOM pMatchNode,
                                                           DatabasePager pOptionalPager) {

    //Switch to a paginated deliverer if a pager was provided
    if(pOptionalPager != null) {
      //Check user hasn't made a markup error
      if(pQueryMode != null && pQueryMode != QueryMode.ADD_TO) {
        Track.alert("PagedQueryMode", "Paged query " + pInterfaceQuery.getQualifiedName() + " has invalid query mode " + pQueryMode.toString() +
        " specified (this will be ignored) - only ADD-TO is allowed");
      }

      return pOptionalPager.createResultDeliverer(pRequestContext, pInterfaceQuery, pMatchNode);
    }

    //Determine query mode
    QueryMode lQueryMode = pQueryMode;
    if(lQueryMode == null) {
      //Query mode not defined on command; look up on query definition
      lQueryMode = pInterfaceQuery.getQueryMode();
    }

    if(lQueryMode == null) {
      //Query mode still not defined; assume this default
      lQueryMode = QueryMode.ADD_TO;
    }
    else if((lQueryMode == QueryMode.AUGMENT || lQueryMode == QueryMode.PURGE_SELECTED) && pInterfaceQuery.getKeyElementNames() == null) {
      //Legacy workarounds: remap  AUGMENT/PURGE_SELECTED mode to ADD_TO if no target-path defined
      Track.alert("AugmentMissingKey", "Switching mode for query " + pInterfaceQuery.getQualifiedName() + " from " + lQueryMode + " to ADD-TO as it is missing a primary key definition", TrackFlag.BAD_MARKUP);
      lQueryMode = QueryMode.ADD_TO;
    }

    //Determine row provider
    QueryRowProvider lRowProvider;
    switch(lQueryMode){
      case ADD_TO:
      case PURGE_ALL:
        lRowProvider = new AddToRowProvider(pInterfaceQuery, pMatchNode);
        break;
      case PURGE_SELECTED:
        lRowProvider = new PurgeSelectedRowProvider(pInterfaceQuery, pMatchNode);
        break;
      case AUGMENT:
        lRowProvider = new AugmentRowProvider(pInterfaceQuery, pMatchNode);
        break;
      default:
        throw new ExInternal("Unknown query mode " + lQueryMode); //Thanks javac
    }

    //Construct the deliverer
    if(lQueryMode == QueryMode.PURGE_ALL) {
      return new PurgeAllResultDeliverer(pRequestContext, pInterfaceQuery, lRowProvider, pMatchNode);
    }
    else {
      //For ADD TO, PURGE SELECTED and AUGMENT:
      return new StandardResultDeliverer(pRequestContext, pInterfaceQuery, lRowProvider);
    }
  }

  protected InterfaceQueryResultDeliverer(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, QueryRowProvider pRowProvider) {
    mRequestContext = pRequestContext;
    mInterfaceQuery = pInterfaceQuery;
    mRowProvider = pRowProvider;
  }

  @Override
  public final void deliver(ExecutableQuery pQuery)
  throws ExDB {

    int lRowCount = 0;
    try {
      mResultSet = pQuery.getResultSet();
      mResultSetMeta = pQuery.getMetaData();
      JDBCResultAdaptor lResultSetAdaptor = new ResultSetAdaptor(mResultSet);

      //Delegate any pre query processing to subclasses. E.g. purge-all wants to remove all nodes.
      performPreDeliveryProcessing();

      //Allow the row provider to prepare for delivering rows E.g. setup the AUGMENT deliverer so it knows column names etc.
      mRowProvider.prepareForDelivery(mResultSetMeta);

      int lColCount = mResultSetMeta.getColumnCount();
      Map<String, InterfaceParameter> lIntoParams = mInterfaceQuery.getIntoParams();

      DOMDataType[] lColIdxToDataType = new DOMDataType[lColCount];

      ROW_LOOP:
      while(lRowCount < getMaxRows() && mResultSet.next()) {
        lRowCount++;

        //Get or create the DOM element to contain the new row
        DOM lTargetRowContainer = mRowProvider.getTargetRow(lResultSetAdaptor);

        // Validation: if there is more than one row a target-path must be specified (only bother checking on the 2nd iteration)
        if (lRowCount == 2 && ".".equals(mInterfaceQuery.getTargetPath())) {
          throw new ExInternal("Multiple rows returned so a target-path is required and none is defined for query " + mInterfaceQuery.getStatementName());
        }

        COLUMN_LOOP:
        for(int lColIdx = 1; lColIdx <= lColCount; lColIdx++) {

          String lColumnName = mResultSetMeta.getColumnName(lColIdx);
          int lColumnSQLType = mResultSetMeta.getColumnType(lColIdx);

          //Get the fm:into definition - this is optional so may be null
          InterfaceParameter lIntoParam = lIntoParams.get(lColumnName);

          //Work out path for this column - default is column name if no into param is specified, or no relative path is specified on the into param
          String lDestinationColumnPath;
          if(lIntoParam != null && lIntoParam.getRelativeXPath() != null) {
            lDestinationColumnPath = lIntoParam.getRelativeXPath();
          }
          else {
            lDestinationColumnPath = lColumnName;
          }

          //Work out the DOMDataType for this column - either explicitly specified on the into param, cached from a previous iteration or null
          DOMDataType lDestinationDataType;
          if(lIntoParam != null && lIntoParam.getDOMDataType() != null){
            lDestinationDataType = lIntoParam.getDOMDataType();
          }
          else if(lRowCount > 1) {
            lDestinationDataType = lColIdxToDataType[lColIdx - 1];
          }
          else {
            lDestinationDataType = null;
          }

          //Work out if selected DOMs should purge the contents of the existing node before being written (legacy/default is false)
          boolean lPurgeDOMs = false;
          if(lIntoParam != null) {
            lPurgeDOMs = lIntoParam.isPurgeDOMContents();
          }

          // Read the value from the column into the DOM
          DOMDataType lPopulatedDataType = DelivererUtils.convertResultAndPopulateDOM(mRequestContext, lResultSetAdaptor, lColIdx, lColumnSQLType,
                                                                                      lTargetRowContainer, lDestinationColumnPath, lDestinationDataType,
                                                                                      lColumnName, true, lPurgeDOMs);

          //For the first iteration cache the returned datatype against the coulmn index. This avoids re-calculating it every time.
          if(lRowCount == 1) {
            lColIdxToDataType[lColIdx - 1] = lPopulatedDataType;
          }
        } // COLUMN_LOOP

        //If this is the first result, validate the query definition (dev only)
        if(lRowCount == 1 && !FoxGlobals.getInstance().isProduction()) {
          validateQueryDefinition(lIntoParams);
        }

        //The user may have specified actions to run for each row
        runForEachRowCommands(lTargetRowContainer);

        //The row provider may want to clean up the new row
        mRowProvider.finaliseRow(lRowCount, lTargetRowContainer);
      } // ROW_LOOP

      // Write out number of rows read by the query as a useful property in the :{sys} DOM
      mRequestContext.addSysDOMInfo("sqlquery/rowcount", String.valueOf(lRowCount));
    }
    catch (ExTooMany | SQLException e) {
      throw new ExInternal("Error writing query results to DOM in query " + mInterfaceQuery.getStatementName(), e);
    }
    finally {
      //Make sure this happens even in the event of an error
      performPostDeliveryProcessing(lRowCount, mResultSet);
    }
  }

  private void runForEachRowCommands(DOM pRowDOM) {

    XDoCommandList lForEachRowCommands = mInterfaceQuery.getForEachRowCommandList();
    if(lForEachRowCommands != null) {
      ContextUElem lContextUElem = mRequestContext.getContextUElem().localise("For-each-row");
      try {
        lContextUElem.setUElem(ContextLabel.ATTACH, pRowDOM);
        XDoIsolatedRunner lCommandRunner = mRequestContext.createIsolatedCommandRunner(true);
        lCommandRunner.runCommandsAndComplete(mRequestContext, lForEachRowCommands);
      }
      finally {
        lContextUElem.delocalise("For-each-row");
      }
    }
  }

  private void validateQueryDefinition(Map<String, InterfaceParameter> pIntoParams)
  throws SQLException {

    //Check all fm:into columns definitions are selected in the query
    PARAM_LOOP:
    for (InterfaceParameter lParam : pIntoParams.values()) {
      for(int i=1; i <= mResultSetMeta.getColumnCount(); i++) {
        if(mResultSetMeta.getColumnName(i).equals(lParam.getParamName())) {
          //This param is used; check the next one
          continue PARAM_LOOP;
        }
      }
      //Metadata column loop completed without finding a matching into param - error now.
      throw new ExInternal("Error: fm:into with name " + lParam.getParamName() + " was defined in markup but no corresponding column was selected by query " + mInterfaceQuery.getStatementName());
    }

    //Check column names aren't duplicated
    Set<String> lColNameSet = new HashSet<>();
    for(int i=1; i <= mResultSetMeta.getColumnCount(); i++) {
      String lColName = mResultSetMeta.getColumnName(i);
      if(lColNameSet.contains(lColName)) {
        //The name is already in the set - means we already hit it in this loop
        throw new ExInternal("Error: query " + mInterfaceQuery.getStatementName() + " selected a column named " + lColName + " multiple times");
      }
      else {
        lColNameSet.add(lColName);
      }
    }
  }

  @Override
  public final boolean closeStatementAfterDelivery() {
    return true;
  }

  /**
   * Gets the maximum number of rows which should be read by this deliverer's result loop.
   * @return
   */
  public int getMaxRows() {
    return Integer.MAX_VALUE;
  }

  /**
   * Hook to allow subclasses to do whatever they need before delivery starts.
   */
  protected abstract void performPreDeliveryProcessing();

  /**
   * Method called at the end of delivery, regardless of whether it was successful. This allows implementors to do any
   * required finalisation.
   * @param pFinalRowCount
   */
  protected abstract void performPostDeliveryProcessing(int pFinalRowCount, ResultSet pResultSet);

}
