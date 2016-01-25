package net.foxopen.fox.entrypoint;

import net.foxopen.fox.boot.EngineInitialisationController;
import net.foxopen.fox.configuration.FoxBootConfig;
import net.foxopen.fox.configuration.UnconfiguredFoxBootConfig;
import net.foxopen.fox.configuration.resourcemaster.model.FoxEnvironment;
import net.foxopen.fox.configuration.resourcemaster.model.UnconfiguredFoxEnvironment;
import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.ConnectionPoolConfig;
import net.foxopen.fox.dom.xpath.FoxXPathEvaluator;
import net.foxopen.fox.dom.xpath.FoxXPathEvaluatorFactory;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.util.RandomString;

import javax.servlet.ServletContext;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.jar.Manifest;


/**
 * Store static constants for use throughout the rest of the engine
 * TODO - Think about concurrency here, needs some syncing, interfacing or something
 */
public class FoxGlobals {
  private static final FoxGlobals INSTANCE = new FoxGlobals();

  public static FoxGlobals getInstance() {
    return INSTANCE;
  }

  private static final int SECURITY_TOKEN_LENGTH = 128;

  /** If true, XPaths will be evaluated with XPath 1.0 backwards compatibility mode switched ON. */
  public static final boolean gXPathBackwardsCompatibility = false; //PN switched this OFF 10/07/14 - was causing the integer to double casting bug

  /** Tracks if this class has been initialised */
  private boolean mGlobalsInitialised = false;

  /** Version info as read from version.xml if available (or an "unknown" instance if not) */
  private EngineVersionInfo mEngineVersionInfo;

  /** Servlet context path with leading slash removed e.g. "englocal" */
  private String mContextName;

  /** Resolved hostname of the current server. This is set JIT on first access. */
  private String mServerHostName;

  private String mServerIP;

  /** Servlet context captured at engine boot time. */
  private ServletContext mServletContext;

  private FoxEnvironment mFoxEnvironment = new UnconfiguredFoxEnvironment();
  private FoxBootConfig mFoxBootConfig = new UnconfiguredFoxBootConfig();

  private String mBootFilePath;

  private final String mEngineSecurityToken = RandomString.getString(SECURITY_TOKEN_LENGTH);

  private final FoxXPathEvaluator mFoxXPathEvaluator = FoxXPathEvaluatorFactory.createEvaluator(gXPathBackwardsCompatibility);

  private FoxGlobals() {
  }

  public synchronized void initialise(ServletContext pServletContext) {
    if(!mGlobalsInitialised) {
      this.mServletContext = pServletContext;

      mContextName = pServletContext.getContextPath();
      if (mContextName.charAt(0) == '/') {
        mContextName = mContextName.substring(1);
      }

      // Use the Manifest file to populate the server version number and build information
      File lManifestFile = new File(pServletContext.getRealPath("/META-INF/MANIFEST.MF"));
      if (lManifestFile.exists()) {
        try {
          Manifest lManifest = new Manifest(lManifestFile.toURI().toURL().openStream());
          mEngineVersionInfo = EngineVersionInfo.fromManifest(lManifest);
          FoxLogger.getLogger().info("Server version set from manifest file: {}", mEngineVersionInfo.getVersionNumber());
        }
        catch (Throwable th) {
          FoxLogger.getLogger().error("Server manifest reading failed", th);
          mEngineVersionInfo = EngineVersionInfo.unknownInstance();
        }
      }
      else {
        FoxLogger.getLogger().warn("Server manifest file missing");
        mEngineVersionInfo = EngineVersionInfo.unknownInstance();
      }

      mGlobalsInitialised = true;
    }
    else {
      throw new ExInternal("FoxGlobals can only be initialised once");
    }
  }

  public synchronized void initialiseEngineConnectionPool() {
    ConnectionPoolConfig lConConfig = new ConnectionPoolConfig(getEngineConnectionPoolName(), getFoxBootConfig().getDatabaseURL(), getFoxBootConfig().getMainDatabaseUsername(), getFoxBootConfig().getDbPassword());
    lConConfig.setMinPoolSize(1);
    lConConfig.setMaxPoolSize(3);
    ConnectionAgent.shutdownPoolIfExists(lConConfig.getPoolName());
    ConnectionAgent.registerPool(lConConfig);
  }

  /**
   * Gets the ServletContext from the current container (i.e. Tomcat) as captured at boot time.
   * @return
   */
  public ServletContext getServletContext() {
    if (mServletContext == null) {
      throw new ExInternal("Cannot get a reference to ServletContext as it has not been set yet");
    }
    return mServletContext;
  }

  /**
   * Get the context path for this instance of the FOX engine, without the leading slash. For instance for an engine
   * deployed to "/englocal" this will return "englocal".<br><br>
   *
   * Do NOT use this to construct request URIs - entry URIs may be different to the internal context name due to rewrite
   * rules etc. URIs should always be constructed based on a request - see {@link net.foxopen.fox.entrypoint.uri.RequestURIBuilder}.
   * @return Context name
   */
  public String getContextName() {
    return mContextName;
  }

  /**
   * Gets the context path for this instance of the FOX engine, including the leading slash, e.g. "/englocal".<br><br>
   *
   * Do NOT use this to construct request URIs - entry URIs may be different to the internal context name due to rewrite
   * rules etc. URIs should always be constructed based on a request - see {@link net.foxopen.fox.entrypoint.uri.RequestURIBuilder}.
   * @return Context path
   */
  public String getContextPath() {
    return "/" + mContextName;
  }

  /**
   * Set a reference to the FoxEnvironment object
   *
   * @param pFoxEnvironment
   */
  public void setFoxEnvironment(FoxEnvironment pFoxEnvironment) {
    this.mFoxEnvironment = pFoxEnvironment;
  }

  /**
   * Get a reference to the FoxEnvironment object that has engine configuration details
   *
   * @return
   */
  public FoxEnvironment getFoxEnvironment() {
    if (mFoxEnvironment == null) {
      throw new ExInternal("Cannot get a reference to FoxEnvironment as it has not been set yet");
    }
    return mFoxEnvironment;
  }

  /**
   * Get the IP address for this server
   *
   * @return IP of the server FOX is running on
   */
  public String getServerIP() {
    if (mServerIP == null) {
      try {
        mServerIP = InetAddress.getLocalHost().getHostAddress();
      }
      catch (UnknownHostException e) {
        FoxLogger.getLogger().error("Failed to find host IP for this box, setting global IP string to UNKNOWN", e);
        mServerIP = "UNKNOWN";
      }
    }
    return mServerIP;
  }

  /**
   * Get the hostname for this server
   *
   * @return Hostname for the server FOX is running on
   */
  public String getServerHostName() {
    if(mServerHostName == null) {
      try {
        mServerHostName = InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException e) {
        FoxLogger.getLogger().error("Failed to find hostname for this box, will return fail signifier and IP", e);
        mServerHostName = "FAILED-HOSTNAME-" + getServerIP();
      }
    }
    return mServerHostName;
  }

  /**
   * Get the port defined in the engine configuration which FOX is listening on
   *
   * @return
   */
  public String getEnginePort() {
    return getFoxBootConfig().getFoxEnginePort();
  }

  /**
   * Get a string that should be able to identify the engine, using host:port/context. This corresponds to the
   * engine_locator column on the fox_engines table.
   *
   * @return Engine locator
   */
  public String getEngineLocator() {
    return getServerHostName() + ":" + getEnginePort() + "/" + getContextName();
  }

  public void setFoxBootConfig(FoxBootConfig pFoxBootConfig) {
    this.mFoxBootConfig = pFoxBootConfig;
  }

  public FoxBootConfig getFoxBootConfig() {
    if (mFoxBootConfig == null) {
      throw new ExInternal("Cannot get a reference to FoxBootConfig as it has not been set yet");
    }
    return mFoxBootConfig;
  }

  public FoxBootConfig getFoxBootConfigOrNull() {
    return mFoxBootConfig;
  }

  public boolean isProduction() {
    return mFoxBootConfig.isProduction();
  }

  /**
   * Boolean for whether to show extra information useful for developers when errors occur
   *
   * @return True if engine is not in production mode
   */
  public boolean canShowStackTracesOnError() {
    // Could potentially check engine login status
    return !isProduction();
  }

  public boolean isDevelopment() {
    return !isProduction();
  }

  public String getEngineConnectionPoolName() {
    return "FOX_ENGINE_INTERNAL";
  }

  private String getBootFilePath() {
    if (mBootFilePath == null) {
      mBootFilePath = System.getProperty("user.home") + File.separatorChar + ".fox" + File.separatorChar + getContextName();
    }
    return mBootFilePath;
  }

  public String getBootConfigFilePath() {
    return getBootFilePath() + ".config";
  }

  public String getBootSecurityFilePath() {
    return getBootFilePath() + ".security";
  }

  public boolean isEngineInitialised() {
    return EngineInitialisationController.isEngineInitialised();
  }

  public String getEngineSecurityToken() {
    return mEngineSecurityToken;
  }

  public FoxXPathEvaluator getFoxXPathEvaluator() {
    return mFoxXPathEvaluator;
  }

  /**
   * Gets an obvious, readable FOX version number for this engine (i.e. either the manifest value or "FOXrUNKNOWN").
   * @return Readable version number
   */
  public String getEngineVersionNumber() {
    if(mEngineVersionInfo.isVersionKnown()) {
      return mEngineVersionInfo.getVersionNumber();
    }
    else {
      return "FOXrUNKNOWN";
    }
  }

  public EngineVersionInfo getEngineVersionInfo() {
    return mEngineVersionInfo;
  }
}
