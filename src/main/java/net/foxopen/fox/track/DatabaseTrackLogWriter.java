package net.foxopen.fox.track;

import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.job.FoxJobPool;
import net.foxopen.fox.job.FoxJobTask;
import net.foxopen.fox.job.TaskCompletionMessage;
import net.foxopen.fox.sql.SQLManager;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.EnumMap;
import java.util.Map;


class DatabaseTrackLogWriter
implements TrackLogWriter {

  private static final Map<TrackTimer, String> SERIALISE_TIMER_COL_NAMES;
  private static final TrackLogWriter INSTANCE;
  static {
    SERIALISE_TIMER_COL_NAMES = new EnumMap<>(TrackTimer.class);
    SERIALISE_TIMER_COL_NAMES.put(TrackTimer.THREAD_DESERIALISE, "thread_deserialise_time_ms");
    SERIALISE_TIMER_COL_NAMES.put(TrackTimer.MODULE_LOAD, "module_load_time_ms");
    SERIALISE_TIMER_COL_NAMES.put(TrackTimer.THREAD_RAMP_UP, "ramp_up_time_ms");
    SERIALISE_TIMER_COL_NAMES.put(TrackTimer.ACTION_PROCESSING, "action_time_ms");
    SERIALISE_TIMER_COL_NAMES.put(TrackTimer.THREAD_RAMP_DOWN, "ramp_down_time_ms");
    SERIALISE_TIMER_COL_NAMES.put(TrackTimer.OUTPUT_GENERATION, "response_time_ms");
    SERIALISE_TIMER_COL_NAMES.put(TrackTimer.THREAD_SERIALISE, "thread_serialise_time_ms");
    SERIALISE_TIMER_COL_NAMES.put(TrackTimer.DATABASE_EXECUTION, "database_time_ms");
    SERIALISE_TIMER_COL_NAMES.put(TrackTimer.GET_CONNECTION, "get_connection_time_ms");
    SERIALISE_TIMER_COL_NAMES.put(TrackTimer.THREAD_LOCK_MANAGEMENT, "thread_lock_management_time_ms");

    INSTANCE = new DatabaseTrackLogWriter();
  }

  private final FoxJobPool mTrackWriterJobPool = FoxJobPool.createSingleThreadedPool("Track Writer");

  private final ParsedStatement mInsertStatement;

  public static TrackLogWriter instance() {
    return INSTANCE;
  }

  private DatabaseTrackLogWriter() {

    //Create a dynamic insert statement based on the properties and timers we're interested in
    String lColNameClause =  "INSERT INTO " + SQLManager.instance().getFoxSchemaName() + ".fox_thread_tracks (" +
      "id, track_data, server_hostname, server_context, request_id, overall_time_ms, track_written_timestamp, track_open_timestamp, track_close_timestamp";

    for(TrackProperty lProperty : TrackProperty.values()) {
      lColNameClause += ", " + lProperty.getColumnName();
    }

    for(String lTimerCol : SERIALISE_TIMER_COL_NAMES.values()) {
      lColNameClause += ", " + lTimerCol;
    }

    String lValuesClause = "VALUES (" +
      ":id, :track_data, :server_hostname, :server_context, :request_id, :overall_time_ms, SYSTIMESTAMP, :track_open_timestamp, :track_close_timestamp";

    for(TrackProperty lProperty : TrackProperty.values()) {
      lValuesClause += ", :" + lProperty.getColumnName();
    }

    for(String lTimerCol : SERIALISE_TIMER_COL_NAMES.values()) {
      lValuesClause += ", :" + lTimerCol;
    }

    String lInsertStatement = lColNameClause + ")\n" + lValuesClause + ")\n";
    mInsertStatement = StatementParser.parseSafely(lInsertStatement, "Insert Track");
  }


  @Override
  public void writeTrack(final TrackLogger pTrackLogger) {

    mTrackWriterJobPool.submitTask(new FoxJobTask() {
      public TaskCompletionMessage executeTask() {
        try(UCon lUCon = ConnectionAgent.getConnection(FoxGlobals.getInstance().getEngineConnectionPoolName(), "Track Insert");) {
          //Custom BindObject for binding the XML data in to a SQLXML writer (avoids creating a DOM in memory)
          BindObject lXMLBind = new BindObject() {
            @Override
            public Object getObject(UCon pUCon) throws SQLException {
              SQLXML lSQLXML = pUCon.getJDBCConnection().createSQLXML();
              Writer lXMLWriter = lSQLXML.setCharacterStream();
              try {
                XMLTrackSerialiser.instance().serialiseToWriter(pTrackLogger, lXMLWriter, false);
                lXMLWriter.close();
              }
              catch (IOException e) {
                throw new ExInternal("Failed to serialise track to SQLXML", e);
              }
              return lSQLXML;
            }

            @Override
            public String getObjectDebugString() {
              return "[TRACKXML]";
            }

            @Override
            public BindSQLType getSQLType() {
              return BindSQLType.XML;
            }

            @Override
            public BindDirection getDirection() {
              return BindDirection.IN;
            }
          };

          String lTrackId = pTrackLogger.getTrackId();

          UConBindMap lBindMap = new UConBindMap()
            .defineBind(":id", lTrackId)
            .defineBind(":server_hostname", FoxGlobals.getInstance().getServerHostName())
            .defineBind(":server_context", FoxGlobals.getInstance().getContextPath())
            .defineBind(":track_data", lXMLBind)
            .defineBind(":request_id", pTrackLogger.getRequestId())
            .defineBind(":overall_time_ms", pTrackLogger.getRootEntry().getOutTime() - pTrackLogger.getRootEntry().getInTime())
            .defineBind(":track_open_timestamp", pTrackLogger.getOpenTime())
            .defineBind(":track_close_timestamp", pTrackLogger.getCloseTime());

          //Bind properties
          for(TrackProperty lProperty : TrackProperty.values()) {
            lBindMap.defineBind(":" + lProperty.getColumnName(), pTrackLogger.getProperty(lProperty));
          }

          //Bind timers
          for(Map.Entry<TrackTimer, String> lTimerCol : SERIALISE_TIMER_COL_NAMES.entrySet()) {
            long lTimerVal = pTrackLogger.getTimerValue(lTimerCol.getKey());
            lBindMap.defineBind(":" + lTimerCol.getValue(), lTimerVal != -1 ? lTimerVal : null);
          }

          lUCon.executeAPI(mInsertStatement, lBindMap);
          lUCon.commit();
          //UCon closed by try-with-resources
          return new TaskCompletionMessage("Track written for track id " + lTrackId);
        }
        catch (Throwable th) {
          throw new ExInternal("Track log write failed", th);
        }
      }
    });
  }
}
