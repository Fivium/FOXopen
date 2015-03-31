package net.foxopen.fox.database;

import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.StatusCollection;
import net.foxopen.fox.enginestatus.StatusDestination;
import net.foxopen.fox.enginestatus.StatusDetail;
import net.foxopen.fox.enginestatus.StatusItem;
import net.foxopen.fox.enginestatus.StatusMessage;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusTable;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.job.BasicFoxJobPool;
import net.foxopen.fox.job.FoxJobTask;
import net.foxopen.fox.job.ScheduledFoxJobPool;
import net.foxopen.fox.job.TaskCompletionMessage;
import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackTimer;
import oracle.jdbc.OracleConnection;
import oracle.xdb.XMLType;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 *
 */
public class ConnectionAgent {

  /**
   *  The factory which will provide new ConnectionPools. This may need to be configurable in the future.
   */
  private static ConnectionPoolFactory gConnectionPoolFactory = HikariConnectionPoolFactory.instance();

  static {
    EngineStatus.instance().registerStatusProvider(new ConnectionStatusProvider());

    /*
      Run a daily cleanup of the XMLType cache. This works around a memory leak present in the Oracle 11.2.0.4 XDB JAR,
      which permanently holds stale connection references in its BinXMLProcessor cache.
     */
    ScheduledFoxJobPool.instance().scheduleTask(new FoxJobTask() {
      @Override
      public String getTaskDescription() {
        return "XMLTypeCleanup";
      }

      @Override
      public TaskCompletionMessage executeTask() {
        XMLType.cleanupCache(true);
        return new TaskCompletionMessage(this, "XMLType cache cleanup successful");
      }
    });
  }

  /**
   * Job pool for recycling returned connections.
   */
  private static final BasicFoxJobPool RECYCLE_JOB_POOL = BasicFoxJobPool.createSingleThreadedPool("UCon Recycle");

  /**
   * Weak map between the base connection objects and the amount of times they have been recycled
   */
  private static final Map<Connection, Integer> CONNECTION_RECYCLE_COUNT_MAP = new WeakHashMap<>();

  private static final Map<String, ConnectionPool> CONNECTION_POOLS = new ConcurrentHashMap<>();


  /**
   * Check a pool name exists as a connection.
   *
   * @param pPoolName the name of the connection pool
   *
   * @return true if it was found, false if it was not found
   */
  public static boolean checkConnectionExists(String pPoolName) {
    return CONNECTION_POOLS.containsKey(pPoolName.toLowerCase());
  }

  /**
   * Create a new named connection pool based on the pool name in the connection pool config object
   *
   * @param pPoolConfig
   */
  public static void registerPool(ConnectionPoolConfig pPoolConfig) {
    if (CONNECTION_POOLS.containsKey(pPoolConfig.getPoolName().toLowerCase())) {
      throw new ExInternal("Attempted to register pool '" + pPoolConfig.getPoolName() + "' but it already exists");
    }

    ConnectionPool lConPool = gConnectionPoolFactory.createPool(pPoolConfig);
    CONNECTION_POOLS.put(pPoolConfig.getPoolName().toLowerCase(), lConPool);
  }

  /**
   * Reregisters a named connection pool based on the pool name in the connection pool config object
   * This method does not catch exceptions like register so the consuming codes decides what to do with them.
   * @param pPoolConfig
   */
  public static void reRegisterPool(ConnectionPoolConfig pPoolConfig) {
    ConnectionPool lNewPool = gConnectionPoolFactory.createPool(pPoolConfig);
    ConnectionPool lOldPool = CONNECTION_POOLS.put(pPoolConfig.getPoolName().toLowerCase(), lNewPool);
    // Possibly use mbeans to only shutdown when 0 active connections
    // Possibly add our own atomicint to conntectionpool class and inc on get, dec on release/close
    lOldPool.shutdownPool();
  }

  /**
   * Checks for an existing pool called pPoolName
   *
   * @param pPoolName
   * @return
   */
  public static boolean hasConnectionPool(String pPoolName) {
    return CONNECTION_POOLS.containsKey(pPoolName.toLowerCase());
  }


  /**
   * Remove a named connection pool from reference and ask it to shutdown.
   * Throws ExInternal if no connection pool exists with that name.
   *
   * @param pPoolName Name of the connection pool to shutdown
   */
  public static void shutdownPool(String pPoolName){
    synchronized(CONNECTION_POOLS) {
      ConnectionPool lConPool = CONNECTION_POOLS.remove(pPoolName.toLowerCase());

      if (lConPool == null) {
        throw new ExInternal("No Connection Pool called '" + pPoolName + "'");
      }
      else {
        lConPool.shutdownPool();
      }
    }
  }

  /**
   * Remove a named connection pool from reference and ask it to shutdown if it exists
   *
   * @param pPoolName Name of the connection pool to shutdown
   */
  public static void shutdownPoolIfExists(String pPoolName) {
    synchronized(CONNECTION_POOLS) {
      ConnectionPool lConPool = CONNECTION_POOLS.remove(pPoolName.toLowerCase());
      if (lConPool != null) {
        lConPool.shutdownPool();
      }
    }
  }

  /**
   * Remove all connection pools registered with this ConnectionAgent
   */
  public static void shutdownAllPools(){
    FoxLogger.getLogger().info("Shutting down all connection pools");
    synchronized(CONNECTION_POOLS) {
      for (Map.Entry<String, ConnectionPool> lConnectionPoolEntry : CONNECTION_POOLS.entrySet()) {
        lConnectionPoolEntry.getValue().shutdownPool();
        FoxLogger.getLogger().trace("Shutting down {} job pool", lConnectionPoolEntry.getKey());
      }
      CONNECTION_POOLS.clear();
    }
  }

  /**
   * Get a connection from the pool and return it ready for use
   *
   * @param pPoolName Name of pool to get the connection from
   * @param pPurpose Message to set in the connections ModuleInfo
   * @return Connection ready for use
   * @throws SQLException If the getConnection Fails
   */
  public static UCon getConnection(String pPoolName, String pPurpose)
  throws ExServiceUnavailable {
    ConnectionPool lConPool = CONNECTION_POOLS.get(pPoolName.toLowerCase());
    if (lConPool == null) {
      throw new ExInternal("No connection pool called '" + pPoolName + "' to get a connection from");
    }

    Connection lConnection;
    Track.pushDebug("PoolGetConnection");
    Track.timerStart(TrackTimer.GET_CONNECTION);
    try {
      Track.counterIncrement("PoolGetConnection"); //used for proof of concept, can probably be removed
      lConnection = lConPool.getConnection();
    }
    catch (Throwable e) {
      throw new ExServiceUnavailable("Failed to get a connection from pool '" + pPoolName + "' for '" + pPurpose + "'", e);
    }
    finally {
      Track.timerPause(TrackTimer.GET_CONNECTION);
      Track.pop("PoolGetConnection");
    }

    return new UCon(lConPool, lConnection, pPurpose);
  }

  /**
   * Close a connection
   * TODO - NP - Perhaps make this put to a queue like recycling? Though hopefully this isn't called often
   *
   * @param pDatabaseCon Connection to close
   */
  public static void closeConnection(UCon pDatabaseCon){
    pDatabaseCon.getConnectionPool().forceCloseConnection(pDatabaseCon.getJDBCConnection());
  }

  /**
   * Check in a connection for recycling
   *
   * @param pDatabaseCon Connection to recycle
   */
  public static void checkInForRecycle(UCon pDatabaseCon){
    // Add the UCon to a queue to be picked up by the recycle thread
    RECYCLE_JOB_POOL.submitTask(new UConRecycleTask(pDatabaseCon));
  }

  private static class UConRecycleTask
  implements FoxJobTask {

    private final UCon mRecycleUCon;

    private UConRecycleTask(UCon pRecycleUCon) {
      mRecycleUCon = pRecycleUCon;
    }

    @Override
    public TaskCompletionMessage executeTask() {
      String lPoolName = mRecycleUCon.getConnectionPool().getConfig().getPoolName();
      try {
        Connection lActualConnection = mRecycleUCon.getJDBCConnection();

        // Increment the recycle count (make sure we retrieve the physical DB connection to use as a map key, not the Hikari wrapper)
        Integer lRecycleCount = CONNECTION_RECYCLE_COUNT_MAP.get(lActualConnection.unwrap(OracleConnection.class));
        if (lRecycleCount == null) {
          lRecycleCount = 0;
        }

        if (lRecycleCount < mRecycleUCon.getConnectionPool().getConfig().getMaximumRecycles()) {
          String lCheckinSQL = mRecycleUCon.getConnectionPool().getConfig().getConnectionCheckinSQL();
          try {
            // Rollback anything that was previously going on
            mRecycleUCon.rollback();

            // Execute the recycle SQL
            mRecycleUCon.executeAPI(lCheckinSQL, "Connection Agent Check In SQL");
          }
          catch (ExDB e) {
            // Forcefully kill the connection if checkin failed
            mRecycleUCon.getConnectionPool().forceCloseConnection(lActualConnection);
            throw new ExInternal("Recycle connection job failed to clean itself up for recycle (rollback and check in SQL): " + lCheckinSQL, e);
          }

          // Put connection back in the pool
          CONNECTION_RECYCLE_COUNT_MAP.put(lActualConnection.unwrap(OracleConnection.class), ++lRecycleCount);
          mRecycleUCon.setModuleInfo("READY Recycled " + lRecycleCount);

          // Call general release to pool method for the connection
          mRecycleUCon.getConnectionPool().releaseConnection(lActualConnection);
          return new TaskCompletionMessage(this, "Closed connection returned to pool");
        }
        else {
          // Force close a connection if it hit its recycle limit
          mRecycleUCon.getConnectionPool().forceCloseConnection(lActualConnection);
          return new TaskCompletionMessage(this, "Closed connection forcibly closed");
        }
      }
      catch(Throwable th) {
        throw new ExInternal("Failed to return connection to pool " +  lPoolName, th);
      }
    }

    @Override
    public String getTaskDescription() {
      return "UConRecycle";
    }
  }

  private static class ConnectionStatusProvider
  implements StatusProvider {

    @Override
    public void refreshStatus(StatusDestination pDestination) {

      StatusTable lPoolTable = pDestination.addTable("Pool List", "Pool Name", "Active", "Idle", "Total", "Threads Waiting");
      lPoolTable.setRowProvider(new StatusTable.RowProvider() {
        @Override
        public void generateRows(StatusTable.RowDestination pRowDestination) {
          for (final ConnectionPool lPool : CONNECTION_POOLS.values()) {
            StatusTable.Row lPoolRow = pRowDestination.addRow(lPool.getConfig().getPoolName());
            lPoolRow.setColumn(lPool.getConfig().getPoolName());
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            try {
              ObjectName lPoolObjectName = new ObjectName("com.zaxxer.hikari:type=Pool (" + lPool.getConfig().getPoolName() + ")");
              lPoolRow.setColumn(mBeanServer.getAttribute(lPoolObjectName, "ActiveConnections").toString());
              lPoolRow.setColumn(mBeanServer.getAttribute(lPoolObjectName, "IdleConnections").toString());
              lPoolRow.setColumn(mBeanServer.getAttribute(lPoolObjectName, "TotalConnections").toString());
              lPoolRow.setColumn(mBeanServer.getAttribute(lPoolObjectName, "ThreadsAwaitingConnection").toString());

              lPoolRow.setColumn("Pool config", new StatusDetail("View pool config", new StatusDetail.Provider(){
                @Override
                public StatusItem getDetailMessage() {
                  StatusCollection lDetails = new StatusCollection("configDetails");
                  ConnectionPoolConfig lConfig = lPool.getConfig();
                  lDetails.addItem(new StatusMessage("Acquire timeout MS", Long.toString(lConfig.getAcquireTimeoutMS())));

                  lDetails.addItem(new StatusMessage("Connection alive test SQL", lConfig.getConnectionAliveTestSQL()));
                  lDetails.addItem(new StatusMessage("Connection checkin SQL", lConfig.getConnectionCheckinSQL()));
                  lDetails.addItem(new StatusMessage("Connection checkout SQL", lConfig.getConnectionCheckoutSQL()));
                  lDetails.addItem(new StatusMessage("Connection init SQL", lConfig.getConnectionInitSQL()));

                  lDetails.addItem(new StatusMessage("Database URL", lConfig.getURL()));
                  lDetails.addItem(new StatusMessage("Database user", lConfig.getUser()));
                  lDetails.addItem(new StatusMessage("Datasource class", lConfig.getDataSourceClassName()));

                  lDetails.addItem(new StatusMessage("Idle timeout MS", Long.toString(lConfig.getIdleTimeoutMS())));

                  lDetails.addItem(new StatusMessage("Maximum pool size", Integer.toString(lConfig.getMaxPoolSize())));
                  lDetails.addItem(new StatusMessage("Minimum pool size", Integer.toString(lConfig.getMinPoolSize())));
                  lDetails.addItem(new StatusMessage("Maximum recycles", Integer.toString(lConfig.getMaximumRecycles())));

                  return lDetails;
                }
              }));
            }
            catch (Throwable e) {
              lPoolRow.setColumn("Status Error", e.getMessage());
            }
          }
        }
      });
    }

    @Override
    public String getCategoryTitle() {
      return "Connection Pools";
    }

    @Override
    public String getCategoryMnemonic() {
      return "connectionPools";
    }

    @Override
    public boolean isCategoryExpandedByDefault() {
      return true;
    }
  }
}
