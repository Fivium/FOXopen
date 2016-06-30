package net.foxopen.fox.module.datadefinition.datatransformer;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.out.ResultSetAdaptor;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.ex.ExInternal;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


public class VerbatimTransformerRecordDeliverer
  implements DataTransformerResultDeliverer {

  private final JSONArray mTransformedData = new JSONArray();

  public VerbatimTransformerRecordDeliverer() {
  }

  @Override
  public void deliver(ExecutableQuery pQuery) {

    ResultSet lResultSet = pQuery.getResultSet();
    ResultSetMetaData lMetaData = pQuery.getMetaData();
    ResultSetAdaptor lResultSetAdaptor = new ResultSetAdaptor(lResultSet);

    try {
      List<JSONObject> lResultList = new ArrayList<>();

      while (lResultSet.next()) {
        JSONObject lResult = new JSONObject();

        for(int i = 1; i <= lMetaData.getColumnCount(); i++) {
          String lColumnLabel = lMetaData.getColumnLabel(i);
          Object lColumnValue = SQLTypeConverter.getValueForJSONObject(lResultSetAdaptor, i, lMetaData.getColumnType(i));
          lResult.put(lColumnLabel, lColumnValue);
        }

        lResultList.add(lResult);
      }

      mTransformedData.addAll(lResultList);
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