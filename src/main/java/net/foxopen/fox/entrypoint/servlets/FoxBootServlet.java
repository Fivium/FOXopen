package net.foxopen.fox.entrypoint.servlets;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FoxBootServlet
extends HttpServlet {

  public static final String BOOT_SERVLET_PATH = "foxboot";

  private final static String FOX_ENVIRONMENT_POOL_NAME = "fox-environment-connection";
  private final static String FOX_ENVIRONMENT_COL_NAME = "ENVIRONMENT_KEY";

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
      // Load up engine mirror cache
      //EngineMirror.init(); // TODO - NP - Reimplement Engine Mirror

      // Load internal components
      ComponentManager.loadInternalComponents();

      // Populate error pages from engine mirror cache
//      populateErrorComponentMap();

      // Scan for plugins and get names ready to post when registering engine
      PluginManager.instance().scanPluginDirectory();

      // Load initial enum fox caches
      CacheManager.reloadFoxCaches();

      // Load config (do this safe)
      FoxConfigHelper.getInstance().loadEngineBootConfig();

      // Set the global info for freshly created connections
      UCon.setGlobalInfo(FoxGlobals.getInstance().getContextName() + ' ' + XFUtil.nvl(FoxGlobals.getInstance().getEngineVersionNumber()));

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

      //Refresh the Engine Mirror
      //EngineMirror.loadCacheFromDatabase(false); // TODO - NP - Reimplement Engine Mirror

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

  // TODO - NP - Put this in component manager class
//  public static boolean populateErrorComponentMap() {
//    try {
//      DOM lErrorComponentsDOM = DOM.createDocument(new File(EngineMirror.getMirrorFolder() + File.separator + "error_pages.xml"), false);
//      gDefaultErrorApp = lErrorComponentsDOM.get1SNoEx("/*/DEFAULT-ERROR-APP");
//      DOMList lErrorComponentsDOMList = lErrorComponentsDOM.getUL("/*/COMPONENT");
//      DOM lErrorComonentDOM;
//      if (gErrorComponentMap.size() > 0) {
//        gErrorComponentMap.clear();
//      }
//      while((lErrorComonentDOM = lErrorComponentsDOMList.popHead()) != null) {
//        gErrorComponentMap.put(lErrorComonentDOM.get1SNoEx("APP"), lErrorComonentDOM.get1SNoEx("NAME"));
//        gAppStatuses.put(lErrorComonentDOM.get1SNoEx("APP")+": error-module", new String[] {"CONFIGURED", "<a href=\"" + lErrorComonentDOM.get1SNoEx("APP")+"/"+lErrorComonentDOM.get1SNoEx("NAME") + "\">" + lErrorComonentDOM.get1SNoEx("NAME") + "</a>"});
//      }
//      return true;
//    }
//    catch (Throwable th) {
//      return false;
//    }
//  }


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
      boolean lConnectionAndEnvironmentCheck = false;

      JSONObject lJSONConnectionAndEnvironmentResult = new JSONObject();

      try {
        String lDBURL = pRequest.getParameter("db_url");
        String lDBUsername = pRequest.getParameter("db_user");
        String lDBPassword = pRequest.getParameter("db_password"); // TODO - This should look up in existing config if 160's

        // Null check
        if (XFUtil.isNull(lDBURL) || XFUtil.isNull(lDBUsername) || XFUtil.isNull(lDBPassword)) {
          createJsonError("You must provide a database URL, username and password in order to check a connection.").respond(lFoxRequest);
        }
        else {
          OracleDriver lDriver = new OracleDriver();

          Properties lProperties = new Properties();
          lProperties.setProperty("user", lDBUsername);
          lProperties.setProperty("password", lDBPassword);

          OracleConnection lTestConnection = (OracleConnection) lDriver.connect("jdbc:oracle:thin:@"+lDBURL, lProperties);

          // Get the fox environments
          ParsedStatement lFoxEnvironmentQuery = null;
          try {
            lFoxEnvironmentQuery = StatementParser.parse("SELECT fe.environment_key FROM " + lDBUsername + ".fox_environments fe", "Get Fox Connections");
          }
          catch (ExParser e) {
            createJsonError("The query to get the fox environments on testing a connection failed. " + e.getMessage()).respond(lFoxRequest);
          }

          List<UConStatementResult> lQueryResult = null;
          try {
            UCon lUCon = UCon.createUCon(lTestConnection, "Test Main Fox Connection");
            lQueryResult = lUCon.queryMultipleRows(lFoxEnvironmentQuery);
          }
          catch (ExDB e) {
            createJsonError("Database error when trying to acquire the fox environments: " + e.getMessage()).respond(lFoxRequest);
          }

          // If there are 1 or more fox environments add the root element
          // otherwise add the error message to message
          List<String> lFoxEnvironmentList = new ArrayList<>();
          if (lQueryResult != null && lQueryResult.size() >= 1) {
            // Loop over fox environments and add them to xml
            for (UConStatementResult lRow : lQueryResult) {
              lFoxEnvironmentList.add(lRow.getString(FOX_ENVIRONMENT_COL_NAME));
            }
            // Add to JSON object
            lJSONConnectionAndEnvironmentResult.put("fox_environment_list", lFoxEnvironmentList);
          }
          else {
            createJsonError("No fox environments were returned from the query. Please check your fox environments table. Using database schema " + lDBUsername).respond(lFoxRequest);
          }

          // Close connection
          ConnectionAgent.shutdownPoolIfExists(FOX_ENVIRONMENT_POOL_NAME);

          // The pool is set to fail fast so if it gets here then the connection is alive and the environments have been acquired without error
          lConnectionAndEnvironmentCheck = true;
        }

        lJSONConnectionAndEnvironmentResult.put("status", lConnectionAndEnvironmentCheck ? "success" : "failure");
      }
      catch (Throwable e) {
        createJsonError("Failed to connect. Please check the username / password / service combination is correct. " + e.getMessage() +
                        (e.getCause() != null ? "\n\nCaused by: " + e.getCause().getMessage() : "")).respond(lFoxRequest);
      }

      new FoxResponseCHAR("application/json", new StringBuffer(lJSONConnectionAndEnvironmentResult.toJSONString()), 0).respond(lFoxRequest);
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
