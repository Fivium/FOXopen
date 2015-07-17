package net.foxopen.fox.module.datadefinition.datatransformer;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.out.ResultSetAdaptor;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.ex.ExInternal;
import org.json.simple.JSONArray;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ArrayTransformerRecordDeliverer
  implements DataTransformerResultDeliverer {

  private final JSONArray mTransformedData = new JSONArray();
  private final String mSeriesColumnName;
  private final String mXColumnName;
  private final String mYColumnName;

  public ArrayTransformerRecordDeliverer(String pSeriesColumnName, String pXColumnName, String pYColumnName) {
    mSeriesColumnName = pSeriesColumnName;
    mXColumnName = pXColumnName;
    mYColumnName = pYColumnName;
  }

  @Override
  public void deliver(ExecutableQuery pQuery) {

    ResultSet lResultSet = pQuery.getResultSet();
    ResultSetMetaData lMetaData = pQuery.getMetaData();
    ResultSetAdaptor lResultSetAdaptor = new ResultSetAdaptor(lResultSet);

    try {
      int lSeriesColumnIndex = 0, lXColumnIndex = 0, lYColumnIndex = 0;

      //Validate the result set
      for(int i = 1; i <= lMetaData.getColumnCount(); i++) {
        if(!XFUtil.isNull(mSeriesColumnName) && mSeriesColumnName.equals(lMetaData.getColumnLabel(i))) {
          lSeriesColumnIndex = i;
        }
        else if(mXColumnName.equals(lMetaData.getColumnLabel(i))) {
          lXColumnIndex = i;
        }
        else if(mYColumnName.equals(lMetaData.getColumnLabel(i))) {
          lYColumnIndex = i;
        }
      }

      if(lXColumnIndex == 0) {
        throw new ExInternal("Error in Data Definition - missing 'x' column labelled '" + mXColumnName + "'");
      }
      else if(lYColumnIndex == 0) {
        throw new ExInternal("Error in Data Definition - missing 'y' column labelled '" + mYColumnName + "'");
      }

      Map<String, JSONArray> lSeriesMap = new HashMap<>();
      JSONArray lSeries = null;
      int lRowNum = 0;
      while (lResultSet.next()) {
        if (!XFUtil.isNull(mSeriesColumnName)) {
          // If a series column name was defined, use it
          String lSeriesName = SQLTypeConverter.getValueAsString(lResultSetAdaptor, lSeriesColumnIndex, lMetaData.getColumnType(lSeriesColumnIndex));
          if (XFUtil.isNull(lSeriesName)) {
            throw new ExInternal("Error in Data Definition - 'series' column cannot be null on row " + lRowNum);
          }
          lSeries = lSeriesMap.get(lSeriesName);
          if (lSeries == null) {
            lSeries = new JSONArray();
            lSeriesMap.put(lSeriesName, lSeries);
          }
        }
        else if (lSeries == null) {
          // If no series column name was defined, set up the series variable with just a data field for all tuples
          lSeries = new JSONArray();
        }

        JSONArray lCoordTuple = new JSONArray();

        Object lXColumnValue = SQLTypeConverter.getValueForJSONObject(lResultSetAdaptor, lXColumnIndex, lMetaData.getColumnType(lXColumnIndex));
        if(XFUtil.isNull(lXColumnValue)) {
          throw new ExInternal("Error in Data Definition - 'x' column cannot be null on row " + lRowNum);
        }
        Object lYColumnValue = SQLTypeConverter.getValueForJSONObject(lResultSetAdaptor, lYColumnIndex, lMetaData.getColumnType(lYColumnIndex));
        if(XFUtil.isNull(lYColumnValue)) {
          throw new ExInternal("Error in Data Definition - 'y' column cannot be null on row " + lRowNum);
        }

        lCoordTuple.add(lXColumnValue);
        lCoordTuple.add(lYColumnValue);

        lSeries.add(lCoordTuple);

        lRowNum++;
      }

      if (!XFUtil.isNull(mSeriesColumnName)) {
        JSONArray lSeriesNames = new JSONArray();
        JSONArray lSeriesDataArrays = new JSONArray();
        for (Map.Entry<String, JSONArray> lSeriesMapEntry : lSeriesMap.entrySet()) {
          lSeriesNames.add(lSeriesMapEntry.getKey());
          lSeriesDataArrays.add(lSeriesMapEntry.getValue());
        }
        mTransformedData.add(lSeriesNames);
        mTransformedData.addAll(lSeriesDataArrays);
      }
      else {
        mTransformedData.add(new JSONArray());
        mTransformedData.add(lSeries);
      }
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to run record query for Data Definition", e);
    }
  }

  @Override
  public boolean closeStatementAfterDelivery() {
    return true;
  }

  @Override
  public JSONArray getTransformedData() {
    return mTransformedData;
  }
}