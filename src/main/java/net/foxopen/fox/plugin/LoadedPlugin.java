package net.foxopen.fox.plugin;

import com.google.common.collect.EvictingQueue;

import java.text.SimpleDateFormat;

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Queue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.job.FoxJobPool;
import net.foxopen.fox.plugin.api.FoxPlugin;
import net.foxopen.fox.sql.SQLManager;

/**
 * Wrapper for a FoxPlugin which has been successfully loaded by the PluginManager. All access to FoxPlugin implementations
 * should be via this class to ensure the plugins have been correctly loaded and registered.
 */
public class LoadedPlugin {

  private static final String CLASS_FILE_EXT = ".class";
  public static final int LAST_MESSAGE_QUEUE_SIZE = 10;

  private final FoxPlugin mFoxPlugin;
  /** Manager context to allow the plugin to communicate with the PluginManager */
  private final PluginManagerContextImpl mManagerContext;
  private final PluginClassLoader mClassLoader;

  private final Queue<String> mLastMessages = EvictingQueue.create(LAST_MESSAGE_QUEUE_SIZE);

  /**
   * Attempts to create a LoadedPlugin containing the FoxPlugin of the given name. An error is raised if the JAR does not
   * contain an appropriate class which implements the FoxPlugin interface, or if a new FoxPlugin instance cannot be instantiated.
   * @param pExpectedPluginName
   * @param pJarFile
   * @param pPluginClassLoader
   * @return
   */
  static LoadedPlugin createLoadedPlugin(String pExpectedPluginName, JarFile pJarFile, PluginClassLoader pPluginClassLoader) {

    FoxPlugin lPlugin = null;
    Enumeration<JarEntry> lJarEntries = pJarFile.entries();

    //Load all the classes in the plugin directory within the JAR, looking for the FoxPlugin implementation
    while (lJarEntries.hasMoreElements()) {
      JarEntry lJarEntry = lJarEntries.nextElement();

      //Normalise path string
      String lJarEntryName = lJarEntry.getName().replace('\\','/');

      //Only examine .class files in the plugin package namespace
      if(lJarEntry.isDirectory() || !lJarEntryName.endsWith(CLASS_FILE_EXT) || !lJarEntryName.contains("net/foxopen/fox/plugin")){
        continue;
      }

      //Remove extension and replace "/" with "." to establish class name
      String lClassName = lJarEntryName.substring(0, lJarEntryName.length() - CLASS_FILE_EXT.length());
      lClassName = lClassName.replace('/', '.');

      Class lClass;
      try {
        lClass = pPluginClassLoader.loadClass(lClassName);
      }
      catch (ClassNotFoundException e) {
        throw new ExInternal("Failed to load class from PluginClassLoader", e);
      }

      //If this is a FoxPlugin implementor
      if(FoxPlugin.class.isAssignableFrom(lClass)) {
        //Check we've not already found a plugin
        if(lPlugin != null) {
          throw new ExInternal("More than one FoxPlugin implementation found in plugin JAR");
        }

        //Construct a new instance
        Object lNewPluginObject = null;
        try {
          lNewPluginObject = lClass.newInstance();
        }
        catch (InstantiationException | IllegalAccessException e) {
          throw new ExInternal("Failed to create a new plugin instance - ensure the plugin has a public no-args constructor", e);
        }

        lPlugin = (FoxPlugin) lNewPluginObject;

        //Check name reported by plugin matches that specified in the plugin JAR manifest
        if(!pExpectedPluginName.equals(lPlugin.getName())) {
          throw new ExInternal("Plugin name mismatch: expected " + pExpectedPluginName + " got " + lPlugin.getName());
        }
      }
    }

    //Class file loop ended without locating a plugin
    if(lPlugin == null) {
      throw new ExInternal("Failed to locate an implementation of FoxPlugin in the net/foxopen/fox/plugin package");
    }
    else {
      return new LoadedPlugin(lPlugin, pPluginClassLoader);
    }
  }

  private LoadedPlugin(FoxPlugin pFoxPlugin, PluginClassLoader pClassLoader) {
    mFoxPlugin = pFoxPlugin;
    mClassLoader = pClassLoader;

    //Tell the plugin about the manager context
    DOM lPluginConfig = getPluginConfig(pFoxPlugin.getName());
    mManagerContext = new PluginManagerContextImpl(this, lPluginConfig);
    mFoxPlugin.setManagerContext(mManagerContext);
  }

  private DOM getPluginConfig(String pPluginName) {
    final String GET_PLUGIN_CONFIG_SQL_FILENAME = "GetPluginConfig.sql";

    ContextUCon lContextUCon = ContextUCon.createContextUCon(FoxGlobals.getInstance().getEngineConnectionPoolName(), "Fox Plugin Config Context");
    try {
      lContextUCon.pushConnection("Fox Plugin Config Connection");
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":p_plugin_name", pPluginName);

      UCon lUCon = null;
      try {
        lUCon = lContextUCon.getUCon("Fox Plugin Config UCon");
        try {
          return lUCon.queryScalarDOM(SQLManager.instance().getStatement(GET_PLUGIN_CONFIG_SQL_FILENAME, LoadedPlugin.class), lBindMap);
        }
        catch (ExDBTooFew e) {
          // Do nothing, configuration is optional if there is not one there.
        }
        catch (ExDB e) {
          throw new ExInternal("An error occured try to acquire the plugin properties. ", e);
        }
      }
      finally {
        if (lUCon != null) {
          lContextUCon.returnUCon(lUCon, "Fox Plugin Config UCon");
        }
      }
    }
    finally {
      lContextUCon.popConnection("Fox Plugin Config Connection");
    }

    return null;
  }

  public FoxPlugin getPlugin() {
    return mFoxPlugin;
  }

  public String getStatusInfo() {
    return mClassLoader.getDebugInfo();
  }

  public PluginManagerContextImpl getManagerContext() {
    return mManagerContext;
  }

  /**
   * Records a message sent from the plugin.
   * @param pMessage
   */
  void logMessage(String pMessage){
    mLastMessages.add(new SimpleDateFormat(FoxJobPool.STATUS_DATE_FORMAT).format(new Date()) + " - " + pMessage);
  }

  public Collection<String> getLastLogMessages() {
    return mLastMessages;
  }
}
