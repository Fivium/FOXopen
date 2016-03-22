package net.foxopen.fox.entrypoint.servlets;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxRequestHttp;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.banghandler.InternalAuthentication;
import net.foxopen.fox.boot.EngineInitialisationController;
import net.foxopen.fox.boot.FoxBootStatusProvider;
import net.foxopen.fox.boot.InitialisationResult;
import net.foxopen.fox.boot.RuntimeStatusProvider;
import net.foxopen.fox.configuration.FoxConfigHandler;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExInternalConfiguration;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.logging.FoxLogger;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleDriver;
import org.apache.log4j.PropertyConfigurator;
import org.json.simple.JSONObject;
import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;

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

  private final static String FOX_ENVIRONMENT_COL_NAME = "ENVIRONMENT_KEY";

  private final static int TEST_CONNECTION_VALID_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(10);

  static {
    EngineStatus.instance().registerStatusProvider(new RuntimeStatusProvider());
    EngineStatus.instance().registerStatusProvider(new FoxBootStatusProvider());
  }

  @Override
  public final void init(ServletConfig pServletConfig) throws ServletException {
    super.init(pServletConfig);

    // Configure the logging properties for FoxLogger
    PropertyConfigurator.configure(pServletConfig.getServletContext().getRealPath("/WEB-INF/config/log4j.properties"));

    // Avoid the oracle XML parsers being used for document building and SAX parsing
    System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    System.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");


    // Initializes the OpenSAML library, loading default configurations
    try {
      DefaultBootstrap.bootstrap();
    }
    catch (ConfigurationException e) {
      throw new ExInternal("Failed to initialise the OpenSAML library, loading default configurations", e);
    }

    if (FoxGlobals.getInstance().isEngineInitialised()) {
      FoxLogger.getLogger().info("FoxBoot: already initialised");
      return;
    }
    else {
      //Initialise globals object with retrieved servlet config
      FoxGlobals.getInstance().initialise(pServletConfig.getServletContext());

      EngineInitialisationController.initialiseEngine(false);
    }

    FoxLogger.getLogger().info("FoxBoot init");
  }

  @Override
  public void destroy() {
    super.destroy();
    EngineInitialisationController.shutdownEngine();
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

  private void processHttpRequest(HttpServletRequest pRequest, HttpServletResponse pResponse) {
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
      processHandleConfigureRequest(lFoxRequest);
    }
    else if (lCommand.startsWith("!INIT")) {
      processInitRequest(lFoxRequest);
    }
    else if(lCommand.equalsIgnoreCase("!TESTCONNECTION")) {
      processTestConnectionRequest(lFoxRequest);
    }
    else if (lCommand.equalsIgnoreCase("!GETFOXENVIRONMENTS")) {
      processGetFoxEnvironmentsRequest(lFoxRequest);
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

  private static void processHandleConfigureRequest(FoxRequestHttp pFoxRequest) {
    JSONObject lResponse = new JSONObject();
    try {
      FoxConfigHandler.processConfigureHandleRequest(pFoxRequest);

      //Note: this suppresses all errors
      InitialisationResult lInitialisationResult = EngineInitialisationController.initialiseEngine(false);

      if (!lInitialisationResult.isEngineInitialised()) {
        throw new ExInternalConfiguration("Configuration saved successfully, but engine initialisation failed", lInitialisationResult.asException());
      }

      lResponse.put("message", "Configuration saved and engine initialised successfully.");
      lResponse.put("status", "success");
    }
    catch (Throwable th) {
      lResponse.put("message", XFUtil.getJavaStackTraceInfo(th));
      lResponse.put("status", "failed");
    }

    new FoxResponseCHAR("application/json", new StringBuffer(lResponse.toJSONString()), 0).respond(pFoxRequest);
  }

  private static void processInitRequest(FoxRequest pFoxRequest) {
    InitialisationResult lInitialisationResult = EngineInitialisationController.initialiseEngine(false);

    String lInitResult;
    if (lInitialisationResult.isEngineInitialised()) {
      lInitResult = "Engine initialised";
    }
    else {
      lInitResult = XFUtil.getJavaStackTraceInfo(lInitialisationResult.asException());
    }

    new FoxResponseCHAR("text/plain", new StringBuffer(lInitResult), 0).respond(pFoxRequest);
  }

  private static void processTestConnectionRequest(FoxRequest pFoxRequest) {
    boolean lIsConnectionSuccess = false;

    try {
      OracleConnection lConnection = getDatabaseConnectionFromRequest(pFoxRequest.getHttpRequest());
      lConnection.close();
      lIsConnectionSuccess = true;
    }
    catch (ExInternalConfiguration | SQLException e) {
      createJsonError("Connection test failed: " + e.getMessage()).respond(pFoxRequest);
    }

    JSONObject lJSONConnectionResult = new JSONObject();
    lJSONConnectionResult.put("status", lIsConnectionSuccess ? "success" : "failure");
    new FoxResponseCHAR("application/json", new StringBuffer(lJSONConnectionResult.toJSONString()), 0).respond(pFoxRequest);
  }

  private static void processGetFoxEnvironmentsRequest(FoxRequest pFoxRequest) {
    JSONObject lJSONEnvironmentResult = new JSONObject();
    boolean lIsSuccess = false;

    try {
      List<String> lFoxEnvironments = getFoxEnvironmentsFromRequest(pFoxRequest.getHttpRequest());
      if (!lFoxEnvironments.isEmpty()) {
        lJSONEnvironmentResult.put("fox_environment_list", lFoxEnvironments);
      }
      else {
        throw new ExInternalConfiguration("No fox environments were returned from the query. Please check your fox environments table.");
      }

      lIsSuccess = true;
    }
    catch (ExInternalConfiguration e) {
      createJsonError("Failed to get fox environments: " + e.getMessage()).respond(pFoxRequest);
    }

    lJSONEnvironmentResult.put("status", lIsSuccess ? "success" : "failure");
    new FoxResponseCHAR("application/json", new StringBuffer(lJSONEnvironmentResult.toJSONString()), 0).respond(pFoxRequest);
  }

  private static List<String> getFoxEnvironmentsFromRequest(HttpServletRequest pRequest) {
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

  private static List<String> getFoxEnvironments(OracleConnection pConnection) throws ExInternalConfiguration {
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

  private static OracleConnection getDatabaseConnectionFromRequest(HttpServletRequest pRequest) throws ExInternalConfiguration {
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
  private static String getUpdatedDatabaseUserPassword(String pUsername, String pNewPassword) {
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
  private static OracleConnection getDatabaseConnection(String pDBURL, String pDBUsername, String pDBPassword) throws ExInternalConfiguration {
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

      if (!lConnection.isValid(TEST_CONNECTION_VALID_TIMEOUT_MS)) {
        lConnection.close();
        throw new ExInternalConfiguration("Connection opened but is not valid after "
                                          + TimeUnit.MILLISECONDS.toSeconds(TEST_CONNECTION_VALID_TIMEOUT_MS)
                                          + " seconds");
      }

      return lConnection;
    }
    catch (SQLException e) {
      throw new ExInternalConfiguration("Failed to connect. Please check the username / password / service combination is correct. " + e.getMessage() +
                             (e.getCause() != null ? "\n\nCaused by: " + e.getCause().getMessage() : ""));
    }
  }

  private static FoxResponse createJsonError(String pErrorMessage) {
    JSONObject lJSONResponse = new JSONObject();
    lJSONResponse.put("status", "false");
    lJSONResponse.put("message", pErrorMessage);
    return new FoxResponseCHAR("application/json", new StringBuffer(lJSONResponse.toJSONString()), 0);
  }
}
