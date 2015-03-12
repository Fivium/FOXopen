package net.foxopen.fox.plugin;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginProperties {
  private static final String GEL_PLUGIN_PROPERTIES_SQL_FILENAME = "GetPluginProperties.sql";

  private final String mPluginName;
  private final Boolean mWsAllowed;
  private final Boolean mLoadAllowed;

  public static Map<String, PluginProperties> createAllPluginProperties(ContextUCon pContextUCon) {
    Map<String, PluginProperties> lPluginPropertyMap = new HashMap<>();

    UConBindMap lBindMap = new UConBindMap();
    lBindMap.defineBind(":p_environment_key", FoxGlobals.getInstance().getFoxBootConfig().getFoxEnvironmentKey());
    lBindMap.defineBind(":p_engine_locator", FoxGlobals.getInstance().getEngineLocator());

    UCon lUCon = null;
    try {
      lUCon = pContextUCon.getUCon("Get plugin properties");
      try {
        List<UConStatementResult> lPluginPropertyResultList =
          lUCon.queryMultipleRows(SQLManager.instance().getStatement(GEL_PLUGIN_PROPERTIES_SQL_FILENAME, PluginProperties.class), lBindMap);

        for (UConStatementResult lPluginPropertyResult : lPluginPropertyResultList) {
          final String PLUGIN_NAME_COL = "PLUGIN_NAME";
          String lPluginName = lPluginPropertyResult.getString(PLUGIN_NAME_COL);

          final String WS_ALLOWED_COL = "WS_ALLOWED";
          Boolean lWsAllowed = "true".equals(lPluginPropertyResult.getString(WS_ALLOWED_COL)) ? true : false;

          final String LOAD_ALLOWED_COL = "LOAD_ALLOWED";
          Boolean lLoadAllowed = "true".equals(lPluginPropertyResult.getString(LOAD_ALLOWED_COL)) ? true : false;

          PluginProperties lCurrentPluginProperties = new PluginProperties(lPluginName, lWsAllowed, lLoadAllowed);
          lPluginPropertyMap.put(lCurrentPluginProperties.getPluginName(), lCurrentPluginProperties);
        }
      }
      catch (ExDB e) {
        throw new ExInternal("An error occured try to acquire the plugin properties. ", e);
      }
    }
    finally {
      if (lUCon != null) {
        pContextUCon.returnUCon(lUCon, "Get plugin properties");
      }
    }

    return lPluginPropertyMap;
  }

  private PluginProperties(String pPluginName, Boolean pWsAllowed, Boolean pLoadAllowed) {
    mPluginName = pPluginName;
    mWsAllowed = pWsAllowed;
    mLoadAllowed = pLoadAllowed;
  }

  public String getPluginName() {
    return mPluginName;
  }

  public boolean isWsAllowed() {
    return mWsAllowed;
  }

  public boolean isLoadAllowed() {
    return mLoadAllowed;
  }

}
