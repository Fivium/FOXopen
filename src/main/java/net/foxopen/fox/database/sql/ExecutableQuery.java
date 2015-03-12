package net.foxopen.fox.database.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.track.Track;


/**
 * Wraps a PreparedStatement for binding and executing as a SQL query. This should only be used for SELECT statements.
 * For DML statements, use an ExecutableAPI.
 */
public class ExecutableQuery
extends ExecutableStatement {
  /**
   *  Database Prefetch query row size
   */
  // TODO - NP/PN - This should be configurable by developers
  final static int ROW_PREFETCH_SIZE = 100;

  private PreparedStatement mPreparedStatement = null;
  private ResultSet mResultSet = null;
  private ResultSetMetaData mResultSetMetaData = null;

  public ExecutableQuery(ParsedStatement pParsedStatement, BindObjectProvider pBindProvider) {
    super(pParsedStatement);
    evaluateBinds(pBindProvider);
  }

  @Override
  protected void executeInternal(UCon pUCon)
  throws SQLException {

    Track.pushInfo("ExecuteQuery", getParsedStatement().getStatementPurpose());
    try {
      prepare(pUCon);
      bind(pUCon);
      execute();
    }
    finally {
      Track.pop("ExecuteQuery");
    }

  }

  protected void prepare(UCon pUCon)
  throws SQLException {

    Track.pushDebug("Prepare");
    try {
      //TODO PN check that other flags on the prepared statement don't need to be set
      mPreparedStatement = pUCon.getJDBCConnection().prepareStatement(getParsedStatement().getParsedStatementString());
    }
    finally {
      Track.pop("Prepare");
    }
  }

  protected void bind(UCon pUCon)
  throws SQLException {

    Track.pushDebug("Bind");
    try {
      for(int i=0; i < getBindObjectList().size(); i++) {
        BindObject lBindObject = getBindObjectList().get(i);
        applyBind(lBindObject, i, mPreparedStatement, pUCon);
      }
    }
    finally {
      Track.pop("Bind");
    }
  }

  protected void execute()
  throws SQLException {

    // Tell oracle to prefetch rows
    mPreparedStatement.setFetchSize(ROW_PREFETCH_SIZE);

    Track.pushDebug("Execute");
    try {
      // Open cursor result set
      mResultSet = mPreparedStatement.executeQuery();
      mResultSetMetaData = mResultSet.getMetaData();
    }
    finally {
      Track.pop("Execute");
    }
  }

  public ResultSet getResultSet() {
    if(mResultSet == null){
      throw new IllegalStateException("Query must be executed before calling getResultSet");
    }
    return mResultSet;
  }

  public ResultSetMetaData getMetaData() {
    if(mResultSetMetaData == null){
      throw new IllegalStateException("Query must be executed before calling getMetaData");
    }
    return mResultSetMetaData;
  }

  @Override
  protected void closeInternal()
  throws SQLException {
    mPreparedStatement.close();
    getResultSet().close();
  }
}
