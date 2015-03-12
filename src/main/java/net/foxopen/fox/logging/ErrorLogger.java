package net.foxopen.fox.logging;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.job.FoxJobPool;
import net.foxopen.fox.job.FoxJobTask;
import net.foxopen.fox.job.TaskCompletionMessage;
import net.foxopen.fox.sql.SQLManager;

import java.util.Date;

public class ErrorLogger {

  private static final String INSERT_LOG_FILENAME = "InsertErrorLog.sql";

  public static enum ErrorType {
    FATAL,
    SUPPRESSED;
  }

  private static final ErrorLogger INSTANCE = new ErrorLogger();

  private final FoxJobPool mErrorWriterJobPool = FoxJobPool.createSingleThreadedPool("ErrorLogger");

  public static ErrorLogger instance() {
    return INSTANCE;
  }

  private ErrorLogger() {
  }


  /**
   * Log an error to the fox_errors table and return the error ref of the new row (which should be the timestamp to ms)
   *
   * @param pError Error to log
   * @param pErrorType Type of error that occurred
   * @param pRequestId ID of the request to link this error to
   * @return Error ref of the error row that gets inserted
   */
  public long logError(Throwable pError, ErrorType pErrorType, String pRequestId) {
    return logError(pError, pErrorType, pRequestId, "");
  }

  /**
   * Log an error to the fox_errors table and return the error ref of the new row (which should be the timestamp to ms)
   *
   * @param pError Error to log
   * @param pErrorType Type of error that occurred
   * @param pRequestId ID of the request to link this error to
   * @param pAdditionalInfo Additional information to store in the error_detail column before the stacktrace for pError
   * @return Error ref of the error row that gets inserted
   */
  public long logError(final Throwable pError, final ErrorType pErrorType, final String pRequestId, final String pAdditionalInfo) {

    final Date lOccurredTimestamp = new Date();
    final long lErrorReference = System.currentTimeMillis();

    if (FoxLogger.getLogger().isTraceEnabled()) {
      FoxLogger.getLogger().error("ErrorLogger got passed {} error for request {} - {}", pErrorType, pRequestId, pAdditionalInfo, pError);
    }
    else {
      FoxLogger.getLogger().error("ErrorLogger got passed {} error for request {} - {}: {}", pErrorType, pRequestId, pAdditionalInfo, pError.getMessage());
    }

    mErrorWriterJobPool.submitTask(new FoxJobTask() {
      public TaskCompletionMessage executeTask() {
        ParsedStatement lInsertStatement = SQLManager.instance().getStatement(INSERT_LOG_FILENAME, getClass());

        UConBindMap lBindMap = new UConBindMap()
          .defineBind(":error_ref", lErrorReference)
          .defineBind(":error_type", pErrorType.toString())
          .defineBind(":error_detail", UCon.bindStringAsClob((!XFUtil.isNull(pAdditionalInfo) ? pAdditionalInfo + "\n\n" : "") + XFUtil.getJavaStackTraceInfo(pError)))
          .defineBind(":server_hostname", FoxGlobals.getInstance().getServerHostName())
          .defineBind(":server_context",  FoxGlobals.getInstance().getContextPath())
          .defineBind(":request_id", pRequestId)
          .defineBind(":error_occurred_timestamp", lOccurredTimestamp);

        try (UCon lUCon = ConnectionAgent.getConnection(FoxGlobals.getInstance().getEngineConnectionPoolName(), "Error Log Insert")) {
          lUCon.executeAPI(lInsertStatement, lBindMap);
          lUCon.commit();
          //UCon closed by try-with-resources
        }
        catch (ExServiceUnavailable | ExDB e) {
          throw new ExInternal("Failed to write error log", e);
        }

        return new TaskCompletionMessage("Error log successfully written");
      }
    });

    return lErrorReference;
  }
}
