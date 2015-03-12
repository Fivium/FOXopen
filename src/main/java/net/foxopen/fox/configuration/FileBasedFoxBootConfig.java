package net.foxopen.fox.configuration;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExFoxConfigurationValidation;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.logging.FoxLogger;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FileBasedFoxBootConfig
implements FoxBootConfig {
  private final String mDbURL;
  private final String mDbUser;
  private final String mDbPassword;
  private final String mFoxEnvironmentKey;

  private final String mProductionStatus;
  private final String mFoxServiceList;

  private final String mSupportUsername;
  private final String mSupportPassword;

  private final String mAdminUsername;
  private final String mAdminPassword;

  private final String mFoxEnginePort;

  private final Map<String, String> mFoxDatabaseUserMap;

  public static FileBasedFoxBootConfig createFoxBootConfig() throws ExFoxConfiguration {
    FileBasedFoxBootConfig lFoxBootConfig = new FileBasedFoxBootConfig();
    return lFoxBootConfig;
  }

  public static FileBasedFoxBootConfig createFoxBootConfig(HttpServletRequest pFoxConfigurationRequest) throws ExFoxConfiguration {
    FileBasedFoxBootConfig lFoxBootConfig = new FileBasedFoxBootConfig(pFoxConfigurationRequest);
    return lFoxBootConfig;
  }

  /**
   * Load a FileBootConfig from an existing file
   */
  private FileBasedFoxBootConfig() throws ExFoxConfiguration {
    DOM lFoxBootDom = getBootDOM();

    // Gather configuration details from boot DOM
    mDbURL = lFoxBootDom.get1SNoEx("/*/DATABASE_URL");
    mDbPassword = lFoxBootDom.get1SNoEx("/*/PASSWD");
    mDbUser = lFoxBootDom.get1SNoEx("/*/USER");
    mProductionStatus = lFoxBootDom.get1SNoEx("/*/STATUS");
    mFoxServiceList = lFoxBootDom.get1SNoEx("/*/FOX_SERVICE_LIST");
    mSupportUsername = lFoxBootDom.get1SNoEx("/*/SUPPORT_USER");
    mSupportPassword = lFoxBootDom.get1SNoEx("/*/SUPPORT_PASS");
    mAdminUsername = lFoxBootDom.get1SNoEx("/*/ADMIN_USER");
    mAdminPassword = lFoxBootDom.get1SNoEx("/*/ADMIN_PASS");
    mFoxEnvironmentKey = lFoxBootDom.get1SNoEx("/*/FOX_ENVIRONMENT");
    mFoxEnginePort = lFoxBootDom.get1SNoEx("/*/FOX_ENGINE_PORT");

    Map<String, String> lFoxDatabaseUserMap = new HashMap<>();
    DOMList lDatabaseUserList = lFoxBootDom.getUL("/*/DATABASE_USER_LIST/DATABASE_USER");
    for (int i = 0; i < lDatabaseUserList.getLength(); i++) {
      DOM lDatabaseUser = lDatabaseUserList.item(i);
      try {
        String lDBUsername = lDatabaseUser.get1E("USERNAME").value();
        String lDBPassword = lDatabaseUser.get1E("PASSWORD").value();
        lFoxDatabaseUserMap.put(lDBUsername, lDBPassword);
      }
      catch (ExTooFew e) {
        throw new ExFoxConfiguration("Could not find the username or password for a database user element.", e);
      }
      catch (ExTooMany e) {
        throw new ExFoxConfiguration("Found too many usernames or passwords for a database user element.", e);
      }
    }
    mFoxDatabaseUserMap = Collections.unmodifiableMap(lFoxDatabaseUserMap);
  }

  /**
   * Create a new File Boot Config, taking in posted form parameters
   *
   * @param pFoxConfigurationRequest
   * @throws ExFoxConfiguration
   */
  private FileBasedFoxBootConfig(HttpServletRequest pFoxConfigurationRequest) throws ExFoxConfiguration {
    // Validate request before any work is done
    validateFoxConfig(pFoxConfigurationRequest);

    // Grab database usernames and passwords and set them
    Map<String, String> lFoxDatabaseUserMap = new HashMap<>();
    int lFoxConnectionCount = Integer.parseInt(pFoxConfigurationRequest.getParameter("fox_connection_count"));
    for (int i = 0; i < lFoxConnectionCount; i++) {
      String lDBUserName = pFoxConfigurationRequest.getParameter("db_username_"+i);
      String lDBPassword = pFoxConfigurationRequest.getParameter("db_password_"+i);

      // If exactly 1 of these is null then throw an error, otherwise if both are null then ignore this row rather than throwing an error.
      if (XFUtil.isNull(lDBUserName) ^ XFUtil.isNull(lDBPassword)) {
        throw new ExFoxConfiguration("Missing username or password for connection at index " + i + (!XFUtil.isNull(lDBUserName) ? " (username " + lDBUserName + ")" : ""));
      }

      if (!XFUtil.isNull(lDBUserName) && !XFUtil.isNull(lDBPassword)) {
        lDBUserName = lDBUserName.trim().toUpperCase();
        lDBPassword = lDBPassword.trim();
        lFoxDatabaseUserMap.put(lDBUserName, lDBPassword);
      }
    }
    mFoxDatabaseUserMap = lFoxDatabaseUserMap;

    // Set values
    mProductionStatus = pFoxConfigurationRequest.getParameter("status");
    mDbUser = pFoxConfigurationRequest.getParameter("db_user").toUpperCase();
    mDbURL = pFoxConfigurationRequest.getParameter("db_url");
    mSupportUsername = pFoxConfigurationRequest.getParameter("support_user");
    mAdminUsername = pFoxConfigurationRequest.getParameter("admin_user");
    mFoxEnvironmentKey = pFoxConfigurationRequest.getParameter("fox_environment");
    mFoxServiceList = pFoxConfigurationRequest.getParameter("fox_service_list");
    mFoxEnginePort = pFoxConfigurationRequest.getParameter("fox_engine_port");

    // Augment values from old fox boot config and set the database password if it is there.
    FoxBootConfig lCurrentFoxBootConfig = FoxGlobals.getInstance().getFoxBootConfigOrNull();
    if (lCurrentFoxBootConfig != null) {
      if (pFoxConfigurationRequest.getParameter("db_password").replaceAll("\u00A0", "").length() == 0) {
        mDbPassword = lCurrentFoxBootConfig.getDbPassword();
      }
      else {
        mDbPassword = pFoxConfigurationRequest.getParameter("db_password");
      }

      if (pFoxConfigurationRequest.getParameter("support_password").replaceAll("\u00A0", "").length() == 0) {
        mSupportPassword = lCurrentFoxBootConfig.getSupportPassword();
      }
      else {
        mSupportPassword = FoxConfigHelper.hashInternalPassword(pFoxConfigurationRequest.getParameter("support_password"));
      }

      if (pFoxConfigurationRequest.getParameter("admin_password").replaceAll("\u00A0", "").length() == 0) {
        mAdminPassword = lCurrentFoxBootConfig.getAdminPassword();
      }
      else {
        mAdminPassword = FoxConfigHelper.hashInternalPassword(pFoxConfigurationRequest.getParameter("admin_password"));
      }

      // Process Fox DB User map
      for (Map.Entry<String, String> lFoxDBUser : mFoxDatabaseUserMap.entrySet()) {
        String lDBUsername = lFoxDBUser.getKey();
        String lDBPassword = lFoxDBUser.getValue();
        // If the password has not changed then set the old one, the flag used is a non-breakable space. html code 160.
        if (lDBPassword.replaceAll("\u00A0", "").length() == 0) {
          mFoxDatabaseUserMap.put(lDBUsername.toUpperCase(), lCurrentFoxBootConfig.getFoxDatabaseUserMap().get(lDBUsername));
        }
      }
    }
    else {
      mDbPassword = pFoxConfigurationRequest.getParameter("db_password");
      mSupportPassword = FoxConfigHelper.hashInternalPassword(pFoxConfigurationRequest.getParameter("support_password"));
      mAdminPassword = FoxConfigHelper.hashInternalPassword(pFoxConfigurationRequest.getParameter("admin_password"));
    }

    writeToFile(pFoxConfigurationRequest.getParameter("encryptionKey"));
  }

  public boolean isProduction() {
    return "PRODUCTION".equals(mProductionStatus.toUpperCase());
  }

  private void validateFoxConfig(HttpServletRequest pFoxConfigurationRequest) throws ExFoxConfiguration {
    String lFoxEnvironment = pFoxConfigurationRequest.getParameter("fox_environment");
    checkStringNotNull(lFoxEnvironment, "No Fox environment was specified. If the list is null make sure you click the test connection button.");

    String lDatabasePublicEncryptionKey = pFoxConfigurationRequest.getParameter("encryptionKey");
    checkStringNotNull(lDatabasePublicEncryptionKey, "Public encryption key cannot be null");

    String lStatus = pFoxConfigurationRequest.getParameter("status");
    checkStringNotNull(lStatus, "Status not specified");

    String lDatabasePassword = pFoxConfigurationRequest.getParameter("db_password");
    checkStringNotNull(lDatabasePassword, "Database password not specified");

    String lDatabaseUsername = pFoxConfigurationRequest.getParameter("db_user");
    checkStringNotNull(lDatabaseUsername, "Database username not specified");

    String lDatabaseURL = pFoxConfigurationRequest.getParameter("db_url");
    checkStringNotNull(lDatabaseURL, "Database URL not specified");

    String lSupportUsername = pFoxConfigurationRequest.getParameter("support_user").trim();
    checkStringNotNull(lSupportUsername, "You must set a support username");

    String lSupportPassword = pFoxConfigurationRequest.getParameter("support_password").trim();
    checkStringNotNull(lSupportPassword, "You must set a support password");

    String lAdminUsername = pFoxConfigurationRequest.getParameter("admin_user").trim();
    checkStringNotNull(lAdminUsername, "You must set an administrator username");

    String lAdminPassword = pFoxConfigurationRequest.getParameter("admin_password").trim();
    checkStringNotNull(lAdminPassword, "You must set an administrator password");

    String lFoxEnginePort = pFoxConfigurationRequest.getParameter("fox_engine_port").trim();
    checkStringNotNull(lFoxEnginePort, "You must set a fox engine port.");

    try {
      Integer.parseInt(lFoxEnginePort);
    } catch (NumberFormatException e) {
      throw new ExFoxConfigurationValidation("The number provided for the fox engine port was not a valid format. Input given was " + lFoxEnginePort, e);
    }

    // If support passwords do not match
    if (!lAdminPassword.equals(pFoxConfigurationRequest.getParameter("admin_password_2"))) {
      throw new ExFoxConfigurationValidation("Adminstrator password values do not match");
    }

    if (FoxBootConfig.DEFAULT_ADMIN_PASSWORD.equals(lAdminPassword.toLowerCase()) || FoxBootConfig.DEFAULT_ADMIN_PASSWORD.equals(lSupportPassword.toLowerCase())) {
      throw new ExFoxConfigurationValidation("Cannot set the login passwords to their default values");
    }

    // If encryption keys do not match, throw error.
    if(!XFUtil.checkRSAKeyPair(getDecryptionKey(), XFUtil.decodePublicKey(pFoxConfigurationRequest.getParameter("encryptionKey").replaceAll("(\r)*", ""), "RSA"))){
      throw new ExFoxConfigurationValidation("Invalid encryption key");
    }
  }

  private static void checkStringNotNull(String pStringToCheck, String pExceptionMessage) throws ExFoxConfigurationValidation {
    if (XFUtil.isNull(pStringToCheck)) {
      throw new ExFoxConfigurationValidation(pExceptionMessage);
    }
  }

  public String getDatabaseURL() {
    return mDbURL;
  }

  public String getMainDatabaseUsername() {
    return mDbUser;
  }

  public String getDbPassword() {
    return mDbPassword;
  }

  public String getProductionStatus() {
    return mProductionStatus;
  }

  public String getFoxServiceList() {
    return mFoxServiceList;
  }

  public String getSupportUsername() {
    return mSupportUsername;
  }

  public String getSupportPassword() {
    return mSupportPassword;
  }

  public String getAdminUsername() {
    return mAdminUsername;
  }

  public String getAdminPassword() {
    return mAdminPassword;
  }

  public Map<String, String> getFoxDatabaseUserMap() {
    return mFoxDatabaseUserMap;
  }

  public String getFoxEnginePort() {
    return mFoxEnginePort;
  }

  private PrivateKey getDecryptionKey() throws ExFoxConfiguration {
    // Get decryption key (private RSA key) from .security file
    File lSecurityFile = new File(FoxGlobals.getInstance().getBootSecurityFilePath());
    if (lSecurityFile.exists() && lSecurityFile.length() > 0) {
      FileReader lFileReader;
      try {
        lFileReader = new FileReader(lSecurityFile);
        DOM lBootSecurityDOM = DOM.createDocument(lFileReader, false);
        String lMethod = lBootSecurityDOM.get1SNoEx("/*/METHOD");
        if ("LITERAL".equals(lMethod)) {
          String lDecryptionKey = lBootSecurityDOM.get1SNoEx("/*/DECRYPTION_KEY");
          return XFUtil.decodePrivateKey(lDecryptionKey, "RSA");
        }
        else {
          // Potentially other methods in here to look up the key from LDAP or AD
          throw new ExFoxConfiguration("Security file found, but encountered an unknown method in the security file DOM: " + lMethod);
        }
      }
      catch (FileNotFoundException e) {
        throw new ExFoxConfiguration("Security file not found, not readable or zero length: " + lSecurityFile.getAbsolutePath(), e);
      }
    }
    else {
      throw new ExFoxConfiguration("Security file not found, not readable or zero length: " + lSecurityFile.getAbsolutePath());
    }
  }

  private DOM getBootDOM() throws ExFoxConfiguration {
    PrivateKey lDecryptionKey = getDecryptionKey();
    InputStream lInput;
    try {
      lInput = new FileInputStream(FoxGlobals.getInstance().getBootConfigFilePath());
      ByteArrayOutputStream lOut = new ByteArrayOutputStream();
      XFUtil.decryptRSAStream(lInput, lOut, lDecryptionKey);
      return DOM.createDocumentFromXMLString(new String(lOut.toByteArray()));
    }
    catch (Exception e) {
      throw new ExFoxConfiguration("Error trying to get the boot DOM", e);
    }
  }

  private void writeToFile(String pPublicEncryptionKey) throws ExFoxConfiguration {
    // Create the DOM for storing the configuration information
    DOM lConfigDOM = DOM.createDocument("FOX_BOOT");

    // Store all the config information
    lConfigDOM.addElem("DATABASE_URL").setText(mDbURL);

    // Store each database user specified and password
    DOM lDatabaseUserList = lConfigDOM.addElem("DATABASE_USER_LIST");
    for (Map.Entry<String, String> lFoxDBUser : mFoxDatabaseUserMap.entrySet()) {
      String lDBUsername = lFoxDBUser.getKey();
      String lDBPassword = lFoxDBUser.getValue();
      DOM lDatabaseUser = lDatabaseUserList.addElem("DATABASE_USER");
      lDatabaseUser.addElem("USERNAME").setText(lDBUsername);
      lDatabaseUser.addElem("PASSWORD").setText(lDBPassword);
    }

    // Store the primary engine database user
    lConfigDOM.addElem("USER").setText(mDbUser);
    lConfigDOM.addElem("PASSWD").setText(mDbPassword);

    lConfigDOM.addElem("FOX_ENVIRONMENT").setText(mFoxEnvironmentKey);
    lConfigDOM.addElem("STATUS").setText(mProductionStatus);
    lConfigDOM.addElem("FOX_SERVICE_LIST").setText(mFoxServiceList);
    lConfigDOM.addElem("FOX_ENGINE_PORT").setText(mFoxEnginePort);

    lConfigDOM.addElem("SUPPORT_USER").setText(mSupportUsername.trim());
    lConfigDOM.addElem("SUPPORT_PASS").setText(mSupportPassword.trim());

    lConfigDOM.addElem("ADMIN_USER").setText(mAdminUsername.trim());
    lConfigDOM.addElem("ADMIN_PASS").setText(mAdminPassword.trim());

    try {
      PublicKey publicKey = XFUtil.decodePublicKey(pPublicEncryptionKey, "RSA");
      InputStream lDOMStream = new ByteArrayInputStream(lConfigDOM.getRootElement().outputNodeToString(false).getBytes());
      XFUtil.encryptRSAStream(lDOMStream, new FileOutputStream(FoxGlobals.getInstance().getBootConfigFilePath()), publicKey);
    }
    catch (Exception e){
      FoxLogger.getLogger().error("Failed to save configuration file", e);
      throw new ExFoxConfiguration("Failed to save configuration file!" + e.getMessage(), e);
    }
  }

  public String getFoxEnvironmentKey() {
    return mFoxEnvironmentKey;
  }

  public String getDatabaseUserPassword(String pUsername) throws ExFoxConfiguration {
    if (XFUtil.isNull(pUsername)) {
      throw new ExFoxConfiguration("The username given to getUserPassword was null.");
    }

    if (pUsername.equals(mDbUser)) {
      return mDbPassword;
    }
    else {
      String lPassword = mFoxDatabaseUserMap.get(pUsername);
      if (XFUtil.isNull(lPassword)) {
        throw new ExFoxConfiguration("The password returned for a user was null. Are you sure this users password was stored in the !CONFIGURE file. User " + pUsername);
      }
      return lPassword;
    }
  }
}
