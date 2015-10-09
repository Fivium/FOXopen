package net.foxopen.fox.module.mapset;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.QueryResultDeliverer;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.database.sql.out.ResultSetAdaptor;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.PathOrDOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTooMany;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.fieldset.fvm.FVMOption;
import net.foxopen.fox.thread.ActionRequestContext;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class JITMapSet implements MapSet {

  private final String mEvaluatedCacheKey;

  private final AJAXQueryDefinition mMapSetDefinition; // The map set definition

  private final long mCreatedTimeMS; // The last time a refresh occurred

  protected JITMapSet(AJAXQueryDefinition pMapSetDefinition, String pEvaluatedCacheKey) {
    mMapSetDefinition = pMapSetDefinition;
    mEvaluatedCacheKey = pEvaluatedCacheKey;
    mCreatedTimeMS = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " CacheKey: " + mEvaluatedCacheKey + " LifetimeMins: " + ((System.currentTimeMillis() - mCreatedTimeMS) / 1000 / 60) +
      " TimeoutMins: " +  mMapSetDefinition.getRefreshTimeoutMins() + " IsDynamic: " + isDynamic();
  }

  /**
   * JITMapSets can only be created from AJAXQueryDefinitions so return the corresponding AJAXQueryDefinition
   * @return AJAXQueryDefinition used to create this MapSet
   */
  @Override
  public AJAXQueryDefinition getMapSetDefinition() {
    return mMapSetDefinition;
  }

  @Override
  public String getEvaluatedCacheKey() {
    return mEvaluatedCacheKey;
  }

  @Override
  public String getMapSetName() {
    return mMapSetDefinition.getLocalName();
  }

  @Override
  public boolean isDynamic() {
    return mMapSetDefinition.isDynamic();
  }

  @Override
  public boolean isRefreshRequired() {
    if(!mMapSetDefinition.isDynamic()) {
      //If this mapset is not dynamic, it will never require a refresh - TODO make sure editable mapsets are dealt with
      return false;
    }
    if (mMapSetDefinition.getRefreshTimeoutMins() == 0 ) {
      //If refresh timeout is specified as 0, the mapset should always be refreshed
      return true;
    }
    else {
      //Otherwise, only refresh if the specified number of minutes has elapsed since the mapset was created
      return System.currentTimeMillis() - mCreatedTimeMS > mMapSetDefinition.getRefreshTimeoutMins() * 60 * 1000;
    }
  }

  /**
   * JITMapSets do not hold all the possible records so cannot return them in DOM form
   *
   * @return
   * @throws ExInternal as JITMapSet cannot return a DOM with all MapSet records in
   */
  @Override
  public DOM getMapSetAsDOM() {
    throw new ExInternal("Cannot get MapSet as DOM for a JITMapSet");
  }

  @Override
  public boolean containsData(ActionRequestContext pRequestContext, DOM pDataDOM) {
    try {
      // If we can get some kind of key out of the MapSet then the data was valid
      return !XFUtil.isNull(getKey(pRequestContext, pDataDOM));
    }
    catch (Exception e) {
      return false;
    }
  }

  @Override
  public String getKey(ActionRequestContext pRequestContext, DOM pDataDOM) {
    if (pDataDOM == null || XFUtil.isNull(pDataDOM.get1SNoEx(mMapSetDefinition.getRefPath()))) {
      return "";
    }

    String lRef;
    if (XFUtil.isNull(mMapSetDefinition.getRefPath())) {
      lRef = pDataDOM.value();
    }
    else {
      try {
        lRef = pDataDOM.get1S(mMapSetDefinition.getRefPath());
      }
      catch (ExTooFew | ExTooMany e) {
        throw new ExInternal("Failed to find a ref string in mapset data value using ref-path: " + mMapSetDefinition.getRefPath(), e);
      }
    }

    return getKeyForDataString(pRequestContext, pDataDOM, lRef);
  }

  @Override
  public String getKeyForDataString(ActionRequestContext pRequestContext, DOM pMapSetItem, String pDataString) {
    if (XFUtil.isNull(pDataString)) {
      return "";
    }

    if (pMapSetItem == null) {
      throw new ExInternal("When getting the key string for a JIT MapSet you have to pass a valid DOM node for the MapSet item");
    }

    UCon lUCon = pRequestContext.getContextUCon().getUCon("JITMapSet GetKey");
    pRequestContext.getContextUElem().localise("JITMapSet::getKeyForDataString");
    try {
      // TODO - AJMS - No mapset attach yet
      mMapSetDefinition.setupContextUElem(pRequestContext.getContextUElem(), pMapSetItem, new PathOrDOM(""));

      DecoratingBindObjectProvider lBinds = AJAXQueryDefinition.getRefBindObjectProvider(pDataString);

      KeyResultDeliverer lResultDeliverer = new KeyResultDeliverer();
      mMapSetDefinition.getRefQueryStatement(pRequestContext).executeStatement(pRequestContext, pMapSetItem, lUCon, lBinds, lResultDeliverer);
      return lResultDeliverer.getKey();
    }
    catch (ExDB pExDB) {
      throw new ExInternal("Mapset failed to get key: " + getMapSetName(), pExDB);
    }
    finally {
      pRequestContext.getContextUElem().delocalise("JITMapSet::getKeyForDataString");
      pRequestContext.getContextUCon().returnUCon(lUCon, "JITMapSet GetKey");
    }
  }

  private class KeyResultDeliverer implements QueryResultDeliverer {
    private String mKey = "";

    @Override
    public void deliver(ExecutableQuery pQuery) throws ExDB {
      ResultSet lResultSet = pQuery.getResultSet();
      try {
        if(!lResultSet.next()) {
          // Okay for zero rows returned
          return;
        }

        ResultSetMetaData lMetaData = lResultSet.getMetaData();
        int lKeyColumnIndex = 0;
        for (int lCol = 1; lCol <= lMetaData.getColumnCount(); lCol++) {
          if (MapSet.KEY_ELEMENT_NAME.equals(lMetaData.getColumnName(lCol))) {
            lKeyColumnIndex = lCol;
            break;
          }
        }

        if (lKeyColumnIndex == 0) {
          throw new ExDB("Couldn't find a key column in the ref query " + pQuery.getParsedStatement().getStatementPurpose());
        }
        else {
          mKey = SQLTypeConverter.getValueAsString(new ResultSetAdaptor(lResultSet), lKeyColumnIndex, lMetaData.getColumnType(lKeyColumnIndex));
        }

        if(lResultSet.next()) {
          // Null out due to failure
          mKey = null;
          throw new ExDBTooMany("KeyResultDeliverer requires exactly 1 row, got more than 1 for query " + pQuery.getParsedStatement().getStatementPurpose());
        }

      }
      catch (SQLException e) {
        pQuery.convertErrorAndThrow(e);
      }
    }

    @Override
    public boolean closeStatementAfterDelivery() {
      return true;
    }

    /**
     * Get the key value from the result of the ref query on the AJAXQueryDefinition
     *
     * @return Empty String if no rows returned, a value if a row was found or null if more than one row found
     */
    public String getKey() {
      return mKey;
    }
  }

  public void applyDataToNode(ActionRequestContext pRequestContext, String pRef, DOM pDataNode) {
    if (XFUtil.isNull(pRef)) {
      return;
    }

    UCon lUCon = pRequestContext.getContextUCon().getUCon("JITMapSet ApplyDataToNode");
    pRequestContext.getContextUElem().localise("JITMapSet::applyDataToNode");
    try {
      // TODO - AJMS - No mapset attach yet
      mMapSetDefinition.setupContextUElem(pRequestContext.getContextUElem(), pDataNode, new PathOrDOM(""));

      DecoratingBindObjectProvider lBinds = AJAXQueryDefinition.getRefBindObjectProvider(pRef);

      mMapSetDefinition.getRefQueryStatement(pRequestContext).executeStatement(pRequestContext, pRequestContext.getContextUElem().attachDOM(), lUCon, lBinds, new DataResultDeliverer(pDataNode));
    }
    catch (ExDB pExDB) {
      throw new ExInternal("Mapset ApplyDataToNode Failed", pExDB);
    }
    finally {
      pRequestContext.getContextUElem().delocalise("JITMapSet::applyDataToNode");
      pRequestContext.getContextUCon().returnUCon(lUCon, "JITMapSet ApplyDataToNode");
    }
  }

  private class DataResultDeliverer implements QueryResultDeliverer {
    private final DOM mTargetNode;

    public DataResultDeliverer(DOM pTargetNode) {
      mTargetNode = pTargetNode;
    }

    @Override
    public void deliver(ExecutableQuery pQuery) throws ExDB {
      ResultSet lResultSet = pQuery.getResultSet();
      ResultSetAdaptor lResultSetAdaptor = new ResultSetAdaptor(lResultSet);
      try {
        if(!lResultSet.next()) {
          // Okay for zero rows returned
          return;
        }

        int lDataColumnIndex = 0;
        ResultSetMetaData lMetaData = lResultSet.getMetaData();
        for (int lCol = 1; lCol <= lMetaData.getColumnCount(); lCol++) {
          if (MapSet.DATA_ELEMENT_NAME.equals(lMetaData.getColumnName(lCol))) {
            lDataColumnIndex = lCol;
            break;
          }
        }

        if (lDataColumnIndex == 0) {
          throw new ExDB("Couldn't find a data column in the ref query");
        }

        DOM lDataDOM = DOM.createUnconnectedElement(MapSet.DATA_ELEMENT_NAME);

        SQLTypeConverter.applyValueToDOM(lResultSetAdaptor, lDataColumnIndex, lMetaData.getColumnType(lDataColumnIndex), lDataDOM);

        if(lResultSet.next()) {
          throw new ExDBTooMany("SingleRowResultDeliverer requires exactly 1 row, got more than 1 for query " + pQuery.getParsedStatement().getStatementPurpose());
        }
        else {
          lDataDOM.copyContentsTo(mTargetNode);
        }
      }
      catch (SQLException e) {
        pQuery.convertErrorAndThrow(e);
      }
    }

    @Override
    public boolean closeStatementAfterDelivery() {
      return true;
    }
  }

  /**
   * JITMapSet doesn't store a list of entries like DOMMapSets do, so cannot find an index. Calling this will throw an
   * exception.
   * @param pItemDOM
   * @return
   * @throws ExInternal as it cannot return an item index
   */
  @Override
  public int indexOf(DOM pItemDOM) {
    throw new ExInternal("JITMapSet cannot return the index of an item");
  }

  /**
   * JITMapSet doesn't store a list of entries like DOMMapSets do, so this always returns an empty list
   *
   * @return empty List
   */
  @Override
  public List<FVMOption> getFVMOptionList() {
    return Collections.emptyList();
  }

  /**
   * JITMapSet doesn't store a list of entries like DOMMapSets do, so this always returns an empty list
   *
   * @return empty List
   */
  @Override
  public List<MapSetEntry> getEntryList() {
    return Collections.emptyList();
  }
}
