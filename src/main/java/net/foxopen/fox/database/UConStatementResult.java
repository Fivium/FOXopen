package net.foxopen.fox.database;

import net.foxopen.fox.database.sql.SingleRowResultDeliverer;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.plugin.api.database.FxpUConStatementResult;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Container for the result of running either a query or API from one the the {@link UCon} statement execution methods.
 * Columns or out binds are retrieved from the JDBC ResultSet or CallableStatement as raw Objects, and stored in a
 * UConStatementResult mapped against their column names or bind names, respectively. The underlying ResultSet is not
 * referenced by this object, making it safe to use a UConStatementResult after the statement which populated it is closed.
 * However, LOBs should be read before the UCon used to select them is returned to its ContextUCon or closed.<br/><br/>
 *
 * It is not possible to guarantee type safety when dealing with statements which return multiple results. As such, consumers
 * of this object will need advance knowledge of the types returned by the statement which has been used to create it.
 * For example, if you know a column is a Clob and you require a String result, use {@link #getStringFromClob}. Unless
 * indicated in the method name, no conversion process is applied to the raw result object, only casting. As such you may
 * need to retrieve the raw result object and convert it in your consuming code if (common conversions should be added
 * to this class following the current naming convention).<br/><br/>
 *
 * All <tt>getXXX</tt> calls may return null if the column or out bind was null. If the column or bind name does not exist
 * in the result map, an exception is raised. You can check the available column/bind names with {@link #getColumnNames}
 * or {@link #columnExists}.<br/><br/>
 */
public class UConStatementResult implements FxpUConStatementResult {

  /** May be ordered according to column positions if this was constructed from a ResultSet. */
  private final Map<String, Object> mColumnMap;

  /**
   * Constructs a new UConStatementResult from a ResultSet.
   * @param pResultSet
   * @return New UConStatementResult.
   * @throws SQLException If ResultSet read fails.
   */
  static UConStatementResult fromResultSet(ResultSet pResultSet)
  throws SQLException {
    return new UConStatementResult(SingleRowResultDeliverer.createObjectMapFromResultSet(pResultSet));
  }

  /**
   * Constructs a new UConStatementResult from an existing name to object map.
   * @param pColumnMap Map to use (a clone is created).
   * @return New UConStatementResult.
   */
  static UConStatementResult fromMap(Map<String, Object> pColumnMap) {
    return new UConStatementResult(new HashMap<String, Object>(pColumnMap));
  }

  private UConStatementResult(Map<String, Object> pColumnMap) {
    mColumnMap = pColumnMap;
  }

  public Object getObject(String pColumnName) {
    if(!mColumnMap.containsKey(pColumnName)) {
      throw new ExInternal("No result found for column " + pColumnName);
    }
    return mColumnMap.get(pColumnName);
  }

  /**
   * Gets a list of all the column or bind names available in this UConStatementResult. If this object was constructed
   * from a query result row, the list will be ordered according to the query's column list. For other cases the list's
   * order is undefined.
   * @return List of column or bind names in this StatementResult.
   */
  public List<String> getColumnNames() {
    return new ArrayList<String>(mColumnMap.keySet());
  }

  /**
   * Gets the String value for the given column name.
   */
  public String getString(String pColumnName) {
    return (String) getObject(pColumnName);
  }

  /**
   * Gets the String value for the given column name, when the actual object retrieved was a Clob.
   */
  public String getStringFromClob(String pColumnName) {
    return SQLTypeConverter.clobToString((Clob) getObject(pColumnName));
  }

  /**
   * Gets the Clob value for the given column name.
   */
  public Clob getClob(String pColumnName) {
    return (Clob) getObject(pColumnName);
  }

  /**
   * Gets the Blob value for the given column name.
   */
  public Blob getBlob(String pColumnName) {
    return (Blob) getObject(pColumnName);
  }

  /**
   * Converts the Clob for the given column name into a new DOM object.
   */
  public DOM getDOMFromClob(String pColumnName) {
    return SQLTypeConverter.clobToDOM((Clob) getObject(pColumnName));
  }

  /**
   * Converts the SQLXML for the given column name into a new DOM object. The SQLXML is read using the standard SQLXML reader.
   */
  public DOM getDOMFromSQLXML(String pColumnName) {
    return getDOMFromSQLXML(pColumnName, false);
  }

  /**
   * Converts the SQLXML for the given column name into a new DOM object.
   * @param pColumnName
   * @param pBinaryXML True if the SQLXML is encoded as Oracle binary XML.
   * @return
   */
  public DOM getDOMFromSQLXML(String pColumnName, boolean pBinaryXML) {
    return SQLTypeConverter.SQLXMLToDOM((SQLXML) getObject(pColumnName), pBinaryXML);
  }


  /**
   * Gets the Integer value for the given column name or null if the column value is null.
   *
   * @param pColumnName name of the column
   * @return Integer object or null
   */
  public Integer getInteger(String pColumnName) {
    Number lNumberValue = (Number) getObject(pColumnName);
    if (lNumberValue == null) {
      return null;
    }
    else {
      return lNumberValue.intValue();
    }
  }

  /**
   * Gets the Double value for the given column name or null if the column value is null.
   *
   * @param pColumnName name of the column
   * @return Double object or null
   */
  public Double getDouble(String pColumnName) {
    Number lNumberValue = (Number) getObject(pColumnName);
    if (lNumberValue == null) {
      return null;
    }
    else {
      return lNumberValue.doubleValue();
    }
  }

  /**
   * Gets the Long value for the given column name or null if the column value is null.
   *
   * @param pColumnName name of the column
   * @return Long object or null
   */
  public Long getLong(String pColumnName) {
    Number lNumberValue = (Number) getObject(pColumnName);
    if (lNumberValue == null) {
      return null;
    }
    else {
      return lNumberValue.longValue();
    }
  }

  /**
   * Gets the Number value for the given column name, when the actual object retrieved was a String.
   */
  public Number getNumberFromString(String pColumnName) {
    return Double.parseDouble((String) getObject(pColumnName));
  }

  /**
   * Gets the java.util.Date value for the given column name.
   */
  public Date getDate(String pColumnName) {
    return (Date) getObject(pColumnName);
  }

  /**
   * Tests if the column or bind of the given name exists in this UConStatementResult. The column's value may be null
   * when it is retrieved.
   * @param pColumnName Column or bind name to check.
   * @return True if the column exists in this UConStatementResult.
   */
  public boolean columnExists(String pColumnName) {
    return mColumnMap.containsKey(pColumnName);
  }

  /**
   * Tests if the column or bind of the given name has a null result object associated with it. If the column/bind does
   * not exist in this UConStatementResult, this will return true.
   * @param pColumnName Column or bind name to check.
   * @return True if the column is null or does not exist, false otherwise.
   */
  public boolean isNull(String pColumnName) {
    return mColumnMap.get(pColumnName) == null;
  }
}
