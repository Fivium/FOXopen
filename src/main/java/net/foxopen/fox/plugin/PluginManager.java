package net.foxopen.fox.plugin;

import com.google.common.base.Joiner;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.MessageLevel;
import net.foxopen.fox.enginestatus.StatusAction;
import net.foxopen.fox.enginestatus.StatusCategory;
import net.foxopen.fox.enginestatus.StatusCollection;
import net.foxopen.fox.enginestatus.StatusDetail;
import net.foxopen.fox.enginestatus.StatusItem;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusTable;
import net.foxopen.fox.enginestatus.StatusText;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.ws.WebServiceServlet;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.plugin.api.CommandProvidingPlugin;
import net.foxopen.fox.plugin.api.FoxPlugin;
import net.foxopen.fox.plugin.api.PluginManagerContext;
import net.foxopen.fox.plugin.api.WebServiceProvidingPlugin;
import net.foxopen.fox.plugin.api.callout.PluginCallout;
import net.foxopen.fox.plugin.api.command.FxpCommandFactory;
import net.foxopen.fox.plugin.api.ws.FxpWebServiceEndPoint;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Singleton manager responsible for parsing the plugin directory, loading plugins and delegating plugin requests to the
 * relevant plugin. <br/><br>
 *
 * All files (not directories) in the plugin directory are treated as {@link PluginFile}s and picked up when the directory
 * is scanned. At this point they are validated to ensure they are valid JARs and contain the correct manifest markup.
 * The PluginManager then decides if the PluginFile should be loaded, if it is valid. Loading a plugin involves creating a
 * {@link LoadedPlugin} from a PluginFile. <br/><br>
 *
 * The PluginManager tracks the states of these objects so they can be clearly reported for diagnostic info in the !STATUS screens.
 */
public class PluginManager {

  private static final PluginManager INSTANCE = new PluginManager();

  private static final String API_VERSION_PROPERTY_NAME = "plugin-api.version";

  /** All plugin files located in the latest scan */
  private final Set<PluginFile> mPluginFiles = new HashSet<>();

  /** Plugin names to loaded instances. */
  private final Map<String, LoadedPlugin> mLoadedPlugins = new HashMap<>();

  /** Plugin names to loaded command plugins. */
  private final Map<String, CommandProvidingPlugin> mCommandPlugins = new HashMap<>();

  /** Plugin names to loaded web service plugins. */
  private final Map<String, WebServiceProvidingPlugin> mWebServicePlugins = new HashMap<>();

  /** Plugin names to their configuration on the database */
  private Map<String, PluginProperties> mPluginProperties = new HashMap<>();

  // These are replaced every scan
  private String mLastScanLog;
  private Throwable mLastScanException;

  private final StatusCategory mPluginStatusCategory = EngineStatus.instance().registerStatusProvider(new PluginStatusProvider());

  // Read from properties file on load
  private PluginVersion mPluginAPIVersion = null;

  public static PluginManager instance() {
    return INSTANCE;
  }

  private PluginManager() {

    try {
      mPluginAPIVersion = getPluginAPIVersion();
    }
    catch (Throwable th) {
      mPluginStatusCategory.addMessage("Plugin API Version", "API Version could not be read, no plugins will be loaded: " + th.getMessage(), MessageLevel.ERROR);
    };

    mPluginStatusCategory.addMessage("Plugin API Version", "API Version " + mPluginAPIVersion.getVersionString(), MessageLevel.INFO);

    WebServiceServlet.registerWebServiceCategory(new PluginWebServiceCategory(this));
  }

  private Boolean isPluginLoadAllowed(String pPluginName) {
    PluginProperties lPluginProperties = mPluginProperties.get(pPluginName);
    if (lPluginProperties != null) {
      return lPluginProperties.isLoadAllowed();
    }
    else {
      //No plugin config defined; assume the plugin should be enabled by default
      return true;
    }
  }

  private boolean isPluginWSAllowed(String pPluginName) {
    PluginProperties lPluginProperties = mPluginProperties.get(pPluginName);
    if (lPluginProperties != null) {
      return lPluginProperties.isWsAllowed();
    }
    else {
      //No plugin config defined; assume the plugin should be enabled by default
     return true;
    }
  }

  private boolean isPluginLoaded(String pPluginName) {
    return mLoadedPlugins.containsKey(pPluginName);
  }

  private PluginVersion getPluginAPIVersion() {
    Properties lVersionProperties = new Properties();
    try(InputStream lPropFile = getClass().getResourceAsStream("api/version.properties")) {
      lVersionProperties.load(lPropFile);
    }
    catch (IOException e) {
      throw new ExInternal("Failed to read API version property file");
    }

    String lVersionString = lVersionProperties.getProperty(API_VERSION_PROPERTY_NAME);

    if (XFUtil.isNull(lVersionString)) {
      throw new ExInternal("Plugin API version property file missing mandatory " + API_VERSION_PROPERTY_NAME + " property");
    }

    return new PluginVersion(lVersionString);
  }

  private void loadPluginProperties(ContextUCon pContextUCon) {
    mPluginProperties = PluginProperties.createAllPluginProperties(pContextUCon);
  }

  /**
   * Re-scans the plugin directory for new plugins. Currently removed plugins remain loaded and must be manually stopped.
   */
  public synchronized void scanAndLoadPlugins() {
    // Rescan plugin directory
    scanPluginDirectory();

    // Do nothing if trying to load and the engine is not configured
    if (!FoxGlobals.getInstance().isEngineInitialised()) {
      return;
    }

    // Grab the set up connection and load the plugin properties from the database
    ContextUCon lContextUCon = ContextUCon.createContextUCon(FoxGlobals.getInstance().getEngineConnectionPoolName(), "Fox Plugin Properties Context");
    try {
      lContextUCon.pushConnection("Fox Plugin Properties UCon");
      loadPluginProperties(lContextUCon);
    }
    finally {
      lContextUCon.popConnection("Fox Plugin Properties UCon");
    }

    for(PluginFile lPluginFile : mPluginFiles) {
      loadPluginInternal(lPluginFile);
    }
  }

  /**
   * Scan WEB-INF/plugins for plugin jar files and read them into a list
   */
  public void scanPluginDirectory() {

    if(mPluginAPIVersion == null) {
      throw new ExInternal("Cannot load plugins as engine plugin API version is not known");
    }

    //Note this method should not throw exceptions, they should be caught and logged so they can be reported on the status page
    StringBuilder lScanLog = new StringBuilder();

    mPluginFiles.clear();
    mLastScanException = null;

    String lPluginDirPath = FoxGlobals.getInstance().getServletContext().getRealPath("").replace('\\','/') + "/WEB-INF/plugins";
    lScanLog.append("Scanning directory ");
    lScanLog.append(lPluginDirPath);
    lScanLog.append("\n");

    File lPluginDir = new File(lPluginDirPath);

    //All plugin files with the extension removed from the filename
    Set<String> lPluginDirectoryFilenames = new HashSet<>();
    //Any directories within the plugin directory - these may need deleting at end of scan
    Set<File> lDirectories = new HashSet<>();

    try {
      if(!lPluginDir.exists()) {
        throw new ExInternal("Plugin directory could not be located at " + lPluginDirPath);
      }
      else if(!lPluginDir.isDirectory()) {
        throw new ExInternal("File at plugin path " + lPluginDirPath + " is not a directory");
      }
      else {
        Set<String> lFoundPluginNames = new HashSet<>();

        JAR_LOOP:
        for(File lFile : lPluginDir.listFiles()) {
          if(lFile.isFile()) {

            PluginFile lPluginFile = PluginFile.createPluginFile(lFile, mPluginAPIVersion);
            mPluginFiles.add(lPluginFile);
            //Record the directory filename expected to be associated with this plugin
            lPluginDirectoryFilenames.add(FilenameUtils.removeExtension(lFile.getName()));

            if(lPluginFile.isValid()) {
              if (lFoundPluginNames.contains(lPluginFile.getPluginName())) {
                //Check for duplicate plugin names
                throw new ExInternal("File " + lFile.getAbsolutePath() + " contains duplicate plugin definition for plugin" + lPluginFile.getPluginName());
              }
              else {
                lFoundPluginNames.add(lPluginFile.getPluginName());
              }
            }
          }
          else if (lFile.isDirectory()) {
            //Record directory name - may need to be deleted
            lDirectories.add(lFile);
          }
        }
      }

      //Clean up directories for plugin files which have been removed
      for(File lDir : lDirectories) {
        String lDirName = lDir.getName();
        if(!lPluginDirectoryFilenames.contains(lDirName)) {
          lScanLog.append("Deleting directory ");
          lScanLog.append(lDirName);
          lScanLog.append(" as no corresponding plugin could be found\n");
          try {
            FileUtils.deleteDirectory(lDir);
          }
          catch (IOException e) {
            //Not an error which should prevent all plugins from loading
            lScanLog.append("Deleting directory ");
            lScanLog.append(lDirName);
            lScanLog.append(" failed - ");
            lScanLog.append(e.getMessage());
            lScanLog.append("\n");
          }
        }
      }
    }
    catch (Throwable th) {
      mLastScanException = th;
    }

    mLastScanLog = lScanLog.toString();
  }

  public synchronized void loadPlugin(String pPluginName) {
    for(PluginFile lPluginFile : mPluginFiles) {
      if(lPluginFile.isValid() && pPluginName.equals(lPluginFile.getPluginName())) {
        loadPluginInternal(lPluginFile);
      }
    }
  }

  public synchronized void reloadPlugin(String pPluginName) {
    unloadPlugin(pPluginName);
    loadPlugin(pPluginName);
  }

  public List<String> getPluginFileNames() {
    List<String> lPluginNames = new ArrayList<>();
    for (PluginFile lCurrentPlugin : mPluginFiles) {
       lPluginNames.add(lCurrentPlugin.getPluginName());
    }
    return lPluginNames;
  }

  public void unloadPlugin(String pPluginName) {
    mLoadedPlugins.remove(pPluginName);
    mCommandPlugins.remove(pPluginName);
    mWebServicePlugins.remove(pPluginName);
  }

  private void loadPluginInternal(PluginFile pPluginFile ) {
    try {
      if(isPluginLoadAllowed(pPluginFile.getPluginName()) && !isPluginLoaded(pPluginFile.getPluginName())) {
        LoadedPlugin lLoadedPlugin = pPluginFile.loadPlugin();

        //Only attempt further loading if the initial load succeeded
        if(lLoadedPlugin != null) {
          //If this is a command plugin attempt to register it as one
          registerCommandPlugin(lLoadedPlugin);

          //If this is a web service plugin attempt to register it as one
          registerWebServicePlugin(lLoadedPlugin);

          mLoadedPlugins.put(pPluginFile.getPluginName(), lLoadedPlugin);
        }
      }
    }
    catch (Throwable th) {
      //Make sure there are no references to the failed load
      unloadPlugin(pPluginFile.getPluginName());
      pPluginFile.markLoadAsFailed(th);
    }
  }

  private static Set<String> allCommandAliasesForPlugin(CommandProvidingPlugin lCommandPlugin) {
    Set<String> lCommandAliases = new HashSet<>();
    for(FxpCommandFactory lCommandFactory : lCommandPlugin.getCommandFactories()) {
      lCommandAliases.addAll(lCommandFactory.getCommandElementNames());
    }
    return lCommandAliases;
  }

  /**
   * Attempts to add the given plugin to the map of known CommandProvidingPlugins, if appropriate. Validation is performed
   * to ensure that a command alias can only ever be resolved by one plugin.
   * @param pLoadedPlugin
   */
  private void registerCommandPlugin(LoadedPlugin pLoadedPlugin) {
    FoxPlugin lFoxPlugin = pLoadedPlugin.getPlugin();
    if(lFoxPlugin instanceof CommandProvidingPlugin) {
      CommandProvidingPlugin lCommandPlugin = (CommandProvidingPlugin) lFoxPlugin;

      Set<String> lCommandAliases = allCommandAliasesForPlugin(lCommandPlugin);

      for(CommandProvidingPlugin lExistingCommandPlugin : mCommandPlugins.values()) {
        for(String lExistingCommandAlias : allCommandAliasesForPlugin(lExistingCommandPlugin)) {
          //Check the commands being provided are unique to this plugin
          if(lCommandAliases.contains(lExistingCommandAlias)) {
            throw new ExInternal("Command " + lExistingCommandAlias + " is alredy defined in plugin " + lExistingCommandPlugin.getName());
          }
        }
      }

      mCommandPlugins.put(lFoxPlugin.getName(), lCommandPlugin);
    }
  }

  private void registerWebServicePlugin(LoadedPlugin pLoadedPlugin) {
    FoxPlugin lFoxPlugin = pLoadedPlugin.getPlugin();
    if(lFoxPlugin instanceof WebServiceProvidingPlugin && isPluginWSAllowed(lFoxPlugin.getName())) {
      WebServiceProvidingPlugin lWSPlugin = (WebServiceProvidingPlugin) lFoxPlugin;

      //Check the WS name isn't duplicated by an existing plugin
      for(WebServiceProvidingPlugin lExistingWSPlugin : mWebServicePlugins.values()) {
        if(lWSPlugin.getWebServiceName().equals(lExistingWSPlugin.getWebServiceName())) {
          throw new ExInternal("WebService with name " + lWSPlugin.getWebServiceName() + " is already defined in plugin " + lExistingWSPlugin.getName());
        }
      }

      //Check there are no duplicate end points in the plugin
      Set<String> lPluginEndPoints = new HashSet<>();
      for(FxpWebServiceEndPoint lEndPoint : lWSPlugin.getAllEndPoints()) {
        if(lPluginEndPoints.contains(lEndPoint.getName())) {
          throw new ExInternal("WebService EndPoint " + lPluginEndPoints + " is duplicated by plugin web service provider");
        }
        else if(XFUtil.isNull(lEndPoint.getName())) {
          throw new ExInternal("WebService is attempting to provide an EndPoint without a name");
        }

        lPluginEndPoints.add(lEndPoint.getName());
      }

      mWebServicePlugins.put(lFoxPlugin.getName(), lWSPlugin);
    }
  }

  public PluginManagerContext getLoadedPluginManagerContext(String pPluginName) {
    return getLoadedPlugin(pPluginName).getManagerContext();
  }

  private LoadedPlugin getLoadedPlugin(String pPluginName) {
    if(!isPluginLoaded(pPluginName)) {
      throw new ExInternal("Plugin " + pPluginName + " not loaded");
    }
    return mLoadedPlugins.get(pPluginName);
  }

  public void processCallout(PluginCallout pCallout) {

    //Copy the currently loaded plugins sideways to avoid concurrent modification problems
    //Can't sync this whole method as it would block other callouts in the event of a long-running processCallout
    Set<LoadedPlugin> lLoadedPlugins = new HashSet<>();
    synchronized(this) {
      lLoadedPlugins.addAll(mLoadedPlugins.values());
    }

    for(LoadedPlugin lLoadedPlugin : lLoadedPlugins) {
      FoxPlugin lPlugin = lLoadedPlugin.getPlugin();
      if(lPlugin.canHandleCallout(pCallout)) {
        try {
          lPlugin.processCallout(pCallout);
        }
        catch (Throwable th) {
          throw new ExInternal("Plugin " + lPlugin.getName() + " failed to process callout", th);
        }

        return;
      }
    }

    throw new ExInternal("Failed to find a plugin to handle callout type " + pCallout.getClass().getName());
  }

  public synchronized Collection<FxpCommandFactory> getAllCommandFactories() {

    Set<FxpCommandFactory> lAllFactories = new HashSet<>();

    for(CommandProvidingPlugin lCommandProvider : mCommandPlugins.values()) {
      for(FxpCommandFactory lFactory : lCommandProvider.getCommandFactories()) {
        lAllFactories.add(lFactory);
      }
    }

    return lAllFactories;
  }

  public synchronized Collection<WebServiceProvidingPlugin> getAllWebServiceProviders() {
    return Collections.unmodifiableCollection(mWebServicePlugins.values());
  }

  /**
   * Attempts to construct a new command using the given name and markup DOM, by delegating to the approrptiate command
   * providing plugin.
   * @param pCommandName Name/alias of command to search for.
   * @param pMarkupDOM Module command markup.
   * @return New command or null if command cannot be resolved.
   */
  public synchronized Command resolvePluginCommand(String pCommandName, DOM pMarkupDOM) {

    for(CommandProvidingPlugin lCommandPlugin : mCommandPlugins.values()) {
      if(allCommandAliasesForPlugin(lCommandPlugin).contains(pCommandName)) {
        return new PluginCommandWrapper(pMarkupDOM, lCommandPlugin.getName());
      }
    }

    //Command provider not found
    return null;
  }

  synchronized CommandProvidingPlugin resolveCommandProvidingPlugin(String pPluginName) {
    CommandProvidingPlugin lPlugin = mCommandPlugins.get(pPluginName);
    if(lPlugin == null) {
      throw new ExInternal("Plugin " + pPluginName + " is not available");
    }
    return lPlugin;
  }

  private class PluginStatusProvider
  implements StatusProvider {

    @Override
    public void refreshStatus(StatusCategory pCategory) {

      synchronized(PluginManager.this) {
        pCategory.addMessage("Directory Scan Result", mLastScanLog);

        if(mLastScanException != null) {
          pCategory.addMessage("Directory Scan Exception", XFUtil.getJavaStackTraceInfo(mLastScanException), MessageLevel.ERROR);
        }
        else {
          pCategory.addMessage("Directory Scan Exception", "", MessageLevel.INFO);
        }

        final String FILE_NAME_COLUMN = "File Name";
        final String STATUS_COLUMN = "Status";
        final String PLUGIN_NAME_COLUMN = "Plugin Name";
        final String INFO_COLUMN = "Info";
        final String ACTION_COLUMN = "Actions";

        StatusTable lStatusTable = pCategory.addTable("Plugin Info", FILE_NAME_COLUMN, STATUS_COLUMN, PLUGIN_NAME_COLUMN, INFO_COLUMN, ACTION_COLUMN);
        lStatusTable.setRowProvider(new StatusTable.RowProvider() {
          @Override
          public void generateRows(StatusTable.RowDestination pRowDestination) {
            for(PluginFile lPluginFile : mPluginFiles) {

              StatusTable.Row lStatusRow = pRowDestination.addRow(lPluginFile.getFileName());
              StatusCollection lActions = new StatusCollection("actions");

              lStatusRow.setColumn(FILE_NAME_COLUMN, lPluginFile.getFileName());

              if(!lPluginFile.isValid()) {
                lStatusRow
                  .setColumn(STATUS_COLUMN, "INVALID", MessageLevel.ERROR)
                  .setColumn(PLUGIN_NAME_COLUMN, "Unknown")
                  .setColumn(INFO_COLUMN, new StatusDetail("View scan exception details", XFUtil.getJavaStackTraceInfo(lPluginFile.getScanException())));
              }
              else {
                String lPluginName = lPluginFile.getPluginName();
                lStatusRow.setColumn(PLUGIN_NAME_COLUMN, lPluginName);

                if(!isPluginLoaded(lPluginName)) {

                  Throwable lLoadException = lPluginFile.getLoadException();
                  if(lLoadException != null) {
                    lStatusRow.setColumn(INFO_COLUMN, new StatusDetail("View load exception details", XFUtil.getJavaStackTraceInfo(lLoadException)));
                    lStatusRow.setColumn(STATUS_COLUMN, "LOAD ERROR", MessageLevel.ERROR);
                  }
                  else {
                    lStatusRow.setColumn(STATUS_COLUMN, "NOT LOADED", MessageLevel.WARNING);
                  }
                }
                else {
                  //Plugin is loaded
                  final LoadedPlugin lLoadedPlugin = mLoadedPlugins.get(lPluginName);
                  lStatusRow.setColumn(STATUS_COLUMN, "LOADED");

                  StatusCollection lMessages = new StatusCollection("lastMessages")
                    .addItem(new StatusDetail("Last " + LoadedPlugin.LAST_MESSAGE_QUEUE_SIZE + " messages", Joiner.on("\n").join(lLoadedPlugin.getLastLogMessages())))
                    .addItem(new StatusDetail("Loaded classes", new StatusDetail.Provider() {
                      @Override
                      public StatusItem getDetailMessage() {
                        return new StatusText(lLoadedPlugin.getStatusInfo());
                      }
                    }));

                  lStatusRow.setColumn(INFO_COLUMN, lMessages);

                  lActions.addItem(new StatusAction("Unload", PluginBangHandler.instance(), Collections.singletonMap(PluginBangHandler.UNLOAD_PARAM_NAME, lPluginName)));
                }

                lActions.addItem(new StatusAction("Reload", PluginBangHandler.instance(), Collections.singletonMap(PluginBangHandler.RELOAD_PARAM_NAME, lPluginName)));
              }

              lStatusRow.setColumn(ACTION_COLUMN, lActions);
            }
          }
        });

        pCategory.addAction("Re-scan Plugin Directory", PluginBangHandler.instance());
      }
    }

    @Override
    public String getCategoryTitle() {
      return "Plugins";
    }

    @Override
    public String getCategoryMnemonic() {
      return "plugins";
    }

    @Override
    public boolean isCategoryExpandedByDefault() {
      return true;
    }
  }
}
