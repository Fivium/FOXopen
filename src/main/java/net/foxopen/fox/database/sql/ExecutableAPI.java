package net.foxopen.fox.database.sql;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.track.Track;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Wraps a CallableStatement for binding and executing as a SQL API.
 */
public class ExecutableAPI
extends ExecutableStatement {

  private CallableStatement mCallableStatement;
  private int mRowsAffected;
  /** Maps 1-based parameter index to BindSQLTypes for out binds */
  private Map<Integer, BindSQLType> mOutBindIndexToBindTypeMap = null;

  public ExecutableAPI(ParsedStatement pParsedStatement, BindObjectProvider pBindProvider) {
    super(pParsedStatement, pBindProvider);
  }

  @Override
  protected void executeInternal(UCon pUCon)
  throws SQLException {

    Track.pushInfo("ExecuteAPI", getParsedStatement().getStatementPurpose());
    try {
      prepare(pUCon);
      bind(pUCon);
      execute();
    }
    finally {
      Track.pop("ExecuteAPI");
    }
  }

  protected void prepare(UCon pUCon)
  throws SQLException {

    Track.pushDebug("Prepare");
    try {
      String lStatementString = getParsedStatement().getParsedStatementString();
      mCallableStatement = pUCon.getJDBCConnection().prepareCall(lStatementString, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
    }
    finally {
      Track.pop("Prepare");
    }
  }

  protected void bind(UCon pUCon)
  throws SQLException {

    Track.pushDebug("Bind");
    try {

      Map<Integer, BindSQLType> lOutBindIndexToBindTypeMap = new HashMap<>();

      for(int i=0; i < getBindObjectList().size(); i++) {
        BindObject lBindObject = getBindObjectList().get(i);
        // IN / IN_OUT binds
        if(lBindObject.getDirection().isInBind()) {
          applyBind(lBindObject, i, mCallableStatement, pUCon);
        }

        // OUT / IN_OUT binds
        if(lBindObject.getDirection().isOutBind()) {
          BindSQLType lBindType = lBindObject.getSQLType();
          mCallableStatement.registerOutParameter(i + 1, lBindType.getSqlTypeCode());
          lOutBindIndexToBindTypeMap.put(i + 1, lBindType);
        }
      }

      mOutBindIndexToBindTypeMap = Collections.unmodifiableMap(lOutBindIndexToBindTypeMap);
    }
    finally {
      Track.pop("Bind");
    }
  }

  protected void execute()
  throws SQLException {

    Track.pushDebug("Execute");
    try {
      // Open cursor result set
      mRowsAffected = mCallableStatement.executeUpdate();
    }
    finally {
      Track.pop("Execute");
    }
  }

  @Override
  protected void closeInternal()
  throws SQLException {
    if(mCallableStatement != null) {
      mCallableStatement.close();
    }
  }

  public CallableStatement getCallableStatement() {

    if(mCallableStatement == null) {
      throw new IllegalStateException("Cannot call getCallableStatement before calling prepare");
    }

    return mCallableStatement;
  }

  /**
   * Gets the BindSQLType which was used when setting up the parameter at the given bind index. This method should only
   * be called after the API is executed.
   * @param pOutBindIndex 1-based index of the out parameter.
   * @return BindSQLType for the OUT bind at the given index, or null if no OUT bind exists at that index.
   */
  public BindSQLType getOutBindSQLType(int pOutBindIndex) {

    if(mOutBindIndexToBindTypeMap == null) {
      throw new IllegalStateException("Cannot call getOutBindSQLType before calling bind");
    }

    return mOutBindIndexToBindTypeMap.get(pOutBindIndex);
  }
}
