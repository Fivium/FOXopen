package net.foxopen.fox.database.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;

/**
 * ExecutableAPI which provides support for the JDBC batch updating mechanism. An ExecutableBatchAPI first needs to be
 * "prepared" with an insert/update statement. The consumer should then call {@link #addBatch} repeatedly to queue up
 * update batches. These will be periodically flushed to the database whenever the specified batch size is reached.
 * When batching is complete, the consumer should call {@link #finaliseAndClose} to flush any remainining batches and
 * close the statement.
 */
public class ExecutableBatchAPI
extends ExecutableStatement {

  public static final int DEFAULT_BATCH_SIZE = 50;

  private final int mBatchSize;

  private PreparedStatement mPreparedStatement;
  private int mExecuteCount = 0;

  /**
   * Creates a new ExecutableBatchAPI for the given parsed statement and prepares it for execution.
   * @param pParsedStatement Statement to be executed as a batch update.
   * @param pBatchSize Size the batch buffer can reach before being sent to the database.
   * @param pUCon UCon to be used to execute the statement.
   * @return A new ExecutableBatchAPI ready for adding batches to.
   * @throws SQLException
   */
  public static ExecutableBatchAPI createAndPrepare(ParsedStatement pParsedStatement, int pBatchSize, UCon pUCon)
  throws SQLException {
    ExecutableBatchAPI lAPI = new ExecutableBatchAPI(pParsedStatement, pBatchSize);
    lAPI.prepare(pUCon);
    return lAPI;
  }

  private ExecutableBatchAPI(ParsedStatement pParsedStatement, int pBatchSize) {
    super(pParsedStatement);
    mBatchSize = pBatchSize;
  }

  public void prepare(UCon pUCon)
  throws SQLException {

    if(mPreparedStatement != null) {
      throw new ExInternal("prepare should only be called once");
    }

    Track.pushDebug("Prepare");
    try {
      String lStatementString = getParsedStatement().getParsedStatementString();
      mPreparedStatement = pUCon.getJDBCConnection().prepareStatement(lStatementString);
    }
    finally {
      Track.pop("Prepare");
    }
  }

  /**
   * Binds all bind variables from the given BindObjectProvider into the statement and adds them as a batch. This method
   * may also send the batch if the batch size has been reached.
   * @param pUCon UCon being used to execute the statement.
   * @param pBindObjectProvider Bind object provider (this should usually be a UConBindMap).
   * @throws SQLException
   */
  public void addBatch(UCon pUCon, BindObjectProvider pBindObjectProvider)
  throws SQLException {

//    Track2.pushDebug("AddBatch");
    try {
      //Reset the bind list ready for the next batch
      getBindObjectList().clear();
      evaluateBinds(pBindObjectProvider);

      //Apply each bind to the statement
      for(int i=0; i < getBindObjectList().size(); i++) {
        BindObject lBindObject = getBindObjectList().get(i);
        if(lBindObject.getDirection().isInBind()) {
          applyBind(lBindObject, i, mPreparedStatement, pUCon);
        }
      }

      //Add batch and execute if required
      mPreparedStatement.addBatch();
    }
    finally {
//      Track2.pop("AddBatch");
    }

    if (++mExecuteCount % mBatchSize == 0) {
      Track.pushDebug("RunBatch", "Batch size " + mBatchSize + " execute count " + mExecuteCount);
      try {
        mPreparedStatement.executeBatch();
      }
      finally {
        Track.pop("RunBatch");
      }
    }
  }

  /**
   * Executes any remaining batches and closes the statement.
   * @throws SQLException
   */
  public void finaliseAndClose() throws SQLException {
    if(mPreparedStatement != null) {
      mPreparedStatement.executeBatch();
      mPreparedStatement.close();
    }
  }

  @Override
  protected void executeInternal(UCon pUCon) throws SQLException {
    throw new ExInternal("Automatic execution is not supported by this class");
  }

  @Override
  protected void closeInternal() throws SQLException {
    throw new ExInternal("Automatic closing is not supported by this class, you must call finaliseAndClose() explicitly");
  }
}
