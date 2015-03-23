package net.foxopen.fox.plugin.api.database;

import java.sql.Blob;
import java.sql.Clob;

import java.util.Date;
import java.util.List;

import net.foxopen.fox.plugin.api.dom.FxpDOM;


public interface FxpUConStatementResult {
  /**
   * This method is not recommended. It is exposed for edge cases where specific classes are needed rather
   * than the standard exposed ones.
   */
  Object getObject(String pColumnName);

  /**
   * Gets a list of all the column or bind names available in this UConStatementResult. If this object was constructed
   * from a query result row, the list will be ordered according to the query's column list. For other cases the list's
   * order is undefined.
   * @return List of column or bind names in this StatementResult.
   */
  List<String> getColumnNames();

  /**
   * Gets the String value for the given column name.
   */
  String getString(String pColumnName);

  /**
   * Gets the String value for the given column name, when the actual object retrieved was a Clob.
   */
  String getStringFromClob(String pColumnName);

  /**
   * Gets the Clob value for the given column name.
   */
  Clob getClob(String pColumnName);

  /**
   * Gets the Blob value for the given column name.
   */
  Blob getBlob(String pColumnName);

  /**
   * Converts the Clob for the given column name into a new DOM object.
   */
  FxpDOM getDOMFromClob(String pColumnName);

  /**
   * Converts the SQLXML for the given column name into a new DOM object.
   */
  FxpDOM getDOMFromSQLXML(String pColumnName);

  /**
   * Gets the int value for the given column name.
   */
  Integer getInteger(String pColumnName);

  /**
   * Gets the double value for the given column name.
   */
  Double getDouble(String pColumnName);

  Long getLong(String pColumnName);

  /**
   * Gets the Number value for the given column name, when the actual object retrieved was a String.
   */
  Number getNumberFromString(String pColumnName);

  /**
   * Gets the java.util.Date value for the given column name.
   */
  Date getDate(String pColumnName);

  /**
   * Tests if the column or bind of the given name exists in this UConStatementResult. The column's value may be null
   * when it is retrieved.
   * @param pColumnName Column or bind name to check.
   * @return True if the column exists in this UConStatementResult.
   */
  boolean columnExists(String pColumnName);

  /**
   * Tests if the column or bind of the given name has a null result object associated with it. If the column/bind does
   * not exist in this UConStatementResult, this will return true.
   * @param pColumnName Column or bind name to check.
   * @return True if the column is null or does not exist, false otherwise.
   */
  boolean isNull(String pColumnName);
}
