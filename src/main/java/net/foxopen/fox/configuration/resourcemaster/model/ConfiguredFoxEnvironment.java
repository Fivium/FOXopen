package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.App;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.FileUploadType;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxApplicationDefinition;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentDefinition;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentProperty;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.sql.SQLManager;

import java.io.File;
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Holds all information to get and use a resource master including a list of apps. The app list holds
 * all app information and processing.
 */
public class ConfiguredFoxEnvironment
implements FoxEnvironment {

  private final String mEnvironmentKey;

  /** A list of available applications for this resource master. */
  private Map<String, App> mMnemToApp = new HashMap<>();
  private Map<String, App> mMnemOrAliasToApp = new HashMap<>();

  private final FoxEnvironmentDefinition mFoxEnvironmentDefinition;

  private final String mDefaultApplication;
  private final String mCookieDomainMethod;
  private final DatabaseProperties mDatabaseProperties;
  private final AuthenticationProperties mAuthenticationProperties;
  private final FileServiceProperties mFileServiceProperties;
  private final Map<String, String> mEnvDisplayAttributeList;

  static {
    EngineStatus.instance().registerStatusProvider(new EnvironmentConfigStatusProvider());
  }

  /**
   * Grab and parse the resource master from the database.
   *
   * @return A resource master object version of the XML.
   * @throws ExServiceUnavailable Database error.
   */
  public static final ConfiguredFoxEnvironment createFoxEnvironment(ContextUCon pContextUCon, String pEnvironmentKey, FoxEnvironmentDefinition pFoxEnvironmentDefinition)
  throws ExServiceUnavailable, ExApp, ExFoxConfiguration {
    return new ConfiguredFoxEnvironment(pContextUCon, pEnvironmentKey, pFoxEnvironmentDefinition);
  }

  public Map<String, String> getEnvDisplayAttributeList() {
    return mEnvDisplayAttributeList;
  }

  private ConfiguredFoxEnvironment(ContextUCon pContextUCon, String pEnvironmentKey, FoxEnvironmentDefinition pFoxEnvironmentDefinition) throws ExApp {
    mEnvironmentKey = pEnvironmentKey;

    try {
      mFileServiceProperties = FileServiceProperties.createFileServiceProperties(pFoxEnvironmentDefinition);
      mDatabaseProperties = DatabaseProperties.createDatabaseProperties(pFoxEnvironmentDefinition);
      mDefaultApplication = pFoxEnvironmentDefinition.getPropertyAsString(FoxEnvironmentProperty.DEFAULT_APPLICATION);
      mCookieDomainMethod = pFoxEnvironmentDefinition.getPropertyAsString(FoxEnvironmentProperty.COOKIE_DOMAIN_METHOD);
      mAuthenticationProperties = new AuthenticationProperties(pFoxEnvironmentDefinition.getPropertyAsDOM(FoxEnvironmentProperty.AUTHENTICATION_PROPERTIES));

      // Load DOM properties
      DOM lDisplayAttrDOM = pFoxEnvironmentDefinition.getPropertyAsDOM(FoxEnvironmentProperty.ENV_DISPLAY_ATTR_LIST);
      if (lDisplayAttrDOM != null) {
        mEnvDisplayAttributeList = Collections.unmodifiableMap(loadDomPropertyIntoStringMap(lDisplayAttrDOM, FoxEnvironmentProperty.ENV_DISPLAY_ATTR_LIST.getPath() + "/*", "name"));
      }
      else {
        mEnvDisplayAttributeList = Collections.emptyMap();
      }
    }
    catch (ExApp e) {
      throw new ExApp("An error occured in creating the Fox Environment before construction of apps.", e);
    }

    // Load mime-types.properties file (optionally augmented from database) - must be done before app config as FUTs require these for validation
    Reader lClobReader = getClobAuxiliaryConfigOrNull(pContextUCon, FileUploadType.MIME_TYPE_AUX_CONFIG_MNEM);
    FileUploadType.init(new File(FoxGlobals.getInstance().getServletContext().getRealPath("/WEB-INF/config/mime-types.properties")), lClobReader);

    Map<String, FoxApplicationDefinition> lAppDefinitionMap = pFoxEnvironmentDefinition.getAppMnemToAppDefinition();

    for (Entry<String, FoxApplicationDefinition> lAppMnemToAppDefinition : lAppDefinitionMap.entrySet()) {
      String lAppMnem = lAppMnemToAppDefinition.getKey();
      FoxApplicationDefinition lAppDefinition = lAppMnemToAppDefinition.getValue();
      App lApp;
      try {
        lApp = App.createApp(lAppMnem, lAppDefinition, this);
      }
      catch (ExFoxConfiguration | ExServiceUnavailable e) {
        throw new ExApp("An error occured when creating App '" + lAppMnem + "'", e);
      }
      for (String lAppAlias : lApp.getAppAliasList()) {
        if (mMnemOrAliasToApp.containsKey(lAppAlias)) {
          throw new ExApp("App mnem alias has already been mapped. Attempted to map  '" + lAppAlias + "' to '" + lApp.getAppMnem() + "' but it was already mapped to '" + mMnemOrAliasToApp.get(lAppAlias).getAppMnem() + "'.");
        }
        else {
          mMnemOrAliasToApp.put(lAppAlias, lApp);
        }
      }

      if (mMnemOrAliasToApp.containsKey(lAppMnem)) {
        throw new ExApp("App mnem has already been mapped. Attempted to map  '" + lAppMnem + "' to '" + lApp.getAppMnem() + "' but it was already mapped to '" + mMnemOrAliasToApp.get(lAppMnem).getAppMnem() + "'.");
      }
      else {
        mMnemToApp.put(lAppMnem, lApp);
        mMnemOrAliasToApp.put(lAppMnem, lApp);
      }
    }

    mFoxEnvironmentDefinition = pFoxEnvironmentDefinition;
  }

  // Presumes attribute is the key and the text node is value
  private static Map<String, String> loadDomPropertyIntoStringMap(DOM pDOMProperty, String pListPath, String pNameAttribute) throws ExApp {
    DOMList lDomPropertyList = pDOMProperty.getUL(pListPath);
    if (lDomPropertyList == null) {
      throw new ExApp("A property list was found to be null for list path and attribute: " + pListPath + " , " + pNameAttribute);
    }

    Map<String, String> lAttributeMap = new HashMap<>();
    for (int i = 0; i < lDomPropertyList.getLength(); i++) {
      DOM lDisplayAttr = lDomPropertyList.item(i);
      String lName = lDisplayAttr.getAttr(pNameAttribute);
      String lValue = lDisplayAttr.value();
      lAttributeMap.put(lName, lValue);
    }

    return lAttributeMap;
  }

  @Override
  public FileServiceProperties getFileServiceProperties() {
    return mFileServiceProperties;
  }

  @Override
  public String getCookieDomainMethod() {
    return mCookieDomainMethod;
  }

  @Override
  public String getDefaultAppMnem() {
    return mDefaultApplication;
  }

  @Override
  public App getDefaultApp() throws ExApp {
    App lApp = mMnemOrAliasToApp.get(mDefaultApplication);

    if (lApp == null) {
      throw new ExApp("Default application '" + mDefaultApplication + "' has not been initialised");
    }

    return lApp;
  }

  @Override
  public Collection<App> getAllApps() {
    return Collections.unmodifiableCollection(mMnemToApp.values());
  }

  // ExServiceUnavailable when "application index" or "app's module loader" connection fails
  // ExApp when application mnenonic cannot be looked up
  @Override
  public App getAppByMnem(String pAppMnem) throws
  ExServiceUnavailable, ExApp, ExInternal
  {
    return getAppByMnem(pAppMnem, false);
  }

  @Override
  public boolean isValidAppMnem(String pAppMnem) {
    if (!XFUtil.isNull(pAppMnem)) {
      return mMnemOrAliasToApp.containsKey(pAppMnem);
    } else {
      throw new ExInternal("An app mnem was provided as null when checking if it is a valid.");
    }
  }

  // ExServiceUnavailable when "application index" or "app's module loader" connection fails
  // ExApp when application mnenonic cannot be looked up
  @Override
  public App getAppByMnem(String pAppMnem, boolean pReturnDefaultIfNotKnown) throws ExServiceUnavailable, ExApp, ExInternal {
    App lApp;
    synchronized (mMnemOrAliasToApp) {
      if (pAppMnem == null || !mMnemOrAliasToApp.containsKey(pAppMnem)) {
        if (pReturnDefaultIfNotKnown) {
          lApp = mMnemOrAliasToApp.get(mDefaultApplication);
        }
        else {
          throw new ExApp("Application '" + pAppMnem + "' not known");
        }
      }
      else {
        lApp = mMnemOrAliasToApp.get(pAppMnem);
      }
    }

    // If the acquired App was null throw error
    if (lApp == null) {
      throw new ExApp("Application '" + pAppMnem + "' has not been initialised");
    }

    return lApp;
  }

  /** Flushes internal Application Cache and nested Conponents (modules)
   *   so they get read in again on next URL
   */
  @Override
  public final void flushApplicationCache() throws ExApp, ExFoxConfiguration, ExServiceUnavailable {
    synchronized (mMnemToApp) {
      ContextUCon lContextUCon = null;
      try {
        lContextUCon = ContextUCon.createContextUCon(FoxGlobals.getInstance().getEngineConnectionPoolName(), "Main Fox ContextUCon");
        lContextUCon.pushConnection("Flush Application");
        UCon lPrimaryDBUCon = null;
        try {
          lPrimaryDBUCon = lContextUCon.getUCon("Fox Primary UCon");
          Map<String, App> lAppMap = new HashMap<>();
          Map<String, App> lAppOrAliasMap = new HashMap<>();
          for (String lAppMnem : mMnemToApp.keySet()) {
            FoxApplicationDefinition lAppDefinition = FoxApplicationDefinition.createAppDefinition(lPrimaryDBUCon, FoxGlobals.getInstance().getFoxBootConfig().getFoxEnvironmentKey(), lAppMnem);
            App lApp = App.createApp(lAppMnem, lAppDefinition, FoxGlobals.getInstance().getFoxEnvironment());
            lAppMap.put(lAppMnem, lApp);
            lAppOrAliasMap.put(lAppMnem, lApp);
            for(String lAppAlias : lApp.getAppAliasList()) {
              lAppOrAliasMap.put(lAppAlias, lApp);
            }

            //Flush all objects from the App's associated object cache
            CacheManager.flushMemberCache(BuiltInCacheDefinition.APP_COMPONENTS, lAppMnem);
          }

          mMnemToApp = lAppMap;
          mMnemOrAliasToApp = lAppOrAliasMap;
        }
        finally {
          if (lPrimaryDBUCon != null) {
            lContextUCon.returnUCon(lPrimaryDBUCon, "Fox Primary UCon");
          }
        }
      }
      finally {
        if (lContextUCon != null) {
          lContextUCon.popConnection("Flush Application");
        }
      }
    }
  }

  @Override
  public FoxEnvironmentDefinition getEnvironmentDefinition() {
    return mFoxEnvironmentDefinition;
  }

  @Override
  public Reader getClobAuxiliaryConfigOrNull(ContextUCon pContextUCon, String pConfigMnem) {

    UCon lUCon = pContextUCon.getUCon("GetAuxConfig");
    try {
      try {
        UConBindMap lBindMap = new UConBindMap()
          .defineBind("environment_key", mEnvironmentKey)
          .defineBind("config_mnem", pConfigMnem);

        UConStatementResult lResult = lUCon.querySingleRow(SQLManager.instance().getStatement("GetEnvironmentAuxiliaryConfig.sql", getClass()), lBindMap);
        Clob lClob = lResult.getClob("CLOB_DATA");
        if(lClob != null) {
          return lClob.getCharacterStream();
        }
        else {
          throw new ExInternal("CLOB_DATA column cannot be null for retrival of config " + pConfigMnem);
        }
      }
      catch (ExDBTooFew e) {
        //This is OK
        return null;
      }
      catch (ExDB | SQLException e) {
        throw new ExInternal("Failed to retrieve " + pConfigMnem + " aux config", e);
      }
    }
    finally {
      pContextUCon.returnUCon(lUCon, "GetAuxConfig");
    }
  }

  @Override
  public DatabaseProperties getDatabaseProperties() {
    return mDatabaseProperties;
  }

  @Override
  public AuthenticationProperties getAuthenticationProperties() {
    return mAuthenticationProperties;
  }

}
