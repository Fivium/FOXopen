package net.foxopen.fox.entrypoint.servlets;

import com.google.common.primitives.Ints;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.FoxRequestHttp;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.banghandler.InternalAuthentication;
import net.foxopen.fox.boot.FoxBootStatusProvider;
import net.foxopen.fox.boot.RuntimeStatusProvider;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.configuration.FoxConfigHandler;
import net.foxopen.fox.configuration.FoxConfigHelper;
import net.foxopen.fox.configuration.resourcemaster.model.UnconfiguredFoxEnvironment;
import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternalConfiguration;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.job.FoxJobPool;
import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.plugin.PluginManager;
import net.foxopen.fox.queue.ServiceQueueHandler;
import net.foxopen.fox.thread.persistence.DatabaseSharedDOMManager;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleDriver;
import org.apache.log4j.PropertyConfigurator;
import org.json.simple.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FoxBootServlet
extends HttpServlet {

  public static final String BOOT_SERVLET_PATH = "foxboot";

  private final static String FOX_ENVIRONMENT_POOL_NAME = "fox-environment-connection";
  private final static String FOX_ENVIRONMENT_COL_NAME = "ENVIRONMENT_KEY";
  private final static int TEST_CONNECTION_VALID_TIMEOUT_MILLIS = Ints.checkedCast(TimeUnit.SECONDS.toMillis(10));

  private static Throwable gLastBootError;

  static {
    EngineStatus.instance().registerStatusProvider(new RuntimeStatusProvider());
    EngineStatus.instance().registerStatusProvider(new FoxBootStatusProvider());
  }

  @Override
  public final void init(ServletConfig pServletConfig) throws ServletException {
    super.init(pServletConfig);

    // Configure the logging properties for FoxLogger
    PropertyConfigurator.configure(pServletConfig.getServletContext().getRealPath("/WEB-INF/config/log4j.properties"));

    if (FoxGlobals.getInstance().isEngineInitialised()) {
      // TODO - Clear down and re-call initReal(pServletConfig)
      FoxLogger.getLogger().info("FoxBoot: already initialised");
      return;
    }
    else {
      //Initialise globals object with retrieved servlet config
      FoxGlobals.getInstance().initialise(pServletConfig.getServletContext());

      initReal();
    }

    FoxLogger.getLogger().info("FoxBoot init");
  }

  private void initReal() {
    try {
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

      // TODO - NP - This should probably happen as part of loadResourceMaster() above
      //initAuthenticationPackage();

      //Re-parse cached queries in case database user changed
      DatabaseSharedDOMManager.parseQueries();

      //Mark engine as initialised
      FoxGlobals.getInstance().setEngineInitialised(true);

      // Scan plugin directory and load configured plugins now we're configured
      PluginManager.instance().scanAndLoadPlugins();

      gLastBootError = null;
    }
    catch (Throwable th) {
      gLastBootError = th;
      FoxLogger.getLogger().error("FoxBoot init failed", th);
      FoxGlobals.getInstance().setEngineInitialised(false);
    }
  }

  @Override
  public void destroy() {
    super.destroy();
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

  @Override
  protected void doGet(HttpServletRequest pRequest, HttpServletResponse pResponse)
  throws ServletException, IOException {
    processHttpRequest(pRequest, pResponse);
  }

  @Override
  protected void doPost(HttpServletRequest pRequest, HttpServletResponse pResponse)
  throws ServletException, IOException {
    processHttpRequest(pRequest, pResponse);
  }

  public final void processHttpRequest(HttpServletRequest pRequest, HttpServletResponse pResponse) {
    FoxRequestHttp lFoxRequest = new FoxRequestHttp(pRequest, pResponse);

    boolean lAuthResult = InternalAuthentication.instance().authenticate(lFoxRequest, InternalAuthLevel.INTERNAL_ADMIN);
    if(!lAuthResult) {
      //Internal authenticator should issue challenge response
      return;
    }

    String lCommand = lFoxRequest.getRequestURI();
    int lBangIndex = lFoxRequest.getRequestURI().indexOf("!");
    if (lBangIndex >= 0) {
      lCommand = lCommand.substring(lBangIndex);
    }

    if(lCommand.startsWith("!SECURITY")) {
      FoxConfigHandler.processSecurityRequest(lFoxRequest).respond(lFoxRequest);
    }
    else if(lCommand.startsWith("!HANDLESECURITY")) {
      FoxConfigHandler.processSecurityHandleRequest(lFoxRequest).respond(lFoxRequest);
    }
    else if(lCommand.startsWith("!HANDLECONFIGURE")) {
      try {
        FoxResponse lFoxResponse = FoxConfigHandler.processConfigureHandleRequest(lFoxRequest);
        //Note: this suppressed all errors
        initReal();
        //TODO PN - report save success but initReal failure to consumer
        lFoxResponse.respond(lFoxRequest);
      }
      catch (ExFoxConfiguration|ExTooFew|ExTooMany|ExBadPath|ExServiceUnavailable e) {
        throw new ExInternalConfiguration("Could not process handle configure command.", e);
      }
    }
    else if (lCommand.startsWith("!INIT")) {
      initReal();
      new FoxResponseCHAR("text/plain", new StringBuffer("Engine initialised"), 0).respond(lFoxRequest);
    }
    else if(lCommand.equalsIgnoreCase("!TESTCONNECTION")) {
      boolean lIsConnectionSuccess = false;

      try {
        OracleConnection lConnection = getDatabaseConnectionFromRequest(pRequest);
        lConnection.close();
        lIsConnectionSuccess = true;
      }
      catch (ExInternalConfiguration | SQLException e) {
        createJsonError("Connection test failed: " + e.getMessage()).respond(lFoxRequest);
      }

      JSONObject lJSONConnectionResult = new JSONObject();
      lJSONConnectionResult.put("status", lIsConnectionSuccess ? "success" : "failure");
      new FoxResponseCHAR("application/json", new StringBuffer(lJSONConnectionResult.toJSONString()), 0).respond(lFoxRequest);
    }
    else if (lCommand.equalsIgnoreCase("!GETFOXENVIRONMENTS")) {
      JSONObject lJSONEnvironmentResult = new JSONObject();
      boolean lIsSuccess = false;

      try {
        List<String> lFoxEnvironments = getFoxEnvironmentsFromRequest(pRequest);
        if (!lFoxEnvironments.isEmpty()) {
          lJSONEnvironmentResult.put("fox_environment_list", lFoxEnvironments);
        }
        else {
          throw new ExInternalConfiguration("No fox environments were returned from the query. Please check your fox environments table.");
        }

        lIsSuccess = true;
      }
      catch (ExInternalConfiguration e) {
        createJsonError("Failed to get fox environments: " + e.getMessage()).respond(lFoxRequest);
      }

      lJSONEnvironmentResult.put("status", lIsSuccess ? "success" : "failure");
      new FoxResponseCHAR("application/json", new StringBuffer(lJSONEnvironmentResult.toJSONString()), 0).respond(lFoxRequest);
    }
    else {
      try {
        FoxConfigHandler.processConfigureRequest(lFoxRequest);
      }
      catch (ExFoxConfiguration | ExTooFew | ExTooMany e) {
        throw new ExInternalConfiguration("Could not process configure command.", e);
      }
    }
  }

  private List<String> getFoxEnvironmentsFromRequest(HttpServletRequest pRequest) {
    OracleConnection lConnection;
    try {
      lConnection = getDatabaseConnectionFromRequest(pRequest);
    }
    catch (ExInternalConfiguration e) {
      throw new ExInternalConfiguration("Failed to connect to database while getting fox environments", e);
    }

    List<String> lFoxEnvironments;
    try {
      lFoxEnvironments = getFoxEnvironments(lConnection);
    }
    catch (ExInternalConfiguration e) {
      throw new ExInternalConfiguration("Failed to get fox environments from database", e);
    }
    finally {
      try {
        lConnection.close();
      }
      catch (SQLException e) {
        throw new ExInternalConfiguration("Failed to close fox environments database connection", e);
      }
    }

    return lFoxEnvironments;
  }

  private List<String> getFoxEnvironments(OracleConnection pConnection) throws ExInternalConfiguration {
    String lUsername;

    try {
      lUsername = pConnection.getUserName();
    }
    catch (SQLException e) {
      throw new ExInternalConfiguration("Could not get username from connection", e);
    }

    try {
      ParsedStatement lFoxEnvironmentQuery = StatementParser.parse("SELECT fe." + FOX_ENVIRONMENT_COL_NAME + " FROM " + lUsername + ".fox_environments fe", "Get Fox Environments");
      UCon lUCon = UCon.createUCon(pConnection, "Get Fox Environments");
      List<UConStatementResult> lQueryResult = lUCon.queryMultipleRows(lFoxEnvironmentQuery);

      // Return the fox environments from the query result
      return lQueryResult.stream()
                         .map(pRow -> pRow.getString(FOX_ENVIRONMENT_COL_NAME))
                         .collect(Collectors.toList());
    }
    catch (ExParser e) {
      throw new ExInternalConfiguration("The query to get the fox environments failed", e);
    }
    catch (ExDB e) {
      throw new ExInternalConfiguration("Database error when trying to acquire the fox environments", e);
    }
  }

  private OracleConnection getDatabaseConnectionFromRequest(HttpServletRequest pRequest) throws ExInternalConfiguration {
    String lDBURL = pRequest.getParameter("db_url");
    String lDBUsername = pRequest.getParameter("db_user");

    String lDBPassword = getUpdatedDatabaseUserPassword(lDBUsername, pRequest.getParameter("db_password"));
    return getDatabaseConnection(lDBURL, lDBUsername, lDBPassword);
  }

  /**
   * Returns the new password if the password is not obfuscated as a result of being loaded from config (i.e. the user
   * has updated the password), or if the password is obfuscated, the password loaded from the config for the username
   * @param pUsername The database username
   * @param pNewPassword The password value on the form
   * @return The updated password or the current password from config if not updated
   */
  private String getUpdatedDatabaseUserPassword(String pUsername, String pNewPassword) {
    String lPassword;

    if (XFUtil.isObfuscatedValue(pNewPassword)) {
      try {
        lPassword = FoxGlobals.getInstance().getFoxBootConfig().getDatabaseUserPassword(pUsername);
      }
      catch (ExFoxConfiguration e) {
        throw new ExInternalConfiguration("Password for user '" + pUsername + "' was obfuscated, but could not be found in configuration", e);
      }
    }
    else {
      lPassword = pNewPassword;
    }

    return lPassword;
  }

  /**
   * Returns an open and valid connection for the given database details. If the connection could not be opened an
   * exception is raised. If the connection was opened but is not valid, the connection is closed and an exception is
   * raised.
   * @param pDBURL
   * @param pDBUsername
   * @param pDBPassword
   * @return
   */
  private OracleConnection getDatabaseConnection(String pDBURL, String pDBUsername, String pDBPassword) throws ExInternalConfiguration {
    if (XFUtil.isNull(pDBURL) || XFUtil.isNull(pDBUsername) || XFUtil.isNull(pDBPassword)) {
      throw new ExInternalConfiguration("You must provide a database URL, username and password to connect.");
    }

    OracleDriver lDriver = new OracleDriver();
    Properties lProperties = new Properties();
    lProperties.setProperty("user", pDBUsername);
    lProperties.setProperty("password", pDBPassword);

    try {
      OracleConnection lConnection = (OracleConnection) lDriver.connect("jdbc:oracle:thin:@" + pDBURL, lProperties);

      if (lConnection == null) {
        throw new ExInternalConfiguration("Connection could not be opened as the database driver cannot open the URL");
      }

      if (!lConnection.isValid(TEST_CONNECTION_VALID_TIMEOUT_MILLIS)) {
        lConnection.close();
        throw new ExInternalConfiguration("Connection opened but is not valid after "
                                          + TimeUnit.MILLISECONDS.toSeconds(TEST_CONNECTION_VALID_TIMEOUT_MILLIS)
                                          + " seconds");
      }

      return lConnection;
    }
    catch (SQLException e) {
      throw new ExInternalConfiguration("Failed to connect. Please check the username / password / service combination is correct. " + e.getMessage() +
                             (e.getCause() != null ? "\n\nCaused by: " + e.getCause().getMessage() : ""));
    }
  }

  private FoxResponse createJsonError(String pErrorMessage) {
    JSONObject lJSONResponse = new JSONObject();
    lJSONResponse.put("status", "false");
    lJSONResponse.put("message", pErrorMessage);
    return new FoxResponseCHAR("application/json", new StringBuffer(lJSONResponse.toJSONString()), 0);
  }

  public static Throwable getLastBootError() {
    return gLastBootError;
  }
}
