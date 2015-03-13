package net.foxopen.fox.plugin;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * A file in the plugin directory which may or may not be a valid plugin file which can be loaded into a LoadedPlugin.
 * Note: a plugin file may not be valid if it is not a recongised FoxPlugin JAR file. Always check validity by calling
 * {@link #isValid} before invoking any method which does not make sense for an invalid plugin file.
 */
class PluginFile {

  private static final String FOX_PLUGIN_NAME_ATTRIBUTE_NAME = "FOX-Plugin-Name";
  private static final String FOX_PLUGIN_VERSION_ATTRIBUTE_NAME = "FOX-Plugin-Version";
  private static final String BUILD_TAG_ATTRIBUTE_NAME = "Build-Tag";
  private static final String MIN_API_VERSION_ATTRIBUTE_NAME = "Min-API-Version";

  /** File within the plugin directory */
  private final File mPluginFile;

  //Can be null if the plugin file failed at the scan phase
  private final String mPluginName;
  //Can be null if the plugin file failed at the scan phase
  private final String mPluginVersion;
  //Can be null if the plugin file failed at the scan phase
  private final String mPluginBuildTag;
  //Not null if the plugin file failed at the scan phase
  private final Throwable mScanException;
  //Not null if the plugin file failed at the load phase
  private Throwable mLoadException;

  /**
   * Creates a new PluginFile from the given file. An object is always returned regardless of the plugin's validity.
   * @param pFile File to parse as a FoxPlugin JAR.
   * @param pPluginAPIVersion The minimum API version which the plugin is expected to conform to.
   * @return New PluginFile which may or may not be valid.
   */
  static PluginFile createPluginFile(File pFile, PluginVersion pPluginAPIVersion) {

    if(pFile.isFile()) {
      Throwable lScanException = null;
      String lPluginName = null;
      String lPluginVersion = null;
      String lPluginBuildTag = null;

      try {
        //If it's a file, assume it's a jar
        String lPathToJar = pFile.getAbsolutePath();
        JarFile lPluginJar = new JarFile(lPathToJar);

        Manifest lManifest;
        try {
          lManifest = lPluginJar.getManifest();
        }
        catch (IOException e) {
          throw new ExInternal("Failed to get manifest from plugin JAR", e);
        }

        if(lManifest == null) {
          throw new ExInternal("Failed to read JAR Plugin Name from plugin file - no manifest file found in JAR");
        }

        lPluginName = lManifest.getMainAttributes().getValue(FOX_PLUGIN_NAME_ATTRIBUTE_NAME);

        if(XFUtil.isNull(lPluginName)) {
          //Check plugin name specified
          throw new ExInternal("Failed to read JAR Plugin Name from plugin file - check manifest contains " + FOX_PLUGIN_NAME_ATTRIBUTE_NAME + " attribute");
        }

        lPluginVersion = lManifest.getMainAttributes().getValue(FOX_PLUGIN_VERSION_ATTRIBUTE_NAME);
        lPluginBuildTag = lManifest.getMainAttributes().getValue(BUILD_TAG_ATTRIBUTE_NAME);

        Attributes lManifestAttrs = lManifest.getMainAttributes();

        String lPluginsMinAPIVersion = lManifestAttrs.getValue(MIN_API_VERSION_ATTRIBUTE_NAME);
        try {
          if(!XFUtil.isNull(lPluginsMinAPIVersion)) {
            PluginVersion lMinAPIVersion = new PluginVersion(lPluginsMinAPIVersion);
            if (lMinAPIVersion.isVersionCompatible(pPluginAPIVersion)) {
              throw new ExInternal("Could not load plugin - minimum API version required is " + lMinAPIVersion.getVersionString() + " but this engine only provides version " + pPluginAPIVersion.getVersionString());
            }
          }
        }
        catch (NumberFormatException e) {
          throw new ExInternal("Error loading plugin - bad version number for " + MIN_API_VERSION_ATTRIBUTE_NAME + " manifest property", e);
        }
      }
      catch(Throwable th) {
        lScanException = th;
      }

      return new PluginFile(pFile, lPluginName, lPluginVersion, lPluginBuildTag, lScanException);
    }
    else {
      throw new ExInternal("PluginFiles can only be created from files");
    }
  }

  private PluginFile(File pPluginFile, String pPluginName, String pPluginVersion, String pPluginBuildTag, Throwable pScanException) {
    mPluginFile = pPluginFile;
    mPluginName = pPluginName;
    mPluginVersion = pPluginVersion;
    mPluginBuildTag = pPluginBuildTag;
    mScanException = pScanException;
  }

  public boolean isValid() {
    return mScanException == null;
  }

  public String getFileName() {
    return mPluginFile.getName();
  }

  public File getPluginJar() {
    return mPluginFile;
  }

  public String getPluginName() {
    return mPluginName;
  }

  public String getPluginVersion() {
    return mPluginVersion;
  }

  public String getPluginBuildTag() {
    return mPluginBuildTag;
  }

  Throwable getScanException() {
    return mScanException;
  }

  Throwable getLoadException() {
    return mLoadException;
  }

  void markLoadAsFailed(Throwable pLoadFailure) {
    mLoadException = pLoadFailure;
  }

  /**
   * Loads this plugin by extracting its plugin libraries, creating a new ClassLoader and constructing a new LoadedPlugin
   * object to wrap the FoxPlugin instance.
   * @return New LoadedPlugin, or null if the load failed. Inspect {@link #getLoadException} to view the reason for this.
   */
  LoadedPlugin loadPlugin() {

    if(!isValid()) {
      throw new ExInternal("Cannot load an invalid plugin");
    }

    mLoadException = null;

    try {
      JarFile lJarFile;
      try {
        lJarFile = new JarFile(mPluginFile);
      }
      catch (IOException e) {
        throw new ExInternal("JAR problem", e);
      }

      //Extract the plugin libraries
      File lLibDir = extractPluginLibs(lJarFile);

      //Determine all URLs for classloader - the main JAR and any of its extracted libs
      List<URL> lURLs = new ArrayList<>();
      try {
        lURLs.add(new URL("jar:file:" + mPluginFile.getAbsolutePath()  +"!/"));
        for(File lLibFile : lLibDir.listFiles()) {
          lURLs.add(new URL("jar:file:" + lLibFile.getAbsolutePath()  +"!/"));
        }
      }
      catch (MalformedURLException e) {
        throw new ExInternal("URL problem", e);
      }

      //Special classes which need to be loaded here first
      //lURLs.add(getClass().getResource(PluginRequestContextWrapper.class.getSimpleName() + ".class"));

      //Create a new classloader for this plugin
      PluginClassLoader lPluginClassLoader = new PluginClassLoader(lURLs.toArray(new URL[0]), getClass().getClassLoader());

      //Load relevant classes, locate the plugin file and instantiate a new LoadedPlugin
      return LoadedPlugin.createLoadedPlugin(mPluginName, lJarFile, lPluginClassLoader);
    }
    catch (Throwable th) {
      mLoadException = th;
      return null;
    }
  }

  /**
   * Extracts the nested JARs from the plugin JAR into a sub-directory within the plugin directory. The sub-directory's
   * name is the plugin name.
   * @param pJarFile Plugin JAR to extract nested JAR libs from.
   * @return The new directory containing the extracted JARs.
   * @throws IOException
   */
  private File extractPluginLibs(JarFile pJarFile) throws IOException {

    //Create lib dir for this plugin
    File lLibDir = new File(FilenameUtils.removeExtension(mPluginFile.getAbsolutePath()));
    //Delete any existing files
    lLibDir.delete();
    //Create an empty target directory
    lLibDir.mkdir();

    //Extract nested libs to directory with name corresponding to the plugin JAR filename
    Enumeration<JarEntry> lJarEntries = pJarFile.entries();
    while (lJarEntries.hasMoreElements()) {
      JarEntry lJarEntry = lJarEntries.nextElement();
      //Normalise path strings
      String lLibFilename = lJarEntry.getName().replace('\\','/');
      //Extract files from the /lib subdirectory
      if(lLibFilename.indexOf("lib/") != -1 && !lJarEntry.isDirectory()) {

        //Create an ouput file in the external lib sub directory, with the same name as the lib file in the plugin JAR
        File lDestFile = new File(lLibDir, lLibFilename.replace("lib/", ""));
        //Copy file from JAR to external plugin lib directory
        IOUtils.copy(pJarFile.getInputStream(lJarEntry), new FileOutputStream(lDestFile));

        // Create the absolute file path to the library JAR
        String lCurrentJarName = lJarEntry.getName().replace("lib/", "");
        String lCurrentJarFilename = (lLibDir.getAbsolutePath() + File.separator + lCurrentJarName).replace('/', File.separatorChar);
        // Check for internal libraries
        checkJarForInternalLibraries(lCurrentJarFilename);
      }
    }

    return lLibDir;
  }

  /**
   * Check a JAR file file for containing internal classes, any in net/foxopen/fox.
   *
   * @param pJarPath The file path to the JAR File.
   */
  private void checkJarForInternalLibraries(String pJarPath) {

    JarFile lCurrentJarFile = null;
    // Open the JAR File
    try {
      lCurrentJarFile = new JarFile(new File(pJarPath));
    }
    catch (IOException e) {
      throw new ExInternal("Could not open an extracted fox plugin library to check for internal classes being overridden, called " + mPluginName);
    }

    // Loop over each entry checking whether its path contains foxopen
    if (lCurrentJarFile != null) {
      Enumeration<JarEntry> lCurrentJarEntries = lCurrentJarFile.entries();

      while (lCurrentJarEntries.hasMoreElements()) {
        JarEntry lCurrentJarEntry = lCurrentJarEntries.nextElement();

        if (lCurrentJarEntry.getName().contains("net/foxopen/fox")) {
          throw new ExInternal("A fox plugin jar provided contained classes from the core fox engine. Only third party classes or libraries are allowed. The path found was " + lCurrentJarEntry.getName()
          + " for plugin name " + mPluginName);
        }
      }

      // Close the jar if it has been loaded properly
      if (lCurrentJarFile != null) {
        try {
          lCurrentJarFile.close();
        }
        catch (IOException e) {
          // Ignore any closing errors
        }
      }

    }
  }
}
