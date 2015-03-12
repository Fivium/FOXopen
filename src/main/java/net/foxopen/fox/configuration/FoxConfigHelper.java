package net.foxopen.fox.configuration;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentDefinition;
import net.foxopen.fox.configuration.resourcemaster.model.ConfiguredFoxEnvironment;
import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.ConnectionPoolConfig;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.plugin.PluginManager;
import net.foxopen.fox.sql.SQLManager;

import java.io.File;
import java.util.List;

public class FoxConfigHelper {

  private static final String REGISTER_ENGINE_FILENAME = "RegisterEngine.sql";
  private static final String DELETE_ENGINE_FILENAME = "DeleteEngine.sql";
  private static final String GET_FOX_CONNECTIONS_FILENAME = "GetFoxConnections.sql";
  private static final String POOL_NAME_COL = "POOL_NAME";
  private static final String USERNAME_COL = "USERNAME";

  private static final FoxConfigHelper INSTANCE = new FoxConfigHelper();

  private FoxConfigHelper() {
   // Do nothing just to stop people getting it.
  }

  public static FoxConfigHelper getInstance() {
    return INSTANCE;
  }

  public void loadEngineBootConfig() throws ExFoxConfiguration {
    // Load Boot dom from the config file
    try {
      FoxGlobals.getInstance().setFoxBootConfig(FileBasedFoxBootConfig.createFoxBootConfig());
      //Ensure any SQL files with references to the FOXMGR schema are flushed in case the schema name has changed
      SQLManager.instance().flushCache();
      //gStatusCategory.addMessage("Boot File", "Boot File found", MessageLevel.SUCCESS);
    }
    catch (ExFoxConfiguration pExFoxConfiguration) {
      FoxGlobals.getInstance().setFoxBootConfig(new UnconfiguredFoxBootConfig());
      //gStatusCategory.addMessage("Boot File", "Boot File not found", MessageLevel.ERROR);
      throw pExFoxConfiguration;
    }
  }

  /**
   * Register this engine with the database and load up a FoxEnvironment from the Resource Master tables
   *
   * @param pContextUCon
   * @throws ExServiceUnavailable
   * @throws ExApp
   * @throws ExFoxConfiguration
   */
  public void loadResourceMaster(ContextUCon pContextUCon) throws ExServiceUnavailable, ExApp, ExFoxConfiguration {
    registerFoxEngine(pContextUCon);
    String lEnvironmentKey = FoxGlobals.getInstance().getFoxBootConfig().getFoxEnvironmentKey();
    FoxEnvironmentDefinition lFoxEnvironmentDefinition = createFoxEnvironmentDefinitionFromDatabase(pContextUCon, lEnvironmentKey);
    FoxGlobals.getInstance().setFoxEnvironment(ConfiguredFoxEnvironment.createFoxEnvironment(pContextUCon, lEnvironmentKey, lFoxEnvironmentDefinition));
  }

  private void registerFoxEngine(ContextUCon pContextUCon) throws ExFoxConfiguration {
    if (FoxGlobals.getInstance().getFoxBootConfig() != null) {
      UCon lPrimaryDBUCon = null;
      try {
        lPrimaryDBUCon = pContextUCon.getUCon("Register Engine");
        // Register engine
        registerEngine(lPrimaryDBUCon, FoxGlobals.getInstance().getEngineLocator(), FoxGlobals.getInstance().getEngineSecurityToken(), FoxGlobals.getInstance().getFoxBootConfig());
        // Load Connection details
        loadFoxConnections(FoxGlobals.getInstance().getEngineLocator(), FoxGlobals.getInstance().getFoxBootConfig().getFoxEnvironmentKey(), lPrimaryDBUCon);
      }
      catch (ExFoxConfiguration e) {
        throw new ExFoxConfiguration("There was an error registering the fox engine with locator '" + FoxGlobals.getInstance().getEngineLocator() + "' to the database.", e);
      }
      finally {
        if (lPrimaryDBUCon != null) {
          pContextUCon.returnUCon(lPrimaryDBUCon, "Register Engine");
        }
      }
    }
  }

  private FoxEnvironmentDefinition createFoxEnvironmentDefinitionFromDatabase(ContextUCon pContextUCon, String pFoxEnvironment) throws ExServiceUnavailable, ExApp, ExFoxConfiguration {
    // Initialise here. Call parse method.
    FoxEnvironmentDefinition lFoxEnvironmentDefinition;
    UCon lPrimaryDBUCon = null;
    try {
      lPrimaryDBUCon = pContextUCon.getUCon("Create Fox Environment");
      return FoxEnvironmentDefinition.createFoxEnvironmentDefinition(lPrimaryDBUCon, pFoxEnvironment);
    }
    finally {
      pContextUCon.returnUCon(lPrimaryDBUCon, "Create Fox Environment");
    }
  }

  /**
   * Grab all connections for this engine from the database, to be used when constructing the connections.
   */
  public void loadFoxConnections(String pEngineLocator, String pEnvironmentKey, UCon pSetupUCon) throws ExFoxConfiguration {
    UConBindMap lBindMap = new UConBindMap();
    lBindMap.defineBind(":p_engine_locator", pEngineLocator);
    lBindMap.defineBind(":p_environment_key", pEnvironmentKey);

    List<UConStatementResult> lConnectionResultList;
    try {
      lConnectionResultList = pSetupUCon.queryMultipleRows(SQLManager.instance().getStatement(GET_FOX_CONNECTIONS_FILENAME, FoxConfigHelper.class), lBindMap);
      for (UConStatementResult lConnectionResult : lConnectionResultList) {
        String lPoolName = lConnectionResult.getString(POOL_NAME_COL);
        String lDBUserName = lConnectionResult.getString(USERNAME_COL);

        if (FoxGlobals.getInstance().getEngineConnectionPoolName().equals(lPoolName)) {
          FoxLogger.getLogger().error("Skipping over connection pool config '{}' in database connection pools config, this pool is the engine connection pool and cannot be reconfigured by the database", FoxGlobals.getInstance().getEngineConnectionPoolName());
          continue;
        }

        String lDBPassword;
        try {
          lDBPassword = FoxGlobals.getInstance().getFoxBootConfig().getDatabaseUserPassword(lDBUserName);
        }
        catch (ExFoxConfiguration e) {
          throw new ExFoxConfiguration("Failed to retrieve password for user '" + lDBUserName + "' when constructing connection pool '" + lPoolName + "'", e);
        }
        ConnectionPoolConfig lConnectionPoolConfig = ConnectionPoolConfig.createConnectionPoolConfig(lConnectionResult, FoxGlobals.getInstance().getFoxBootConfig().getDatabaseURL(), lDBPassword);
        ConnectionAgent.shutdownPoolIfExists(lConnectionPoolConfig.getPoolName());
        ConnectionAgent.registerPool(lConnectionPoolConfig);
      }
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to get fox connections with engine locator " + pEngineLocator + " and environment key " + pEnvironmentKey, e);
    }

  }

  private DOM getPluginDOM() {
    DOM lPluginDOM = DOM.createDocument("FOX_PLUGIN_LIST");
    List<String> lPluginNames = PluginManager.instance().getPluginFileNames();
    for (String lPluginName : lPluginNames) {
      lPluginDOM.addElem("FOX_PLUGIN").addElem("PLUGIN_NAME").setText(lPluginName);
    }
    return lPluginDOM;
  }

  public void registerEngine(UCon pUCon, String pEngineLocator, String pSecurityToken, FoxBootConfig pFoxBootConfig) throws ExFoxConfiguration {
    // Register the engine with the primary database
    DOM lPluginDOM = getPluginDOM();

    UConBindMap lBindMap = new UConBindMap();
    lBindMap.defineBind(":p_engine_locator", pEngineLocator);
    lBindMap.defineBind(":p_plugin_list", lPluginDOM);
    lBindMap.defineBind(":p_security_token", pSecurityToken);
    lBindMap.defineBind(":p_environment_key", pFoxBootConfig.getFoxEnvironmentKey());
    lBindMap.defineBind(":status_out", UCon.bindOutString());

    UConStatementResult lAPIResult = null;
    try {
      lAPIResult = pUCon.executeAPI(SQLManager.instance().getStatement(REGISTER_ENGINE_FILENAME, FoxConfigHelper.class), lBindMap);
      String lStatus = lAPIResult.getString(":status_out");

      if (!lStatus.equals("SUCCESS")) {
        throw new ExFoxConfiguration("Registering the engine was not a success, problem: " + lStatus);
      }
    }
    catch (ExDB e) {
      throw new ExFoxConfiguration("Error registering the engine. ", e);
    }
  }

  public static void deleteEngine(UCon pUCon, String pEnvironmentKey, String pEngineLocator) throws ExFoxConfiguration {
    UConBindMap lBindMap = new UConBindMap();
    lBindMap.defineBind(":p_environment_key", pEnvironmentKey);
    lBindMap.defineBind(":p_engine_locator", pEngineLocator);
    lBindMap.defineBind(":status_out", UCon.bindOutString());

    UConStatementResult lAPIResult;
    try {
      lAPIResult = pUCon.executeAPI(SQLManager.instance().getStatement(DELETE_ENGINE_FILENAME, FoxConfigHelper.class), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Error deleting the engine. ", e);
    }

    String lStatus = lAPIResult.getString(":status_out");

    if (!lStatus.equals("SUCCESS")) {
      throw new ExFoxConfiguration("Deleting the engine was not a success, the status returned was failed " + lStatus);
    }
  }

  public void deleteConfigFile(String pBootFilePath) {
    new File(pBootFilePath + ".config").delete();
  }

  public static String hashInternalPassword(String pUnhashedPassword) {
    return XFUtil.md5(pUnhashedPassword);
  }

  public static boolean verifyInternalPassword(String pCurrentPassword, String pSentPassword) {
    return pCurrentPassword.equals(hashInternalPassword(pSentPassword));
  }
}
