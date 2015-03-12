package net.foxopen.fox.module.mapset;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.QueryResultDeliverer;
import net.foxopen.fox.database.sql.out.ResultSetAdaptor;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dbinterface.InterfaceStatement;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.CacheKey;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


public class RecordQueryDefinition
extends InterfaceQueryDefinition {

  static final String KEY_COLUMN_ATTR = "key-column-name";
  static final String DATA_COLUMN_ATTR = "data-column-name";

  private static final String DEFAULT_KEY_COLUMN_NAME = MapSet.KEY_ELEMENT_NAME;
  private static final String DEFAULT_DATA_COLUMN_NAME = MapSet.DATA_ELEMENT_NAME;

  private final String mKeyColumnName;
  private final String mDataColumnName;

  protected RecordQueryDefinition(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule, InterfaceStatement pDOMQueryStatement,
                                  String pQueryMatchXPath, String pKeyColumnName, String pDataColumnName)
  throws ExModule {
    super(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule, pDOMQueryStatement, pQueryMatchXPath);
    mKeyColumnName = XFUtil.nvl(pKeyColumnName, DEFAULT_KEY_COLUMN_NAME);
    mDataColumnName = XFUtil.nvl(pDataColumnName, DEFAULT_DATA_COLUMN_NAME);
  }

  @Override
  protected DOM createMapSetDOM(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue) {

    DOM lMapSetContainerDOM = createDefaultContainerDOM();
    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    //Determine match node
    DOM lMatchNode = establishMatchNode(lContextUElem);

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Create Record Query MapSet");
    try {
      //Construct a container for the mapset
      DOM lMapSetDOM = lMapSetContainerDOM.addElem(MapSet.MAPSET_ELEMENT_NAME);
      //Execute the query and deliver to the mapset DOM using a custom deliverer
      mDOMQueryStatement.executeStatement(pRequestContext, lMatchNode, lUCon, new RecordDeliverer(lMapSetDOM));
    }
    catch (ExDB e) {
      throw new ExInternal("Error running mapset record query", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Create Record Query MapSet");
    }

    return lMapSetContainerDOM;
  }

  private final class RecordDeliverer
  implements QueryResultDeliverer {

    private final DOM mMapSetTargetDOM;

    public RecordDeliverer(DOM pMapSetTargetDOM) {
      mMapSetTargetDOM = pMapSetTargetDOM;
    }

    @Override
    public void deliver(ExecutableQuery pQuery) {

      ResultSet lResultSet = pQuery.getResultSet();
      ResultSetMetaData lMetaData = pQuery.getMetaData();
      ResultSetAdaptor lResultSetAdaptor = new ResultSetAdaptor(lResultSet);

      try {
        int lDataColumnIndex = 0, lKeyColumnIndex = 0;

        //Validate the result set
        for(int i = 1; i <= lMetaData.getColumnCount(); i++) {
          if(mDataColumnName.equals(lMetaData.getColumnLabel(i))) {
            lDataColumnIndex = i;
          }
          else if(mKeyColumnName.equals(lMetaData.getColumnLabel(i))) {
            lKeyColumnIndex = i;
          }
        }

        if(lDataColumnIndex == 0) {
          throw new ExInternal("Error in record mapset " + getLocalName() + " - missing 'data' column labelled '" + mDataColumnName + "'");
        }
        else if(lKeyColumnIndex == 0) {
          throw new ExInternal("Error in record mapset " + getLocalName() + " - missing 'key' column labelled '" + mKeyColumnName + "'");
        }

        //Loop through each row and create a rec with corresponding key/data entries
        int lRowNum = 0;
        while (lResultSet.next()) {

          //Create a new rec
          DOM lRecDOM = mMapSetTargetDOM.addElem(MapSet.REC_ELEMENT_NAME);

          //Add the key element as a string
          String lKey = SQLTypeConverter.getValueAsString(lResultSetAdaptor, lKeyColumnIndex, lMetaData.getColumnType(lKeyColumnIndex));
          if(XFUtil.isNull(lKey)) {
            throw new ExInternal("Error in record mapset " + getLocalName() + " - 'key' column cannot be null on row " + lRowNum);
          }
          lRecDOM.addElem(MapSet.KEY_ELEMENT_NAME, lKey);

          //Check the data column is not null and bring into rec
          if(XFUtil.isNull(lResultSet.getObject(mDataColumnName))) {
            throw new ExInternal("Error in record mapset " + getLocalName() + " - 'data' column cannot be null on row " + lRowNum);
          }
          else {
            DOM lDataElem = lRecDOM.addElem(MapSet.DATA_ELEMENT_NAME);
            SQLTypeConverter.applyValueToDOM(lResultSetAdaptor, lDataColumnIndex, lMetaData.getColumnType(lDataColumnIndex), lDataElem);
          }

          //For all additional columns, bring the value into the DOM in the most type-appropriate way (i.e. SQLXML as a DOM, dates in xs:date format, varchars as a string, etc)
          for(int i=1; i<= lMetaData.getColumnCount(); i++) {
            if(i != lDataColumnIndex && i != lKeyColumnIndex) {
              DOM lColTarget = lRecDOM.addElem(lMetaData.getColumnName(i));
              SQLTypeConverter.applyValueToDOM(lResultSetAdaptor, i, lMetaData.getColumnType(i), lColTarget);
            }
          }

          lRowNum++;
        }
      }
      catch (SQLException e) {
        throw new ExInternal("Failed to run record query for mapset " + getLocalName(), e);
      }
    }

    @Override
    public boolean closeStatementAfterDelivery() {
      return true;
    }
  }
}
