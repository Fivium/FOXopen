package net.foxopen.fox.database.sql;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.database.sql.bind.CloseableBindObject;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBFlashback;
import net.foxopen.fox.ex.ExDBSyntax;
import net.foxopen.fox.ex.ExDBTimeout;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackTimer;


/**
 * A SQL statement (query or API), constructed from a ParsedStatement, which has bind objects applied for the context
 * of an individual execution. Queries should be used for SELECT statements and APIs for DML statement and PL/SQL calls.
 * Once executed, the statement is closed and cannot be re-executed. The results of executing the statement can be directed
 * to an appropriate destination using a ResultDeliverer before the statement is closed.<br/><br/>
 *
 * This class encapsulates the behaviour shared by query and API execution - subclasses hold references to the JDBC statements
 * and implement different binding methods, which differ between queries and APIs.
 */
public abstract class ExecutableStatement {

  /** The ParsedStatement which was used to create this ExecutableStatement. */
  private final ParsedStatement mParsedStatement;

  /** List of objects to be bound into bind positions in the statement, in the order the binds appear in the statement.  */
  private final List<BindObject> mBindObjectList;

  /**
   * Applies a bind to a JDBC PreparedStatement at the specified 0-based index.
   * @param pBindObject Object to be bound.
   * @param pIndex 0-based index of bind within PreparedStatement.
   * @param pStatement Statement to be bound into.
   * @param pUCon Current connection.
   * @throws SQLException
   */
  protected static void applyBind(BindObject pBindObject, int pIndex, PreparedStatement pStatement, UCon pUCon)
  throws SQLException {

    int lIndex = pIndex + 1;
    Object lObject =  pBindObject.getObject(pUCon);

    switch(pBindObject.getSQLType()) {
      case BLOB:
        pStatement.setBlob(lIndex, (Blob) lObject);
        break;
      case CLOB:
        pStatement.setClob(lIndex, (Clob) lObject);
        break;
      case NUMBER:
        if (lObject instanceof Integer) {
          pStatement.setInt(lIndex, (Integer) lObject);
          break;
        }
        else if (lObject instanceof Double){
          pStatement.setDouble(lIndex, (Double) lObject);
          break;
        }
      else if (lObject instanceof Long){
        pStatement.setLong(lIndex, (Long) lObject);
        break;
      }
        else {
          throw new ExInternal("Cannot convert a " + lObject.getClass().getName() + " to a NUMBER (must be int, double or long)");
        }
      case TIMESTAMP:
        pStatement.setTimestamp(lIndex, (Timestamp) lObject);
        break;
      case STRING:
        pStatement.setString(lIndex, (String) lObject);
        break;
      case XML:
        pStatement.setSQLXML(lIndex, (SQLXML) lObject);
        break;
      default:
        throw new ExInternal("Don't know how to bind a " + pBindObject.getSQLType());
    }

  }

  protected ExecutableStatement(ParsedStatement pParsedStatement) {
    mParsedStatement = pParsedStatement;
    mBindObjectList = new ArrayList<>(mParsedStatement.getBindNameList().size());
  }

  protected final void evaluateBinds(BindObjectProvider pBindProvider) {

    //Loop through the binds in this statement and resolve them from the given BindProvider. If the BindProvider gives
    //us named binds, we can record resolved binds against their names to avoid evaluating the same bind multiple times.
    Map<String, BindObject> lNamedBindToObject = new HashMap<>();

    for(int i=0; i < mParsedStatement.getBindNameList().size(); i++) {
      String lBindName = mParsedStatement.getBindNameList().get(i);

      BindObject lBindObject = null;

      //Check in the map to see if this bind was already evaluated
      if(pBindProvider.isNamedProvider()) {
        lBindObject = lNamedBindToObject.get(lBindName);
      }

      //Not in map or we're not using the map
      if(lBindObject == null) {
        try {
          lBindObject = pBindProvider.getBindObject(lBindName, i);
        }
        catch(Throwable th) {
          throw new ExInternal("Failed to retrieve value for bind variable " + lBindName + " (bind index " + i + ") in SQL statement " + mParsedStatement.getStatementPurpose(), th);
        }
      }
      else {
        //The bind object was already in the map - it has already been resolved. Check this is not an out parameter
        //as it doesn't make sense to bind an out parameter more than once.
        if(lBindObject.getDirection().isOutBind()) {
          throw new ExInternal("Bind " + lBindName + " is marked as an output parameter but is used multiple times in SQL statement " + mParsedStatement.getStatementPurpose());
        }
      }

      mBindObjectList.add(lBindObject);

      //Put in the map to avoid duplicate resolution
      if(pBindProvider.isNamedProvider()) {
        lNamedBindToObject.put(lBindName, lBindObject);
      }
    }
  }

  protected final void closeBinds()
  throws SQLException {

    //Close closeable binds - get a unique set so we don't close the same object more than once
    Set<BindObject> lDistinctBindObjects = new HashSet<>(mBindObjectList);
    for(BindObject lBind : lDistinctBindObjects) {
      if(lBind instanceof CloseableBindObject) {
        ((CloseableBindObject) lBind).close();
      }
    }
  }

  /**
   *
   * @param pUCon
   * @param pDeliverer Optional
   * @param pClose
   * @throws ExDB
   */
  private final void executeInternal(UCon pUCon, ResultDeliverer pDeliverer, boolean pClose)
  throws ExDB {

    boolean lExecuteFailed = false;
    try {

      pUCon.setClientInfo(getParsedStatement().getStatementPurpose());

      if(pDeliverer != null && pDeliverer instanceof PreExecutionDeliveryHandler) {
        //If the deliverer wants to know that execution is about to start, tell it
        Track.pushInfo("PreExecutionHandler");
        try {
          ((PreExecutionDeliveryHandler) pDeliverer).processBeforeExecution(pUCon, this);
        }
        finally {
          Track.pop("PreExecutionHandler");
        }
      }

      Track.timerStart(TrackTimer.DATABASE_EXECUTION);
      try {
        //Execute the query/API
        executeInternal(pUCon);
      }
      finally {
        Track.timerPause(TrackTimer.DATABASE_EXECUTION);
      }
      //Deliver - note this MUST happen before close, in case the deliverer is relying on any temporary resources
      //associated with the binds or the result set.
      if(pDeliverer != null) {
        Track.pushInfo("DeliverStatementResult", pDeliverer.getClass().getSimpleName());
        try {
          pDeliverer.deliver(this);
        }
        finally {
          Track.pop("DeliverStatementResult");
        }
      }
    }
    catch (SQLException e) {
      //Convert SQL exception to ExDB
      lExecuteFailed = true;
      convertErrorAndThrow(e);
    }
    catch (ExDB e) {
      //Rethrow ExDB exception from deliverer
      lExecuteFailed = true;
      throw e;
    }
    catch (Throwable th) {
      lExecuteFailed = true;
      throw new ExInternal(generateErrorMessage(), th);
    }
    finally {
      //Close temporary resources.
      if(pClose) {
        try {
          close();
        }
        catch (Throwable th) {
          //Don't suppress the original error if an additional error is caused by close
          if(!lExecuteFailed) {
            if(th instanceof SQLException) {
              convertErrorAndThrow((SQLException) th);
            }
            else {
              throw new ExInternal("Unexpected error caught when closing statement", th);
            }
          }
          else {
            Track.recordSuppressedException("ExecutableStatement close", th);
          }
        }
      }

      //Don't allow setClientInfo problems to overwrite the main error
      try {
        pUCon.setClientInfo("");
      }
      catch (Throwable th) {
        Track.recordSuppressedException("ExecutableStatement setClientInfo", th);
      }
    }
  }

  /**
   * Executes the statement and delivers the result using the provided deliverer. The statement is then closed immediately
   * if the deliverer specifies it should be, in order to release any temporary resources associated with the statement.
   * Consumers should ensure the statement is closed manually later if the deliverer does not permit an immediate close.
   * Use this method for running queries or APIs which return a result.
   * @param pUCon
   * @param pDeliverer
   * @throws ExDB If query execution or delivery fails.
   */
  public final void executeAndDeliver(UCon pUCon, ResultDeliverer pDeliverer)
  throws ExDB {
    executeInternal(pUCon, pDeliverer, pDeliverer.closeStatementAfterDelivery());
  }

  /**
   * Executes the statement and immediately closes it, freeing up any associated resources. Use this method for running DML
   * statements or APIs which do not return results into a deliverer.
   * @param pUCon
   * @throws ExDB If query execution or delivery fails.
   */
  public final void executeAndClose(UCon pUCon)
  throws ExDB  {
    executeInternal(pUCon, null, true);
  }

  private final String generateErrorMessage() {
    StringBuilder lErrorMessage = new StringBuilder("Error executing statement " + mParsedStatement.getStatementPurpose() + ":\n\n");

    lErrorMessage.append(mParsedStatement.getOriginalStatement());

    if(mBindObjectList.size() > 0) {
      lErrorMessage.append("\n\nStatement parameter listing:\n\n");

      int i = -1;
      for(BindObject lBindObject : mBindObjectList) {
        lErrorMessage.append("[" + ++i + "]\n");
        lErrorMessage.append("Input value: ");
        lErrorMessage.append(XFUtil.nvl(lBindObject.getObjectDebugString(), "null"));
        lErrorMessage.append("\n");

        lErrorMessage.append("Direction: ");
        lErrorMessage.append(lBindObject.getDirection().toString().toLowerCase());
        lErrorMessage.append("\n");

        lErrorMessage.append("SQL Datatype: ");
        lErrorMessage.append(lBindObject.getSQLType().toString().toLowerCase());
        lErrorMessage.append("\n");

        lErrorMessage.append("Bind name: ");
        lErrorMessage.append(mParsedStatement.getBindNameList().get(i));
        lErrorMessage.append("\n");

        lErrorMessage.append("\n\n");
      }
    }

    return lErrorMessage.toString();
  }

  public final ExDB convertError(SQLException pSQLException) {
    return UCon.convertSQLException(pSQLException, generateErrorMessage());
  }

  /**
   * Converts a JDBC SQLException to a FOX exception, based on the SQL error code, and re-throws the exception.
   * Also appends debug information about the statement and parameters into the error message in order to help developers.
   * @param pSQLException Original exception to convert.
   * @throws ExDBTimeout In the event of "ORA-00054 resource busy and acquire with NOWAIT specified"
   * or "ORA-00051 timeout occurred while waiting for a resource".
   * @throws ExDBFlashback In the event of a flashback related error.
   * @throws ExDBSyntax In the event of any other error.
   */
  public final void convertErrorAndThrow(SQLException pSQLException)
  throws ExDBTimeout, ExDBFlashback, ExDBSyntax {
    ExDB lError = convertError(pSQLException);
    //Convert to specific types so we can declare that this method throws either type
    if(lError instanceof ExDBTimeout) {
      throw (ExDBTimeout) lError;
    }
    else if(lError instanceof ExDBFlashback) {
      throw (ExDBFlashback) lError;
    }
    else {
      throw (ExDBSyntax) lError;
    }
  }

  /**
   * Gets the ParsedStatement which was used to create this ExecutableStatement.
   * @return
   */
  public final ParsedStatement getParsedStatement() {
    return mParsedStatement;
  }

  protected final List<BindObject> getBindObjectList() {
    return mBindObjectList;
  }

  /**
   * Closes this statement, including its ResultSet (if it has one) and its binds. Consumers only need to call this
   * method if they have used a ResultDeliverer which does not close the statement itself.
   * @throws SQLException
   */
  public final void close()
  throws SQLException {
    closeBinds();
    closeInternal();
  }

  protected abstract void executeInternal(UCon pUCon)
  throws SQLException;

  protected abstract void closeInternal()
  throws SQLException;

}
