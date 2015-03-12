package net.foxopen.fox.database.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExDBTooMany;

/**
 * Deliverer for querying exactly one row of one or more columns. The columns are stored in a Map of column names to values.
 * This deliver throws exceptions if the incorrect amount of rows is selected.
 */
public class SingleRowResultDeliverer
implements QueryResultDeliverer {

  private Map<String, Object> mColumnMap;

  public SingleRowResultDeliverer() { }

  /**
   * Constructs a map of column names to raw result objects for the given result set. The map is ordered according to
   * the order of the columns.
   * @param pResultSet
   * @return
   * @throws SQLException
   */
  public static Map<String, Object> createObjectMapFromResultSet(ResultSet pResultSet)
  throws SQLException {
    Map<String, Object> lColumnMap = new LinkedHashMap<>();
    ResultSetMetaData lMetaData = pResultSet.getMetaData();
    for (int lCol = 1; lCol <= lMetaData.getColumnCount(); lCol++) {
      //Read the raw SQL objects into the column map
      lColumnMap.put(lMetaData.getColumnName(lCol), pResultSet.getObject(lCol));
    }

    return lColumnMap;
  }

  @Override
  public void deliver(ExecutableQuery pQuery)
  throws ExDBTooFew, ExDBTooMany, ExDB {

    ResultSet lResultSet = pQuery.getResultSet();

    try {
      if(!lResultSet.next()) {
        throw new ExDBTooFew("SingleRowResultDeliverer requires exactly 1 row, got 0 for statement " + pQuery.getParsedStatement().getStatementPurpose());
      }

      mColumnMap = createObjectMapFromResultSet(lResultSet);

      if(lResultSet.next()) {
        //Null out due to failure
        mColumnMap = null;
        throw new ExDBTooMany("SingleRowResultDeliverer requires exactly 1 row, got more than 1 for query " + pQuery.getParsedStatement().getStatementPurpose());
      }

    }
    catch (SQLException e) {
      pQuery.convertErrorAndThrow(e);
    }
  }

  public Map<String, Object> getColumnMap() {
    return Collections.unmodifiableMap(mColumnMap);
  }

  @Override
  public boolean closeStatementAfterDelivery() {
    return true;
  }
}
