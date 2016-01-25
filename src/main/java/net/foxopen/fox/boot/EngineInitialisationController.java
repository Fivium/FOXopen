package net.foxopen.fox.boot;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.configuration.FoxConfigHelper;
import net.foxopen.fox.configuration.resourcemaster.model.UnconfiguredFoxEnvironment;
import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.job.FoxJobPool;
import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.plugin.PluginManager;
import net.foxopen.fox.queue.ServiceQueueHandler;
import net.foxopen.fox.thread.persistence.DatabaseSharedDOMManager;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Single point of control for engine initialisation.
 */
public class EngineInitialisationController {

  private final static int INIT_REATTEMPT_THROTTLE_TIME_MS = (int) TimeUnit.SECONDS.toMillis(10);

  /** Lock to prevent concurrent init requests */
  private static final Lock INITIALISE_LOCK = new ReentrantLock();

  /** Tracks if the engine has been successfully initialised */
  private static boolean gEngineInitialised = false;

  /** Last error encountered by an init attempt - null for successful inits.  */
  private static Throwable gLastInitError;

  /** Last time an init was attempted (successful or failed) */
  private static long gLastInitAttemptTime;

  private EngineInitialisationController() { }

  /**
   * Checks if the engine is initialised and attempts to initialise it if not (with throttling applied).
   * @return An InitialisationResult representing success/failure (including exception details). If the engine was already
   * initialised, a "success" result is returned immediately.
   */
  public static InitialisationResult checkAndInitialise() {

    if (gEngineInitialised) {
      return InitialisationResult.successfulInitialisation();
    }
    else {
      return initialiseEngine(true);
    }
  }

  /**
   * Attempts to initialise the engine. If the engine is already initialised, all configuration will be reloaded and all caches
   * etc reset.
   * @param pAllowThrottling If true, throttling will be applied to stop init attempts running too frequently. This should
   *                         typically be false if a privileged system administrator is explicitly requesting an init.
   * @return Result of the initialisation attempt - any exceptions which occur will be reported in this object.
   */
  public static InitialisationResult initialiseEngine(boolean pAllowThrottling) {

    //Use the lock to immediately return a failure if this is a concurrent init request
    if (INITIALISE_LOCK.tryLock()) {
      try {
        long lLastInitTimeDifference = System.currentTimeMillis() - gLastInitAttemptTime;
        if (pAllowThrottling && lLastInitTimeDifference <= INIT_REATTEMPT_THROTTLE_TIME_MS) {
          return InitialisationResult.failedInitialisation("initialisation attempt was throttled, try again in " + (INIT_REATTEMPT_THROTTLE_TIME_MS - lLastInitTimeDifference) + " ms", gLastInitError);
        }

        try {
          gEngineInitialised = false;

          //Start by setting to UNCONFIGURED so we don't end up with an old configured environment if anything goes wrong
          FoxGlobals.getInstance().setFoxEnvironment(new UnconfiguredFoxEnvironment());

          // Load internal components
          ComponentManager.loadInternalComponents();

          // Scan for plugins and get names ready to post when registering engine
          PluginManager.instance().scanPluginDirectory();

          // Load initial enum fox caches
          CacheManager.reloadFoxCaches();

          // Load config (do this safe)
          FoxConfigHelper.getInstance().loadEngineBootConfig();

          // Set the global info for freshly created connections
          UCon.setGlobalInfo("/" + FoxGlobals.getInstance().getContextName());

          // Register a default engine connection pool
          FoxGlobals.getInstance().initialiseEngineConnectionPool();

          ContextUCon lContextUCon = ContextUCon.createContextUCon(FoxGlobals.getInstance().getEngineConnectionPoolName(), "FOX Boot connection");
          // Registers the engine, loads connections from the database and constructs the fox environment including all apps
          try {
            lContextUCon.pushConnection("FOX Boot");
            FoxConfigHelper.getInstance().loadResourceMaster(lContextUCon);
            lContextUCon.popConnection("FOX Boot");
          }
          finally {
            lContextUCon.rollbackAndCloseAll(true);
          }

          //Re-parse cached queries in case database user changed
          DatabaseSharedDOMManager.parseQueries();

          //Mark engine as initialised
          gEngineInitialised = true;

          // Scan plugin directory and load configured plugins now we're configured
          PluginManager.instance().scanAndLoadPlugins();

          //Null out the last init error as we have successfully initialised and it should not be needed any more
          gLastInitError = null;

          return InitialisationResult.successfulInitialisation();
        }
        catch (Throwable th) {
          //Any error encountered above should be treated as an init failure
          gEngineInitialised = false;
          gLastInitError = th;
          FoxLogger.getLogger().error("FoxBoot init failed", th);

          return InitialisationResult.failedInitialisation("error during initialisation", th);
        }
        finally {
          //Always record init time for status reporting/throttling
          gLastInitAttemptTime = System.currentTimeMillis();
        }
      }
      finally {
        INITIALISE_LOCK.unlock();
      }
    }
    else {
      //Error - an initialise attempt is already in progress
      return InitialisationResult.failedInitialisation("another thread is attempting to initialise the engine", gLastInitError);
    }
  }

  /**
   * Attempts to gracefully shut the engine down, e.g. by closing connection pools and deregistering from the database.
   * This method may throw exceptions.
   */
  public static void shutdownEngine() {
    FoxLogger.getLogger().info("Fox shutting down...");

    // Deregister engine, ignore all errors
    FoxLogger.getLogger().trace("de-registering engine from engines table");
    if (FoxGlobals.getInstance().getEngineConnectionPoolName() != null) {
      UCon lPrimaryDBUCon = null;
      try {
        lPrimaryDBUCon = ConnectionAgent.getConnection(FoxGlobals.getInstance().getEngineConnectionPoolName(), "Deregistering engine");
        FoxConfigHelper.getInstance().deleteEngine(lPrimaryDBUCon, FoxGlobals.getInstance().getFoxBootConfig().getFoxEnvironmentKey(), FoxGlobals.getInstance().getEngineLocator());
      }
      catch (Throwable th) {
        FoxLogger.getLogger().error("Error when deregistering the engine on shutdown on the database", th);
        // ignore any destroy errors here
      }
      finally {
        if (lPrimaryDBUCon != null) {
          lPrimaryDBUCon.closeForRecycle();
        }
      }
    }

    // Shut down upload service queues
    synchronized(ServiceQueueHandler.class) {
      ServiceQueueHandler.destroyAllQueueHandlers();
    }

    // Shut down jobs
    FoxJobPool.shutdownAllPools();

    // Shut down connection pools
    ConnectionAgent.shutdownAllPools();

    FoxLogger.getLogger().info("FOX shutdown complete");
  }

  /**
   * @return True if the FOX engine has been successfully initialised.
   */
  public static boolean isEngineInitialised() {
    return gEngineInitialised;
  }

  /**
   * @return Any error which occurred during the last initialisation attempt if it failed. May be null.
   */
  public static Throwable getLastInitError() {
    return gLastInitError;
  }

  /**
   * @return Last time an init was attempted (regardless of success/failure).
   */
  static long getLastInitAttemptTime() {
    return gLastInitAttemptTime;
  }
}
