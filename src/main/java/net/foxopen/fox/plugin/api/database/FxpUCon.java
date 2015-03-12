package net.foxopen.fox.plugin.api.database;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLXML;
import java.sql.Savepoint;

import java.util.List;

import net.foxopen.fox.database.UConResultSet;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTimeout;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExDBTooMany;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.plugin.api.database.parser.FxpParsedStatement;
import net.foxopen.fox.plugin.api.dom.FxpDOM;


public interface FxpUCon <PS extends FxpParsedStatement, UBM extends FxpUConBindMap>{
  /**
   * Put the connection in a queue to await recycling by the job in ConnectionAgent
   */
  void closeForRecycle();

  /**
   * Actually close the connection and don't release it back to the pool
   */
  void closeNoRecycle();

  SQLXML convertClobToSQLXML(Clob pClob);
  
  /**
   * Set the module info for the current connection to help identify what the connection is doing
   *
   * @param pModuleInfo New module info string, 48 characters only. Any more and Oracle will truncate it
   * @return True if module info was set
   */
  boolean setModuleInfo(String pModuleInfo);

  String getModuleInfo();

  /**
   * Set the client info for the current connection to help identify what the connection is doing
   *
   * @param pClientInfo New client info string, 64 characters only. Any more and Oracle will truncate it
   * @return True if the client info was set
   */
  boolean setClientInfo(String pClientInfo);

  void commit() throws ExServiceUnavailable;

  /**
   * Rolls back the connection's current transaction.
   * @throws ExDB If rollback fails.
   */
  void rollback() throws ExDB;
  
  @Deprecated
  void rollbackTo(String pSavepoint) throws ExDB;

  void rollbackTo(Savepoint pSavepoint) throws ExDB;

  /**
   * Sets a savepoint within the connection's current transaction.
   * @param pSavepoint Name of savepoint to set.
   */
  Savepoint savepoint(String pSavepoint);

  Clob getTemporaryClob();

  Blob getTemporaryBlob();

  String getDatabaseName();

  /**
   * Tests if a transaction is currently active on this connection.
   * @return True if a transaction is active, false otherwise.
   */
  boolean isTransactionActive();  

  /**
   * Gets the underlying database connection object which this UCon is wrapping.
   * @return Database connection
   */
  Connection getJDBCConnection();
  
  //Java compiler doesn't allow the type inference from parameter to method return type when these are used in a JAR,
  //plus ScalarResultType would have to be wrapped because it references DOM, so provide explicit methods for scalar queries.
  
  //PN TODO JAVADOC
  FxpDOM queryScalarDOM(PS pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;
  
  FxpDOM queryScalarDOM(PS pStatement, UBM pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;

  Clob queryScalarClob(PS pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;
  
  Clob queryScalarClob(PS pStatement, UBM pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;
  
  Blob queryScalarBlob(PS pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;
  
  Blob queryScalarBlob(PS pStatement, UBM pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;

  String queryScalarString(PS pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;
  
  String queryScalarString(PS pStatement, UBM pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;
  
  Object queryScalarObject(PS pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;
  
  Object queryScalarObject(PS pStatement, UBM pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;
  

  /**
   * Runs a query which is expected to return a single row. See {@link UCon} for more details on how queries
   * are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be positionally bound into the statement.
   * @return A UConStatementResult representing the selected row.
   * @throws ExDBTimeout If a lock acquisition times out.
   * @throws ExDBTooFew If 0 rows are selected.
   * @throws ExDBTooMany If more than 1 row is selected.
   * @throws ExDB If any other database failure occurs.
   */
  FxpUConStatementResult querySingleRow(PS pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;

  /**
   * Runs a query which is expected to return a single row. See {@link UCon}for more details on how queries
   * are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be bound into the statement by name.
   * @return A UConStatementResult representing the selected row.
   * @throws ExDBTimeout If a lock acquisition times out.
   * @throws ExDBTooFew If 0 rows are selected.
   * @throws ExDBTooMany If more than 1 row is selected.
   * @throws ExDB If any other database failure occurs.
   */
  FxpUConStatementResult querySingleRow(PS pStatement, UBM pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB;

  /**
   * Runs a query which is expected to return a muliple rows. All rows are selected into an in-memory list so this method
   * should only be used to select small result sets. This method may select no rows, in which case an empty list is returned.
   * See {@link UCon}for more details on how queries are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be positionally bound into the statement.
   * @return A list of UConStatementResults representing the selected rows.
   * @throws ExDB If any database failure occurs.
   */
  List<? extends FxpUConStatementResult> queryMultipleRows(PS pStatement, Object... pBinds) throws ExDB;

  /**
   * Runs a query which is expected to return a muliple rows. All rows are selected into an in-memory list so this method
   * should only be used to select small result sets. This method may select no rows, in which case an empty list is returned.
   * See {@link UCon}for more details on how queries are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be bound into the statement by name.
   * @return A list of UConStatementResults representing the selected rows.
   * @throws ExDB If any database failure occurs.
   */
  List<? extends FxpUConStatementResult> queryMultipleRows(PS pStatement, UBM pBinds) throws ExDB;

  /**
   * Runs a query and gets its ResultSet. Consumers <b>MUST</b> ensure they call {@link UConResultSet#close}to close
   * the Statement and its ResultSet after they are finished with it, ideally in a <tt>finally</tt> block. Use this method
   * when you are dealing with a large dataset or need to implement custom datatype retrieval.
   * See {@link UCon}for more details on how queries are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be positionally bound into the statement.
   * @return A UConResultSet from which you can retrieve a JDBC ResultSet. This must be closed following any processing.
   * @throws ExDB If any database failure occurs.
   */
  FxpUConResultSet queryResultSet(PS pStatement, Object... pBinds) throws ExDB;

  /**
   * Runs a query and gets its ResultSet. Consumers <b>MUST</b> ensure they call {@link UConResultSet#close}to close
   * the Statement and its ResultSet after they are finished with it, ideally in a <tt>finally</tt> block. Use this method
   * when you are dealing with a large dataset or need to implement custom datatype retrieval.
   * See {@link UCon}for more details on how queries are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be bound into the statement by name.
   * @return A UConResultSet from which you can retrieve a JDBC ResultSet. This must be closed following any processing.
   * @throws ExDB If any database failure occurs.
   */
  FxpUConResultSet queryResultSet(PS pStatement, UBM pBinds) throws ExDB;

  /**
   * Executes an API, which may be a DML statement or PL/SQL block. You may bind OUT bind objects (i.e. {@link UCon#bindOutString}) -
   * the resultant OUT objects are added to the UConStatementResult which this method returns. See {@link UCon}for more
   * details on how APIs are executed.
   * @param pStatement Statement to execute.
   * @param pBinds Objects to be positionally bound into the statement.
   * @return UConStatementResult containing any OUT parameters.
   * @throws ExDB If any database failure occurs.
   */
  FxpUConStatementResult executeAPI(PS pStatement, Object... pBinds) throws ExDB;

  /**
   * Executes an API, which may be a DML statement or PL/SQL block. You may bind OUT bind objects (i.e. {@link UCon#bindOutString}) -
   * the resultant OUT objects are added to the UConStatementResult which this method returns. See {@link UCon}for more
   * details on how APIs are executed.
   * @param pStatement Statement to execute.
   * @param pBinds Objects to be bound into the statement by name.
   * @return UConStatementResult containing any OUT parameters.
   * @throws ExDB If any database failure occurs.
   */
  FxpUConStatementResult executeAPI(PS pStatement, UBM pBinds) throws ExDB;

  /**
   * Executes an API which does not contain any bind variables.
   * @param pStatement Statement string to execute as an API.
   * @param pPurpose Description of the API for logging and debug.
   * @return The number of records affected if this API was a DML statement, or 0.
   * @throws ExDB If statement execution fails.
   */
  int executeAPI(String pStatement, String pPurpose) throws ExDB;
}
