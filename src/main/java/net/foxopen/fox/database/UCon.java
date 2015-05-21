package net.foxopen.fox.database;

import net.foxopen.fox.SQLUtil;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.database.sql.*;
import net.foxopen.fox.database.sql.bind.*;
import net.foxopen.fox.database.sql.out.CallableStatementAdaptor;
import net.foxopen.fox.database.sql.out.JDBCResultAdaptor;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.*;
import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.plugin.api.database.FxpUCon;
import net.foxopen.fox.track.Track;
import oracle.jdbc.OracleConnection;
import oracle.sql.BLOB;
import oracle.sql.CLOB;
import oracle.sql.OPAQUE;
import oracle.xdb.XMLType;
import oracle.xml.binxml.*;

import java.io.Closeable;
import java.sql.*;
import java.util.*;
import java.util.Date;


/**
 * A wrapper for a database connection which provides helper methods for basic statement execution. Do NOT use the deprecated
 * methods on this class - they should be gradually factored out and should be completely removed by the FOX 5 release.<br/><br/>
 *
 * <b>Statement Execution</b><br/><br/>
 *
 * This class allows consumers to access the database in the following ways:
 *
 * <ul>
 * <li>Select a single scalar result with a SELECT statement - see <tt>queryScalarResult</tt></li>
 * <li>Select a single row using a SELECT statement - see <tt>querySingleRow</tt></li>
 * <li>Select a list of many rows using a SELECT statement - see <tt>queryMultipleRows</tt></li>
 * <li>Retrieve a JDBC ResultSet from a SELECT statement - see <tt>queryResultSet</tt></li>
 * <li>Execute a DML statement or PL/SQL API - see <tt>executeAPI</tt></li>
 * </ul>
 *
 * These methods cover simple use cases and only allow basic type conversions, some of which are not type safe. If your consuming
 * code requires more robust type conversion you should consider writing your own {@link net.foxopen.fox.database.sql.bind.BindObjectProvider BindObjectProvider}
 * and/or {@link net.foxopen.fox.database.sql.ResultDeliverer ResultDeliverer} to handle complex or dynamic use cases.
 * See {@link UConStatementResult} for more information on how SQL results are converted to Java objects.<br/><br/>
 *
 * <b>Statement Binding</b><br/><br/>
 *
 * The methods listed above each have two signatures which provide different binding mechanism. A varargs signature is
 * provided for statements which only require a few positional binds (or have no binds). For statements which have many binds, a
 * signature is provided which takes a {@link UConBindMap}. This allows named binding into statements which reuse the same bind name.<br/><br/>
 *
 * UCon supports binding the following classes from a varargs bind parameter or a UConBindMap:
 *
 * <ul>
 * <li>BindObject</li>
 * <li>String</li>
 * <li>java.util.Date</li>
 * <li>DOM</li>
 * <li>Number</li>
 * <li>Blob</li>
 * <li>Clob</li>
 * </ul>
 *
 * <tt>null</tt> is allowed and is bound as a String by default. BindObjects are bound in directly without alteration.
 * Other classes are wrapped in the default BindObject for their class. If you need to bind unsupported classes, you should
 * implement your own BindObject and bind that in. <br/><br/>
 *
 * <b>Out Binding</b><br/><br/>
 *
 * PL/SQL APIs allow OUT binds. To use these, you should use the <tt>bindOutXXX</tt> static methods to create special OUT
 * BindObjects. The resultant OUT value is available in the returned UConStatementResult. The result "column name" is the
 * bind variable's name.<br/><br/>
 *
 * <b>Example Usage</b><br/><br/>
 *
 * This demonstrates how to set up a UCon bind map, execute the statement and retrieve a result:
 * <pre>
ParsedStatement lParsedStatement = StatementParser.parse(
  "BEGIN :bind_out := :bind_in || ' modified'; END;");

UConBindMap lBindMap = new UConBindMap();
lBindMap.defineBind(":bind_in", "my string value");
lBindMap.defineBind(":bind_out", UCon.bindOutString());

UConStatementResult lAPIResult = lUCon.executeAPI(lParsedStatement, lBindMap);

String lOutString = lAPIResult.getString(":bind_out");
</pre>
 *
 * <b>Exception Handling</b><br/><br/>
 *
 * Some methods declare that they throw subclasses of the ExDB exception (i.e. ExDBTooFew) in addition to ExDB itself.
 * If these are important to your consumer you should ensure that they are caught and handled BEFORE catching ExDB.
 * Otherwise it is OK to catch only ExDB and rethrow accordingly.
 */
public class UCon implements FxpUCon<ParsedStatement, UConBindMap>, Closeable {

  /**
   * Global info to be set on connections (usually changed when FOX starts up)
   */
  private static String gGlobalInfo = "UCon.java";

  // TODO - NP - Review all the tracing code, it's not good enough
  // Queries to facilitate !TRACE
  private static final String gPlsqlSetTraceFile =
    "BEGIN EXECUTE IMMEDIATE 'ALTER SESSION SET tracefile_identifier='||:1; END;";
  private static final String gPlsqlStartTracing =
    "BEGIN EXECUTE IMMEDIATE 'ALTER SESSION SET EVENTS ''' || :1 || ' trace name context forever, level ' || :2 || ''''; END;";
  private static final String gPlsqlStopTracing =
    "BEGIN EXECUTE IMMEDIATE 'ALTER SESSION SET EVENTS ''' || :1 || ' trace name context off'''; END;";

  // TODO - NP - Review all constant queries like this too, abstract them perhaps to make them overloadable/configurable
  private static final String GET_TRANSACTION_ID_STATEMENT = "BEGIN :tran_id := DBMS_TRANSACTION.LOCAL_TRANSACTION_ID(FALSE); END;";
  private static final ParsedStatement GET_TRANSACTION_ID_PARSED_STATEMENT = StatementParser.parseSafely(GET_TRANSACTION_ID_STATEMENT, "Get Transaction ID");

  private static final String SET_CLIENT_INFO_STATEMENT =  "BEGIN dbms_application_info.set_client_info(:1); END;";

  public static final String SET_MODULE_INFO_STATEMENT = "BEGIN dbms_application_info.set_module(TO_CHAR(SYSDATE,'MM/DD-HH24MI.SS: ')||:1, :2); END;";
  private static final ParsedStatement SET_MODULE_INFO_PARSED_STATEMENT = StatementParser.parseSafely(SET_MODULE_INFO_STATEMENT, "Set Module Info");

  private final static ParsedStatement SELECT_DB_NAME_PARSED_STATEMENT = StatementParser.parseSafely("SELECT description FROM database_info", "Get Database Name");

  /**
   * Store a reference to the pool this connection comes from
   */
  private final ConnectionPool mConnectionPool;

  /**
   * Store the base sql.connection, this is probably a wrapper from the pool and needs unwrapping instead of casting
   */
  private final Connection mDatabaseConnection;

  private final BinXMLProcessor mBinXMLProcessor; //NP/PN - Moved in from conagent, keep an eye on performance impact of moving here.

  private boolean mOpen = false;

  private String mModuleInfo = null;
  private String mDatabaseInfo = null;

  private final List<String> mSavepointNamesUsedList = new ArrayList<>(2);

  private String mTraceEvent;
  private boolean mIsTracing = false;

  /**
   * Constructs a UCON from a jdbc connection. Ensure methods which make use of the connection pool are not called as these
   * will cause an error. Such as recycle.
   *
   * @param pDatabaseCon a jdbc connection
   * @param pPurpose String identifying the ucon
   * @return UCon for running statements against
   */
  public static UCon createUCon(Connection pDatabaseCon, String pPurpose) {
    return new UCon(null, pDatabaseCon, pPurpose);
  }

  UCon(ConnectionPool pConPool, Connection pDatabaseCon, String pPurpose) {
    mConnectionPool = pConPool;
    mDatabaseConnection = pDatabaseCon;

    Track.pushDebug("CreateBinXMLProcessor");
    try {
      DBBinXMLMetadataProvider lRepository = BinXMLMetadataProviderFactory.createDBMetadataProvider();
      lRepository.associateDataConnection(mDatabaseConnection);
      mBinXMLProcessor = BinXMLProcessorFactory.createProcessor(lRepository);
    }
    catch(BinXMLException  e) {
      throw new ExInternal("UCon BinaryXML Processor construction failed", e);
    }
    finally {
      Track.pop("CreateBinXMLProcessor");
    }

    setModuleInfo(pPurpose);

    mOpen = true;
  }

  /**
   * Put the connection in a queue to await recycling by the job in ConnectionAgent
   */
  public void closeForRecycle() {
    if (!mOpen) {
      FoxLogger.getLogger().warn("closeForRecycle called twice on a UCon, module info = {}", mModuleInfo, Thread.currentThread().getStackTrace());
    }
    else {
      if (mIsTracing) {
        stopTracing();
      }

      if(mDatabaseConnection != null) {
        // Schedule event in another thread to clear the package state and close it, releasing it back to the pool
        ConnectionAgent.checkInForRecycle(this);
      }
      mOpen = false;
    }
  }

  /**
   * Actually close the connection and don't release it back to the pool
   */
  public void closeNoRecycle() {
    if (!mOpen) {
      FoxLogger.getLogger().warn("close called twice on a UCon, module info = {}", mModuleInfo, Thread.currentThread().getStackTrace());
    }
    else {
      if (mIsTracing) {
        stopTracing();
      }

      if(mDatabaseConnection != null) {
        ConnectionAgent.closeConnection(this);
      }
      mOpen = false;
    }
  }

  @Deprecated
  private ExDBSyntax errorConvert(SQLException pSQLException, String pMsg) throws ExDBTimeout, ExDBSyntax {
    int errorInt = pSQLException.getErrorCode();

    // Throw timeout exception for:
    // ORA-00054 resource busy and acquire with NOWAIT specified
    // ORA-00051 timeout occurred while waiting for a resource
    if(errorInt == 51 || errorInt == 54) {
       throw new ExDBTimeout(pMsg, pSQLException);
    }

    // Throw timeout exception for:
    // ORA-00001: unique constraint violated
    if(errorInt == 1) {
       throw new ExDBDuplicateValue(pMsg, pSQLException);
    }

    // Throw other exception
    throw new ExDBSyntax(pMsg, pSQLException);
  }

  /**
   * Converts a SQLException to a subtype of ExDB based on the SQLException's error code.
   * @param pSQLException Original exception to convert.
   * @param pMsg Message to include in the new exception.
   * @return One of ExDBTimeout, ExDBDuplicateValue or ExDBSytnax depending on the type of error.
   */
  public static ExDB convertSQLException(SQLException pSQLException, String pMsg) {
    int errorInt = pSQLException.getErrorCode();

    // Throw timeout exception for:
    // ORA-00054 resource busy and acquire with NOWAIT specified
    // ORA-00051 timeout occurred while waiting for a resource
    if(errorInt == 51 || errorInt == 54) {
      return new ExDBTimeout(pMsg, pSQLException);
    }

    // ORA-01555 snapshot too old
    // ORA-08180 no snapshot found based on specified time
    // ORA-08181 specified number is not a valid system change number
    if(errorInt == 1555 || errorInt == 8180 || errorInt == 8181) {
      return new ExDBFlashback(pMsg, pSQLException);
    }

    // Throw timeout exception for:
    // ORA-00001: unique constraint violated
    if(errorInt == 1) {
      return new ExDBDuplicateValue(pMsg, pSQLException);
    }

    // Throw other exception
    return new ExDBSyntax(pMsg, pSQLException);
  }

  public SQLXML convertClobToSQLXML(Clob pClob) {
    //TODO make database agnostic
    try {
      return XMLType.createXML(mDatabaseConnection.unwrap(OracleConnection.class), (CLOB) pClob);
    }
    catch(SQLException e) {
      throw new ExInternal("XMLType can not be created",e);
    }
  }

  /**
   * @deprecated New binding allows DOMs to be bound directly into statements, do that instead.
   */
  @Deprecated
  public XMLType createXmlType(DOM pDOM) throws ExInternal {
    throw new UnsupportedOperationException();
//    try {
//      return XMLType.createXML(mDatabaseConnection.unwrap(OracleConnection.class), pDOM.outputNodeToString("  ",false, false, false, false, false, true));
//    }
//    catch(SQLException e) {
//      throw new ExInternal("XMLType can not be created",e);
//    }
  }

  public boolean isOpen() {
    try {
      if(mDatabaseConnection != null && !mDatabaseConnection.isClosed()) {
        return true;
      }
    }
    catch (SQLException e) {
      return false;
    }
    return false;
  }

  private void checkOpen()
  throws ExInternal {
    if(!isOpen()) {
      throw new ExInternal("Connection is closed or has not been assigned");
    }
  }

  /** Run a PL/SQL Block, INSERT, UPDATE, or DELETE statement
   * @return Returned variables
   * @deprecated Replace this with a call to executeAPI()
   */
  @Deprecated
  public synchronized Object[] executeCall(String stmt, Object inoutbind[], char[] pDirection)
  throws ExInternal, ExDBSyntax, ExDBTimeout
  {
    checkOpen();
    CallableStatement sel = null;
    try {
      try {
        sel = mDatabaseConnection.prepareCall(stmt);
      }
      catch (SQLException e)
      {
        throw new ExDBSyntax(getClass().getName()+"::executeDML preparing: "+stmt, e);
      }

      try {
        if(inoutbind!=null) {
          BIND_LOOP: for(int i=0; i<inoutbind.length; i++) {
            Object bind = inoutbind[i];
            if(pDirection[i]!='O') {
              if(bind == null){
                sel.setString(i+1, "");
              }else if (bind instanceof String) {
                sel.setString(i+1,(String)bind);
              } else if (bind instanceof BLOB) {
                sel.setBlob(i+1,(BLOB)bind);
              } else if (bind instanceof CLOB) {
                sel.setClob(i+1,(CLOB)bind);
              } else if (bind instanceof XMLType) {
                sel.setObject(i+1,(XMLType)bind);
              } else if (bind instanceof Integer) {
                sel.setInt(i+1,((Integer) bind).intValue());
              } else if (bind instanceof Boolean) {
                sel.setBoolean(i+1,((Boolean) bind).booleanValue());
              } else {
                throw new ExInternal("Can not bind variable, because it is not either a String or Clob.\nFound: "+bind.getClass().getName());
              }
            }
            if(pDirection[i]!='I') {
              if (bind == CLOB.class) {
                sel.registerOutParameter(i+1, Types.CLOB);
              }
              else {
                sel.registerOutParameter(i+1, Types.VARCHAR);
              }
            }
          } // BIND_LOOP
        }
      }
      catch (SQLException e) {
        throw new ExDBSyntax(getClass().getName()+"::executeCall binding: "+stmt, e);
      }

      try {
        int rowCound = sel.executeUpdate();
      }
      catch (SQLException e) {
        throw errorConvert(e, "::executeCall executing: "+stmt);
      }

      try {
        if(inoutbind!=null) {
          FETCH_LOOP: for(int i=0; i<inoutbind.length; i++) {
            if(pDirection[i]!='I') {
              if(inoutbind[i] == CLOB.class) {
                inoutbind[i] = sel.getClob(i+1);
              }
              else {
                inoutbind[i] = sel.getString(i+1);
              }
            }
          } // FETCH_LOOP
        }
      }
      catch (SQLException e) {
        throw new ExDBSyntax(getClass().getName()+"::executeCall fetching: "+stmt, e);
      }

      return inoutbind;
    }
    finally {
      SQLUtil.cleanUp(sel);
    }
  }

  /** Run a PL/SQL Block, INSERT, UPDATE, or DELETE statement
   *
   * @return Returned variables
   * @deprecated Replace this with a call to executeAPI()
   */
  @Deprecated
  public synchronized int executeDML(String stmt, Object inbind[])
  throws ExInternal, ExDBSyntax, ExDBTimeout {
    int rowCount = -1;
    checkOpen();
    PreparedStatement sel = null;

    Track.pushDebug("executeDML");
    try {
      Track.pushDebug("Prepare");
      try {
        sel = mDatabaseConnection.prepareStatement(stmt);
      }
      catch (SQLException e) {
        throw new ExDBSyntax(getClass().getName()+"::executeDML preparing: "+stmt, e);
      }
      finally {
        Track.pop("Prepare");
      }

      Track.pushDebug("Bind");
      try {
        if(inbind!=null) {
          for(int i=0; i<inbind.length; i++) {
            Object bind = inbind[i];
            if (bind instanceof String) {
              sel.setString(i+1, (String)bind);
            } else if (bind instanceof XMLType) {
              sel.setObject(i+1, (XMLType)bind);
            } else if (bind instanceof DOM) {
              sel.setObject(i+1, createXmlType((DOM) bind));
            } else if (bind instanceof CLOB) {
              sel.setClob(i+1, (CLOB)bind);
            } else if (bind instanceof BLOB) {
              sel.setBlob(i+1, (BLOB)bind);
            } else {
              throw new ExInternal("Can not bind variable, because it is not either a String or Clob.\nFound: "+(bind==null?"null":bind.getClass().getName()));
            }
          }
        }
      }
      catch (SQLException e) {
        throw new ExDBSyntax(getClass().getName()+"::executeDML binding: "+stmt, e);
      }
      finally {
        Track.pop("Bind");
      }

      Track.pushDebug("Execute");
      try {
        rowCount = sel.executeUpdate();
      }
      catch (SQLException e) {
        throw errorConvert(e, "::executeDML executing: "+stmt);
      }
      finally {
        Track.pop("Execute");
      }
    }
    finally {
      SQLUtil.cleanUp(sel);
      Track.pop("executeDML");
    }
    return rowCount;
  }

  /**
   * @deprecated Replace this with a call to queryMultipleRows() or queryResultSet() (or implement a new Deliverer)
   */
  @Deprecated
  public synchronized List executeSelectAllRows(String stmt, String inbind[], boolean pIncludeColumnNames, boolean pConvertNullsToTypeClass)
  throws ExDBSyntax, ExDBTimeout {
    throw new UnsupportedOperationException("Not allowed");
  }

  /**
   * @deprecated Replace this with a call to querySingleRow()
   */
  @Deprecated
  public synchronized Object[] selectOneRow(String pSQLStatement, String inbind[])
  throws
    ExInternal
  , ExDBSyntax, ExDBTooFew, ExDBTooMany, ExDBTimeout
  {
    checkOpen();
    PreparedStatement sel = null;
    ResultSet rs          = null;
    ResultSetMetaData meta;
    try {
      // Define cursor
      Track.pushDebug("Prepare");
      try {
        sel = mDatabaseConnection.prepareStatement(pSQLStatement);
      }
      catch (SQLException e) {
        throw new ExDBSyntax("selectOneRow Error: "+pSQLStatement, e);
      }
      finally {
        Track.pop("Prepare");
      }

      // Bind inward variables
      if(inbind != null) {
        Track.pushDebug("Bind");
        try {
          for(int i=0;i<inbind.length;i++) {
            sel.setString(i+1, inbind[i]);
          }
        }
        catch (SQLException e) {
          throw new ExDBSyntax("selectOneRow Error: "+pSQLStatement, e);
        }
        finally {
          Track.pop("Bind");
        }
      }

      // Execute statement
      Track.pushDebug("Execute");
      try {
         rs = sel.executeQuery();
      }
      catch (SQLException e) {
        throw errorConvert(e, "selectOneRow Error:  "+pSQLStatement);
      }
      finally {
        Track.pop("Execute");
      }

      Track.pushDebug("MetaData");
      try {
         meta = rs.getMetaData();
      }
      catch (SQLException e) {
        throw errorConvert(e, "selectOneRow Error:  "+pSQLStatement);
      }
      finally {
        Track.pop("MetaData");
      }

      // Fetch 1
      Track.pushDebug("Fetch1");
      try {
        if (!rs.next()) {
          throw new ExDBTooFew("No rows returned: "+pSQLStatement);
        }
      }
      catch (SQLException e) {
        throw new ExDBTooFew("selectOneRow Error: "+pSQLStatement, e);
      }
      finally {
        Track.pop("Fetch1");
      }

      // Process each returned column
      Object[] lObjectArray;
      Track.pushDebug("GetColumns");
      try {
        lObjectArray = new Object[meta.getColumnCount()];
        int lType;
        for(int cc=0; cc <meta.getColumnCount(); cc++) {
          lType = meta.getColumnType(cc+1);
          switch(meta.getColumnType(cc+1)) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
              lObjectArray[cc] = rs.getString(cc+1);
              break;
            case Types.INTEGER:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
              lObjectArray[cc] = new Double(rs.getDouble(cc+1));
              break;
            case Types.CLOB:
              lObjectArray[cc] = rs.getClob(cc+1);
              break;
            case Types.BLOB:
              lObjectArray[cc] = rs.getBlob(cc+1);
              break;
            case 2007: // sys.XMLType
              OPAQUE op = (OPAQUE) rs.getObject(cc+1);
              lObjectArray[cc] = XMLType.createXML(op);
              break;
            default:
              throw new ExInternal("Select one row does not support datatype code yet "+lType); //////////////////////
          }
        }
      }
      catch (SQLException sqlx) {
        throw new ExInternal("select one row error decoding column types");
      }
      finally {
       Track.pop("GetColumns");
      }

      // Fetch 2
      Track.pushDebug("Fetch2");
      try {
        if(rs.next()) {
          throw new ExDBTooMany("selectOneRow Error:  "+pSQLStatement);
        }
      }
      catch (SQLException e) {
      }
      finally {
        Track.pop("Fetch2");
      }

      // Return input stream
      return lObjectArray;
    }
    finally{
      SQLUtil.cleanUp(sel, rs);
    }
  }

  /**
   * Set the module info for the current connection to help identify what the connection is doing
   *
   * @param pModuleInfo New module info string, 48 characters only. Any more and Oracle will truncate it
   * @return True if module info was set
   */
  public final boolean setModuleInfo(String pModuleInfo) {
    mModuleInfo = pModuleInfo;
    if (mModuleInfo.length() > 48) {
      Track.info("UConSetModuleInfo", "String longer than 48 characters, data will be truncated: " + mModuleInfo);
    }

    try {
      executeAPI(SET_MODULE_INFO_PARSED_STATEMENT, mModuleInfo, gGlobalInfo);
    }
    catch(Throwable x) {
      FoxLogger.getLogger().error("Failed to set module info on UCon", x);
      return false;
    }

    return true;
  }

  public final String getModuleInfo() {
    return mModuleInfo;
  }

  /**
   * Set the client info for the current connection to help identify what the connection is doing
   *
   * @param pClientInfo New client info string, 64 characters only. Any more and Oracle will truncate it
   * @return True if the client info was set
   */
  public final boolean setClientInfo(String pClientInfo) {
    if (pClientInfo.length() > 64) {
      Track.info("UConSetClientInfo", "String longer than 64 characters, data will be truncated: " + pClientInfo);
    }

    try {
      //Skip the standard statement execution mechanism for this method as it is called from the execute statement code so we'd get an infinite loop
      PreparedStatement lPrepareStatement = mDatabaseConnection.prepareStatement(SET_CLIENT_INFO_STATEMENT);
      lPrepareStatement.setString(1, pClientInfo);
      lPrepareStatement.execute();
      lPrepareStatement.close();
    }
    catch(Throwable x) {
//      return false;
      throw new ExInternal("Failed to set client info", x);
    }

    return true;
  }

  /**
   * Update the global info that is set
   *
   * @param pGlobalInfo New Global Info string, 32 characters only. Any more and Oracle will truncate it
   */
  public static final void setGlobalInfo(String pGlobalInfo) {
    if (pGlobalInfo.length() > 32) {
      Track.info("UConSetGlobalInfo", "String longer than 32 characters, data will be truncated: " + pGlobalInfo);
    }

    gGlobalInfo = pGlobalInfo;
  }

  // Commit transaction
  public void commit ()
  throws ExServiceUnavailable {
    checkOpen();
    Track.pushInfo("Commit");
    try {
      mDatabaseConnection.commit();
      mSavepointNamesUsedList.clear();
    }
    catch (SQLException e) {
      throw new ExServiceUnavailable("Failed to commit transaction", e);
    }
    finally {
      Track.pop("Commit");
    }
  }

  /**
   * Rolls back the connection's current transaction.
   * @throws ExDB If rollback fails.
   */
  public void rollback()
  throws ExDB {
    if(mDatabaseConnection != null) {
      Track.pushInfo("Rollback");
      try{
        mDatabaseConnection.rollback();
        mSavepointNamesUsedList.clear();
      }
      catch (SQLException e) {
          throw new ExDB("UCon.rollback()", e);
      }
      finally {
        Track.pop("Rollback");
      }
    }
  }

  /**
   * Rolls back to a savepoint within the connection's current transaction.
   * @param pSavepoint Name of savepoint to roll back to.
   * @throws ExDB If rollback fails.
   */
  @Deprecated
  public void rollbackTo(String pSavepoint)
  throws ExDB {
    checkOpen();
    executeAPI("ROLLBACK TO "+pSavepoint, "UCon Rollback To Savepoint");
    int i = mSavepointNamesUsedList.lastIndexOf(pSavepoint);
    if(i != -1) {
      while(i+1 < mSavepointNamesUsedList.size()) {
        mSavepointNamesUsedList.remove(i+1);
      }
    }
  }

  public void rollbackTo(Savepoint pSavepoint)
  throws ExDB {
    try {
      mDatabaseConnection.rollback(pSavepoint);

      int i = mSavepointNamesUsedList.lastIndexOf(pSavepoint);
      if(i != -1) {
        while(i+1 < mSavepointNamesUsedList.size()) {
          mSavepointNamesUsedList.remove(i+1);
        }
      }
    }
    catch (SQLException e) {
      throw new ExDB("Failed to rollback to savepoint", e);
    }
  }

  /**
   * Sets a savepoint within the connection's current transaction.
   * @param pSavepoint Name of savepoint to set.
   */
  public Savepoint savepoint(String pSavepoint) {
    checkOpen();
    try {
      //executeAPI("SAVEPOINT "+pSavepoint, "UCon Set Savepoint");
      Savepoint lSavepoint = mDatabaseConnection.setSavepoint(pSavepoint);
      if(mSavepointNamesUsedList.isEmpty() || !mSavepointNamesUsedList.get(mSavepointNamesUsedList.size()-1).equals(pSavepoint)) {
        mSavepointNamesUsedList.add(pSavepoint);
      }

      return lSavepoint;
    }
//    catch(ExDB x) {
//      throw x.toUnexpected();
//    }
    catch (SQLException e) {
      throw new ExInternal("Error setting savepoint", e);
    }
  }

  //TODO convert to Clob
  public final CLOB getTemporaryClob() {
    checkOpen();
    CLOB lCLOB;

    try {
      lCLOB = CLOB.createTemporary(
        mDatabaseConnection.unwrap(OracleConnection.class)
      , true /*isCached*/
      , CLOB.DURATION_SESSION /* Only duration_session in client side java applications */
      );
    }
    catch (SQLException e) {
      throw new ExInternal("Temporary clob cannot be created", e);
    }
    return lCLOB;
  }

  public final BLOB getTemporaryBlob() {
    checkOpen();
    BLOB lBLOB;
    try {
      lBLOB = BLOB.createTemporary(
        mDatabaseConnection.unwrap(OracleConnection.class)
      , true /*isCached*/
      , BLOB.DURATION_SESSION /* Only duration_session in client side java applications */
      );
    }
    catch (SQLException e) {
      throw new ExInternal("Temporary blob cannot be created", e);
    }
    return lBLOB;
  }

  /**
   * Frees a temporary CLOB. If the CLOB is not temporary, this method does nothing.
   * @param pTempClob CLOB to be freed.
   */
  public final void freeTemporaryClob(CLOB pTempClob) {
    checkOpen();
    try {
       if(pTempClob!=null && pTempClob.isTemporary()) {
         pTempClob.freeTemporary();
       }
    }
    catch (SQLException e) {
      throw new ExInternal("Temporary clob cannot be freed", e);
    }
  }

  public final void freeTemporaryBlob(BLOB pTempBlob) {
    checkOpen();
    try {
      if(pTempBlob!=null && pTempBlob.isTemporary()) {
        pTempBlob.freeTemporary();
      }
    }
    catch (SQLException e) {
      throw new ExInternal("Temporary clob cannot be freed", e);
    }
  }

  public void startTracing(String pTraceFileIdentifier, String pTraceEvent, String pLevel) {
    if (pTraceFileIdentifier == null) {
      throw new ExInternal("Missing tracefile identifier name, cannot run !TRACE");
    }
    else if (pTraceFileIdentifier.length() > 45) {
      pTraceFileIdentifier = pTraceFileIdentifier.substring(0, 45);
    }

    try {
      //If not already tracing, set the tracefile
      if (!mIsTracing) {
        executeCall(
          gPlsqlSetTraceFile
        , new Object[] {pTraceFileIdentifier}
        , new char[] {'I'}
        );
      }
      //Add traces for all the events we want
      executeCall(
        gPlsqlStartTracing
      , new Object[] {pTraceEvent, pLevel}
      , new char[] {'I', 'I'}
      );
      mTraceEvent = pTraceEvent;
      mIsTracing = true;
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public void stopTracing() {
    try {
      executeCall(
        gPlsqlStopTracing
      , new Object[] {mTraceEvent}
      , new char[] {'I'}
      );
      mIsTracing = false;
    }
    catch (Throwable e) {
      FoxLogger.getLogger().error("Failed to stop tracking in UCon", e);
    }
  }

  // Returns the name of the database currently being used
  public String getDatabaseName() {
    if(mDatabaseInfo == null) {
      try {
        mDatabaseInfo = queryScalarResult(SELECT_DB_NAME_PARSED_STATEMENT, ScalarResultType.STRING).toLowerCase();
      }
      catch (ExDB e) {
        throw new ExInternal("Could not determine database name", e);
      }
    }
    return mDatabaseInfo;
  }

  /**
   * Gets the ID of the database transaction which is currently active on this UCon, or empty string if the UCon is not
   * in a transaction.
   * @return Transaction ID or empty string.
   */
  public String getTransactionId() {
    try {
      UConStatementResult lAPIResult = executeAPI(GET_TRANSACTION_ID_PARSED_STATEMENT, bindOutString());
      return XFUtil.nvl(lAPIResult.getString(":tran_id"));
    }
    catch (ExDB e) {
      throw new ExInternal("Error getting transaction ID", e);
    }
  }

  /**
   * Tests if a transaction is currently active on this connection.
   * @return True if a transaction is active, false otherwise.
   */
  public boolean isTransactionActive() {
    return !XFUtil.isNull(getTransactionId());
  }

  /**
   * Cancels execution of the current JDBC Statement (if any). It is intended that this
   * is called from another Thread. This does not rollback or release connection, but
   * does implicitly close the Statement object. Note that this method is not
   * object synchronized specifically to allow for other Threads to call in, but
   * internally syncs around a safety object protecting access to the Statement.
   * @throws SQLException if JDBC implementation does not support cancelling
   */
  public final void cancelCurrentStatement()
  throws SQLException {
    Statement lCurrentStatement = null;
    if (lCurrentStatement != null) {
      // Cancel and close so the owning Thread can continue and duly tidy itself up
      lCurrentStatement.cancel();
      lCurrentStatement.close();
    }
  }


  /**
   * Get the Connection Pool which this UCon's connection belongs to.
   * @return Connection Pool
   */
  public final ConnectionPool getConnectionPool() {
    return mConnectionPool;
  }

  /**
   * Gets the underlying database connection object which this UCon is wrapping.
   * @return Database connection
   */
  public final Connection getJDBCConnection() {
    return mDatabaseConnection;
  }

  /**
   * Get the Binary XML Processor for the connection
   *
   * @return Binary XML Processor
   */
  public BinXMLProcessor getBinXMLProcessor() {
    return mBinXMLProcessor;
  }

  /**
   * Get the Pool Name for this connection
   *
   * @return Pool name
   */
  public final String getPoolName() {
    return mConnectionPool.getConfig().getPoolName();
  }

  /**
   * Closes UCon for recycle. Used by try-with-resource construct. Callers should explicitly use one of closeForRecycle or
   * closeNoRecycle.
   */
  @Override
  public void close() {
    closeForRecycle();
  }

  /***************************** New Querying Static Methods ***************************************/

  /**
   * Converts the given object to an appropriate BindObject, based on the rules defined in {@link UCon}. If the object
   * cannot be converted, an exception is thrown.
   * @param pObject Object to bind.
   * @return An appropriate BindObject.
   */
  static BindObject convertToBindObject(Object pObject) {

    if(pObject == null){
      //Default to string for nulls - consumers should pass in a NullBindObject of the correct type if they require an alternative
      return new StringBindObject(null);
    }
    else if(pObject instanceof BindObject) {
      //Shortcut if this is already a BindObject
      return (BindObject) pObject;
    }
    else if(pObject instanceof DOM) {
      return new DOMBindObject((DOM) pObject);
    }
    else if(pObject instanceof String) {
      return new StringBindObject((String) pObject);
    }
    else if(pObject instanceof Date) {
      return new TimestampBindObject((Date) pObject);
    }
    else if(pObject instanceof Number) {
      return new NumericBindObject((Number) pObject);
    }
    else if(pObject instanceof Clob) {
      return new ClobBindObject((Clob) pObject);
    }
    else if(pObject instanceof Blob) {
      return new BlobBindObject((Blob) pObject);
    }
    else {
      throw new ExInternal("UCon doesn't know how to bind a " + pObject.getClass().getName());
    }
  }

  /**
   * Creates an OUT bind for a SQLXML parameter in an executeAPI call.
   * @return OUT bind.
   */
  public static BindObject bindOutXML() {
    return new OutBindObject(BindSQLType.XML);
  }

  /**
   * Creates an OUT bind for a Clob parameter in an executeAPI call.
   * @return OUT bind.
   */
  public static BindObject bindOutClob() {
    return new OutBindObject(BindSQLType.CLOB);
  }

  /**
   * Creates an OUT bind for a Blob parameter in an executeAPI call.
   * @return OUT bind.
   */
  public static BindObject bindOutBlob() {
    return new OutBindObject(BindSQLType.BLOB);
  }

  /**
   * Creates an OUT bind for a String parameter in an executeAPI call.
   * @return OUT bind.
   */
  public static BindObject bindOutString() {
    return new OutBindObject(BindSQLType.STRING);
  }

  /**
   * Creates an OUT bind for a parameter in an executeAPI call. Only use this method if you cannot use bindOutXML,
   * bindOutClob or bindOutString.
   * @param pBindSQLType Datatype being bound out.
   * @return OUT bind.
   */
  public static BindObject bindOut(BindSQLType pBindSQLType) {
    return new OutBindObject(pBindSQLType);
  }

  /**
   * Convenience method for converting a String into a Clob bind object. Use this when you have a String which needs to
   * be bound into a statement as a Clob.
   * @param pString String to bind.
   * @return Clob BindObject.
   */
  public static BindObject bindStringAsClob(String pString) {
    return new ClobStringBindObject(pString);
  }

  /**
   * Binds an object into a UCon statement as an "uncloseable" bind, i.e. for a LOB locator which should be kept
   * open after statement execution.
   * @param pObjectToBind Object to be bound as an uncloseable bind.
   * @return Uncloseable BindObject.
   */
  public static UncloseableBindObject bindUncloseableObject(Object pObjectToBind) {
    BindObject lBindObject = convertToBindObject(pObjectToBind);
    return new UncloseableBindObject(lBindObject);
  }

  /***************************** New Querying Main Methods ***************************************/

  private <T> T queryScalarResult(ExecutableQuery pExecutableQuery, ScalarResultType<T> pScalarResultType)
  throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {

    ScalarResultDeliverer<T> lDeliverer = pScalarResultType.getResultDeliverer();
    pExecutableQuery.executeAndDeliver(this, lDeliverer);

    return lDeliverer.getResult();
  }

  /**
   * Runs a query which returns a single value in one row and one column. See {@link UCon} for more details on how queries
   * are executed.
   * @param <T> Expected return type corresponding to the given ScalarResultType.
   * @param pStatement Query to execute.
   * @param pScalarResultType Type to return. This may perform a conversion on the underlying result object.
   * @param pBinds Objects to be positionally bound into the statement.
   * @return The scalar result object.
   * @throws ExDBTimeout If a lock acquisition times out.
   * @throws ExDBTooFew If 0 rows are selected.
   * @throws ExDBTooMany If more than 1 row is selected.
   * @throws ExDB If any other database failure occurs.
   */
  public <T> T queryScalarResult(ParsedStatement pStatement, ScalarResultType<T> pScalarResultType, Object... pBinds)
  throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    ExecutableQuery lExecutableQuery = pStatement.createExecutableQuery(new ArrayBindProvider(pBinds));
    return queryScalarResult(lExecutableQuery, pScalarResultType);
  }

  /**
   * Runs a query which returns a single value in one row and one column. See {@link UCon} for more details on how queries
   * are executed.
   * @param <T> Expected return type corresponding to the given ScalarResultType.
   * @param pStatement Query to execute.
   * @param pScalarResultType Type to return. This may perform a conversion on the underlying result object.
   * @param pBinds Objects to be bound into the statement by name.
   * @return The scalar result object.
   * @throws ExDBTimeout If a lock acquisition times out.
   * @throws ExDBTooFew If 0 rows are selected.
   * @throws ExDBTooMany If more than 1 row is selected.
   * @throws ExDB If any other database failure occurs.
   */
  public <T> T queryScalarResult(ParsedStatement pStatement, ScalarResultType<T> pScalarResultType, UConBindMap pBinds)
  throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    ExecutableQuery lExecutableQuery = pStatement.createExecutableQuery(pBinds);
    return queryScalarResult(lExecutableQuery, pScalarResultType);
  }

  private UConStatementResult querySingleRow(ExecutableQuery pExecutableQuery)
  throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    SingleRowResultDeliverer lDeliverer = new SingleRowResultDeliverer();
    pExecutableQuery.executeAndDeliver(this, lDeliverer);
    return lDeliverer.getResultRow();
  }

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
  public UConStatementResult querySingleRow(ParsedStatement pStatement, Object... pBinds)
  throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    ExecutableQuery lExecutableQuery = pStatement.createExecutableQuery(new ArrayBindProvider(pBinds));
    return querySingleRow(lExecutableQuery);
  }

  /**
   * Runs a query which is expected to return a single row. See {@link UCon} for more details on how queries
   * are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be bound into the statement by name.
   * @return A UConStatementResult representing the selected row.
   * @throws ExDBTimeout If a lock acquisition times out.
   * @throws ExDBTooFew If 0 rows are selected.
   * @throws ExDBTooMany If more than 1 row is selected.
   * @throws ExDB If any other database failure occurs.
   */
  public UConStatementResult querySingleRow(ParsedStatement pStatement, UConBindMap pBinds)
  throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    ExecutableQuery lExecutableQuery = pStatement.createExecutableQuery(pBinds);
    return querySingleRow(lExecutableQuery);
  }

  private List<UConStatementResult> queryMultipleRows(ExecutableQuery pExecutableQuery)
  throws ExDB {
    MultiRowResultDeliverer lDeliverer = new MultiRowResultDeliverer();
    pExecutableQuery.executeAndDeliver(this, lDeliverer);
    return lDeliverer.getResultList();
  }

  /**
   * Runs a query which is expected to return a muliple rows. All rows are selected into an in-memory list so this method
   * should only be used to select small result sets. This method may select no rows, in which case an empty list is returned.
   * See {@link UCon} for more details on how queries are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be positionally bound into the statement.
   * @return A list of UConStatementResults representing the selected rows.
   * @throws ExDB If any database failure occurs.
   */
  public List<UConStatementResult> queryMultipleRows(ParsedStatement pStatement, Object... pBinds)
  throws ExDB {
    ExecutableQuery lExecutableQuery = pStatement.createExecutableQuery(new ArrayBindProvider(pBinds));
    return queryMultipleRows(lExecutableQuery);
  }

  /**
   * Runs a query which is expected to return a muliple rows. All rows are selected into an in-memory list so this method
   * should only be used to select small result sets. This method may select no rows, in which case an empty list is returned.
   * See {@link UCon} for more details on how queries are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be bound into the statement by name.
   * @return A list of UConStatementResults representing the selected rows.
   * @throws ExDB If any database failure occurs.
   */
  public List<UConStatementResult> queryMultipleRows(ParsedStatement pStatement, UConBindMap pBinds)
  throws ExDB {
    ExecutableQuery lExecutableQuery = pStatement.createExecutableQuery(pBinds);
    return queryMultipleRows(lExecutableQuery);
  }

  private UConResultSet queryResultSet(ExecutableQuery pExecutableQuery)
  throws ExDB {
    pExecutableQuery.executeAndDeliver(this, ResultSetDeliverer.INSTANCE);
    return new UConResultSet(pExecutableQuery);
  }

  /**
   * Runs a query and gets its ResultSet. Consumers <b>MUST</b> ensure they call {@link UConResultSet#close} to close
   * the Statement and its ResultSet after they are finished with it, ideally in a <tt>finally</tt> block. Use this method
   * when you are dealing with a large dataset or need to implement custom datatype retrieval.
   * See {@link UCon} for more details on how queries are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be positionally bound into the statement.
   * @return A UConResultSet from which you can retrieve a JDBC ResultSet. This must be closed following any processing.
   * @throws ExDB If any database failure occurs.
   */
  public UConResultSet queryResultSet(ParsedStatement pStatement, Object... pBinds)
  throws ExDB {
    ExecutableQuery lExecutableQuery = pStatement.createExecutableQuery(new ArrayBindProvider(pBinds));
    return queryResultSet(lExecutableQuery);
  }

  /**
   * Runs a query and gets its ResultSet. Consumers <b>MUST</b> ensure they call {@link UConResultSet#close} to close
   * the Statement and its ResultSet after they are finished with it, ideally in a <tt>finally</tt> block. Use this method
   * when you are dealing with a large dataset or need to implement custom datatype retrieval.
   * See {@link UCon} for more details on how queries are executed.
   * @param pStatement Query to execute.
   * @param pBinds Objects to be bound into the statement by name.
   * @return A UConResultSet from which you can retrieve a JDBC ResultSet. This must be closed following any processing.
   * @throws ExDB If any database failure occurs.
   */
  public UConResultSet queryResultSet(ParsedStatement pStatement, UConBindMap pBinds)
  throws ExDB {
    ExecutableQuery lExecutableQuery = pStatement.createExecutableQuery(pBinds);
    return queryResultSet(lExecutableQuery);
  }

  private UConStatementResult executeAPI(ExecutableAPI pExecutableAPI, BindObjectProvider pBindProvider)
  throws ExDB {
    APIDeliverer lDeliverer = new APIDeliverer(pBindProvider);
    pExecutableAPI.executeAndDeliver(this, lDeliverer);
    return lDeliverer.getStatementResult();
  }

  /**
   * Executes an API, which may be a DML statement or PL/SQL block. You may bind OUT bind objects (i.e. {@link UCon#bindOutString}) -
   * the resultant OUT objects are added to the UConStatementResult which this method returns. See {@link UCon} for more
   * details on how APIs are executed.
   * @param pStatement Statement to execute.
   * @param pBinds Objects to be positionally bound into the statement.
   * @return UConStatementResult containing any OUT parameters.
   * @throws ExDB If any database failure occurs.
   */
  public UConStatementResult executeAPI(ParsedStatement pStatement, Object... pBinds)
  throws ExDB {
    BindObjectProvider lBindProvider = new ArrayBindProvider(pBinds);
    ExecutableAPI lExecutableAPI = pStatement.createExecutableAPI(new ArrayBindProvider(pBinds));
    return executeAPI(lExecutableAPI, lBindProvider);
  }

  /**
   * Executes an API, which may be a DML statement or PL/SQL block. You may bind OUT bind objects (i.e. {@link UCon#bindOutString}) -
   * the resultant OUT objects are added to the UConStatementResult which this method returns. See {@link UCon} for more
   * details on how APIs are executed.
   * @param pStatement Statement to execute.
   * @param pBinds Objects to be bound into the statement by name.
   * @return UConStatementResult containing any OUT parameters.
   * @throws ExDB If any database failure occurs.
   */
  public UConStatementResult executeAPI(ParsedStatement pStatement, UConBindMap pBinds)
  throws ExDB {
    ExecutableAPI lExecutableAPI = pStatement.createExecutableAPI(pBinds);
    return executeAPI(lExecutableAPI, pBinds);
  }

  /**
   * Executes an API which does not contain any bind variables.
   * @param pStatement Statement string to execute as an API.
   * @param pPurpose Description of the API for logging and debug.
   * @return The number of records affected if this API was a DML statement, or 0.
   * @throws ExDB If statement execution fails.
   */
  public int executeAPI(String pStatement, String pPurpose)
  throws ExDB {

    Track.pushInfo("ExecuteAPI", "Executing unparsed statement for " + pPurpose);
    try {
      CallableStatement lCall = getJDBCConnection().prepareCall(pStatement);
      int lResult = lCall.executeUpdate();
      lCall.close();
      return lResult;
    }
    catch (SQLException e) {
      throw convertSQLException(e, "Failed to run unparsed API " + pStatement);
    }
    finally {
      Track.pop("ExecuteAPI");
    }
  }

  /***************************** Fox Plugin API satisfying implementations ***************************************/

  /**
   * @see #queryScalarResult(ParsedStatement, ScalarResultType, Object...)
   */
  @Override
  public DOM queryScalarDOM(ParsedStatement pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    return queryScalarResult(pStatement, ScalarResultType.DOM, pBinds);
  }

  /**
   * @see #queryScalarResult(ParsedStatement, ScalarResultType, UConBindMap)
   */
  @Override
  public DOM queryScalarDOM(ParsedStatement pStatement, UConBindMap pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    return queryScalarResult(pStatement, ScalarResultType.DOM, pBinds);
  }

  /**
   * @see #queryScalarResult(ParsedStatement, ScalarResultType, Object...)
   */
  @Override
  public Clob queryScalarClob(ParsedStatement pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    return queryScalarResult(pStatement, ScalarResultType.CLOB, pBinds);
  }

  /**
   * @see #queryScalarResult(ParsedStatement, ScalarResultType, UConBindMap)
   */
  @Override
  public Clob queryScalarClob(ParsedStatement pStatement, UConBindMap pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    return queryScalarResult(pStatement, ScalarResultType.CLOB, pBinds);
  }

  /**
   * @see #queryScalarResult(ParsedStatement, ScalarResultType, Object...)
   */
  @Override
  public Blob queryScalarBlob(ParsedStatement pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    return queryScalarResult(pStatement, ScalarResultType.BLOB, pBinds);
  }

  /**
   * @see #queryScalarResult(ParsedStatement, ScalarResultType, UConBindMap)
   */
  @Override
  public Blob queryScalarBlob(ParsedStatement pStatement, UConBindMap pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    return queryScalarResult(pStatement, ScalarResultType.BLOB, pBinds);
  }

  /**
   * @see #queryScalarResult(ParsedStatement, ScalarResultType, Object...)
   */
  @Override
  public String queryScalarString(ParsedStatement pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    return queryScalarResult(pStatement, ScalarResultType.STRING, pBinds);
  }

  /**
   * @see #queryScalarResult(ParsedStatement, ScalarResultType, UConBindMap)
   */
  @Override
  public String queryScalarString(ParsedStatement pStatement, UConBindMap pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    return queryScalarResult(pStatement, ScalarResultType.STRING, pBinds);
  }

  /**
   * @see #queryScalarResult(ParsedStatement, ScalarResultType, Object...)
   */
  @Override
  public Object queryScalarObject(ParsedStatement pStatement, Object... pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    return queryScalarResult(pStatement, ScalarResultType.SQL_OBJECT, pBinds);
  }

  /**
   * @see #queryScalarResult(ParsedStatement, ScalarResultType, UConBindMap)
   */
  @Override
  public Object queryScalarObject(ParsedStatement pStatement, UConBindMap pBinds) throws ExDBTimeout, ExDBTooFew, ExDBTooMany, ExDB {
    return queryScalarResult(pStatement, ScalarResultType.SQL_OBJECT, pBinds);
  }

  /***************************** New Querying Classes ***************************************/

  /** Provides positional binds to a statement from an array (from a varargs argument). */
  private static class ArrayBindProvider
  implements BindObjectProvider {

    private final Object[] mObjectsToBind;

    public ArrayBindProvider(Object[] pObjectsToBind) {
      mObjectsToBind = pObjectsToBind;
    }

    @Override
    public boolean isNamedProvider() {
      return false;
    }

    @Override
    public BindObject getBindObject(String pBindName, int pIndex) {
      return convertToBindObject(mObjectsToBind[pIndex]);
    }
  }

  /** Dummy deliverer which prevents the statement's ResultSet being closed after execution. */
  private static class ResultSetDeliverer
  implements QueryResultDeliverer {

    private static final ResultSetDeliverer INSTANCE = new ResultSetDeliverer();

    @Override
    public void deliver(ExecutableQuery pQuery) {}

    @Override
    public boolean closeStatementAfterDelivery() {
      return false;
    }
  }

  /** Deliverer for querying a single UConResultObject from a 1-row query. */
  private static class SingleRowResultDeliverer
  implements QueryResultDeliverer {

    private UConStatementResult mResultRow = null;

    @Override
    public void deliver(ExecutableQuery pQuery)
    throws ExDB {

      ResultSet lResultSet = pQuery.getResultSet();
      try {
        boolean lNextResult = lResultSet.next();
        if(!lNextResult) {
          throw new ExDBTooFew("Expected one row got 0 for query " + pQuery.getParsedStatement().getStatementPurpose());
        }
        mResultRow = UConStatementResult.fromResultSet(lResultSet);

        if(lResultSet.next()) {
          throw new ExDBTooMany("Single row result set contained more than one row for query " + pQuery.getParsedStatement().getStatementPurpose());
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

    public UConStatementResult getResultRow() {
      return mResultRow;
    }
  }

  /** Deliverer for querying a list of UConResultObjects from a multi-row query. */
  private static class MultiRowResultDeliverer
  implements QueryResultDeliverer {

    private final List<UConStatementResult> mResultList = new ArrayList<>();

    @Override
    public void deliver(ExecutableQuery pQuery)
    throws ExDB {

      ResultSet lResultSet = pQuery.getResultSet();
      try {
        while(lResultSet.next()) {
          mResultList.add(UConStatementResult.fromResultSet(lResultSet));
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

    public List<UConStatementResult> getResultList() {
      return mResultList;
    }
  }

  /** Deliverer for retrieving OUT binds from an executed API. */
  private static class APIDeliverer
  implements APIResultDeliverer {

    private final BindObjectProvider mBindProvider;
    private UConStatementResult mStatementResult = null;

    public APIDeliverer(BindObjectProvider pBindProvider) {
      mBindProvider = pBindProvider;
    }

    @Override
    public void deliver(ExecutableAPI pAPI) {

      Map<String, Object> lOutBinds = new HashMap<>();
      JDBCResultAdaptor lAdaptor = new CallableStatementAdaptor(pAPI.getCallableStatement());

      //Loop every bind in the API positionally, check if it was an out parameter and if so retrieve its value and add
      //it to the map.
      int i = 0;
      for(String lBindName : pAPI.getParsedStatement().getBindNameList()) {
        BindObject lBindObject = mBindProvider.getBindObject(lBindName, i);
        if(lBindObject.getDirection().isOutBind()) {

          if(lOutBinds.containsKey(lBindName)) {
            throw new ExInternal("Error binding values out of API: OUT bind name " + lBindName + "used more than once");
          }

          try {
            //Use the SQLTypeConverter to select the object from the CallableStatement so the objects in the map will
            //be of predictable types.
            lOutBinds.put(lBindName, SQLTypeConverter.getValueAsObjectForBindSQLType(lAdaptor, i+1, pAPI.getOutBindSQLType(i+1)));
          }
          catch (SQLException e) {
            throw new ExInternal("Failed to convert out bind at index " + i, e);
          }
        }

        i++;
      }

      mStatementResult = UConStatementResult.fromMap(lOutBinds);
    }

    @Override
    public boolean closeStatementAfterDelivery() {
      return true;
    }

    public UConStatementResult getStatementResult() {
      return mStatementResult;
    }
  }

  /** Out binds for executing APIs. */
  private static class OutBindObject
  implements BindObject {

    private final BindSQLType mBindSQLType;

    public OutBindObject(BindSQLType pBindSQLType) {
      mBindSQLType = pBindSQLType;
    }

    @Override
    public Object getObject(UCon pUCon) {
      return null;
    }

    @Override
    public String getObjectDebugString() {
      return null;
    }

    @Override
    public BindSQLType getSQLType() {
      return mBindSQLType;
    }

    @Override
    public BindDirection getDirection() {
      return BindDirection.OUT;
    }
  }
}
