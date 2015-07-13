package net.foxopen.fox.database.storage.dom;

import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.QueryResultDeliverer;
import net.foxopen.fox.database.sql.out.JDBCResultAdaptor;
import net.foxopen.fox.database.sql.out.ResultSetAdaptor;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExDBTooMany;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooMany;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ResultDeliverer for selecting an XMLWorkDoc's LOB locator and any addition columns which the developer may need to access
 * in the fm:validation block of the storage location.
 *
 * For a scalar query, the single result object is assumed to be the LOB locator. For queries returning multiple columns,
 * the LOB locator must be called "DOM". The query must always return exactly 1 row.
 */
class XMLWorkDocResultDeliverer
implements QueryResultDeliverer{

  private Object mDOMColumn;
  private DOM mAdditionalColumnXML = null;
  private static final String DOM_COLUMN_NAME = "DOM";

  @Override
  public void deliver(ExecutableQuery pQuery)
  throws ExDB {

    ResultSet lResultSet = pQuery.getResultSet();

    try {
      if(!lResultSet.next()) {
        throw new ExDBTooFew("XMLWorkDocResultDeliverer requires exactly 1 row, got 0\n\n" + pQuery.generateErrorMessage());
      }

      if(lResultSet.getMetaData().getColumnCount() == 1) {
        //For a scalar result, assume the selected column is the LOB locator we're after
        mDOMColumn = lResultSet.getObject(1);
      }
      else {
        //Multiple columns - search through to find the LOB column, populate other columns into the DOM
        mAdditionalColumnXML = DOM.createDocument("select-result");
        JDBCResultAdaptor lAdaptor = new ResultSetAdaptor(lResultSet);

        for(int i = 1; i <= lResultSet.getMetaData().getColumnCount(); i++) {
          String lColumnName = lResultSet.getMetaData().getColumnName(i);
          if(DOM_COLUMN_NAME.equals(lColumnName)) {
            mDOMColumn = lResultSet.getObject(i);
          }
          else {
            try {
              SQLTypeConverter.applyValueToDOM(lAdaptor, i, lResultSet.getMetaData().getColumnType(i), mAdditionalColumnXML.create1E(lColumnName));
            }
            catch (ExTooMany e) {
              throw new ExInternal("Unable to create target element for additional SELECT statement column", e);
            }
          }
        }

        //Check we found a DOM column
        if(mDOMColumn == null) {
          throw new ExInternal("Select statement which returns multiple columns must return a column called '" + DOM_COLUMN_NAME + "'");
        }
      }

      if(lResultSet.next()) {
        //Null out due to failure
        mDOMColumn = null;
        mAdditionalColumnXML = null;
        throw new ExDBTooMany("XMLWorkDocResultDeliverer requires exactly 1 row, got more than 1\n\n" + pQuery.generateErrorMessage());
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
   * Gets the LOB locator retrieved by the select statement.
   * @return LOB locator
   */
  public Object getDOMColumn() {
    return mDOMColumn;
  }

  /**
   * Gets the XML representation of any additional columns retrieved by the SELECT statement, or null if there were no
   * additional columns.
   * @return Column XML or null.
   */
  public DOM getAdditionalColumnXML() {
    return mAdditionalColumnXML;
  }
}
