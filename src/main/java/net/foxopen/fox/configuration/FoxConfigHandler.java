package net.foxopen.fox.configuration;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExInternalConfiguration;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.logging.FoxLogger;
import org.json.simple.JSONObject;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

public class FoxConfigHandler {

  /**
   * Lets the user set up the FOX boot config file
   *
   * @param pFoxRequest initiating FoxRequest
   * @return FoxResponse (Some text/html response)
   */
  public static FoxResponse processConfigureRequest (FoxRequest pFoxRequest)
  throws ExInternal, ExFoxConfiguration, ExTooFew, ExTooMany {
    // First response variables, defaults.
    String lDbURL = "";
    String lDbUser = "FOX5MGR";
    String lDbPassword = "";
    String lProductionStatus = "";
    String lFoxServiceList = "";
    String lSupportUsername = "";
    String lAdminUsername = "";
    String lFoxEnvironment = "";
    String lFoxEnginePort = ""+pFoxRequest.getHttpRequest().getServerPort();
    Map<String, String> lFoxDatabaseUserMap = new HashMap<>();
    boolean lIsConfigured = false;

    // Grab the master boot dom if it exists here, otherwise throw error
    try {
      FileBasedFoxBootConfig lFoxConfig = FileBasedFoxBootConfig.createFoxBootConfig();
      lDbURL = lFoxConfig.getDatabaseURL();
      lDbUser = lFoxConfig.getMainDatabaseUsername();
      lDbPassword = lFoxConfig.getDbPassword();
      lProductionStatus = lFoxConfig.getProductionStatus();
      lFoxServiceList = lFoxConfig.getFoxServiceList();
      lSupportUsername = lFoxConfig.getSupportUsername();
      lAdminUsername = lFoxConfig.getAdminUsername();
      lFoxEnvironment = lFoxConfig.getFoxEnvironmentKey();
      lFoxEnginePort = lFoxConfig.getFoxEnginePort();
      lFoxDatabaseUserMap = lFoxConfig.getFoxDatabaseUserMap();
      lIsConfigured = true;
    }
    catch (ExFoxConfiguration e) {
      /* do nothing */
    }

    // Create a http request with all the information the JSP page needs
    HttpServletRequest lHttpRequest = pFoxRequest.getHttpRequest();
    lHttpRequest.setAttribute("db_url", lDbURL);
    lHttpRequest.setAttribute("db_user", lDbUser);
    lHttpRequest.setAttribute("db_password", lDbPassword);
    lHttpRequest.setAttribute("is_development", lProductionStatus.equals("DEVELOPMENT")?"selected":"" );
    lHttpRequest.setAttribute("is_production", lProductionStatus.equals("PRODUCTION")?"selected":"");
    lHttpRequest.setAttribute("is_configured", lIsConfigured);
    lHttpRequest.setAttribute("fox_service_list", lFoxServiceList);
    lHttpRequest.setAttribute("support_user", lSupportUsername);
    lHttpRequest.setAttribute("admin_user", lAdminUsername);
    lHttpRequest.setAttribute("fox_environment", lFoxEnvironment);
    lHttpRequest.setAttribute("fox_db_user_map", lFoxDatabaseUserMap);
    lHttpRequest.setAttribute("fox_engine_port", lFoxEnginePort);

    // Forward this request to the JSP page
    RequestDispatcher lRequestDispatcher = lHttpRequest.getRequestDispatcher("/WEB-INF/components-new/foxConfig.jsp");
    try {
      lRequestDispatcher.forward(lHttpRequest, pFoxRequest.getHttpResponse());
    }
    catch (ServletException | IOException e) {
      throw new ExInternalConfiguration("Error when creating configuration webpage.", e);
    }

    return null; // no response needed, a request has already been forwarded.
  }

  public static FoxResponse processConfigureHandleRequest (FoxRequest pFoxRequest)
  throws ExInternal, ExFoxConfiguration, ExTooFew, ExTooMany, ExBadPath, ExServiceUnavailable {

    JSONObject lResponse = new JSONObject();
    try {
      // Create new Boot Config from posted parameters
      //NOTE: this saves the config to disk then immeidately re-reads it...
      FileBasedFoxBootConfig.createFoxBootConfig(pFoxRequest.getHttpRequest());
      //Re-read
      FoxConfigHelper.getInstance().loadEngineBootConfig();
      lResponse.put("message", "Configuration saved successfully.");
      lResponse.put("status", "success");
    }
    catch (Throwable th) {
      ExFoxConfiguration lError = new ExFoxConfiguration("Error loading configuration: ", th);
      lResponse.put("message", XFUtil.getJavaStackTraceInfo(lError));
      lResponse.put("status", "failed");
    }

    return new FoxResponseCHAR("application/json", new StringBuffer(lResponse.toJSONString()), 0);
  }

  public static FoxResponse processSecurityHandleRequest(FoxRequest pFoxRequest) {
    if("".equals(XFUtil.nvl(pFoxRequest.getHttpRequest().getParameter("decryptionKey"), ""))){
      return new FoxResponseCHAR("text/plain", new StringBuffer("You have to enter the private part of a 2048bit RSA key"), 0);
    }

    // Generate DOM to write out
    String lMethod = pFoxRequest.getHttpRequest().getParameter("method");
    String lDecryptionKey = pFoxRequest.getHttpRequest().getParameter("decryptionKey").replaceAll("(\r)*", "");
    DOM lSecurityDOM = generateSecurityDOM(lMethod, lDecryptionKey);

    try {
      writePrivateSecurityKey(lSecurityDOM);
    }
    catch (Exception e) {
      return new FoxResponseCHAR("text/html", new StringBuffer("Failed to save security file!<br />" + e.getMessage()), 0);
    }
    return new FoxResponseCHAR("text/html", new StringBuffer("Saved security file!"), 0);
  }

  /**
   * Lets the user set up the FOX boot security key file
   * @param pFoxRequest initiating FoxRequest
   * @return FoxResponse (Some text/html response)
   */
  public static FoxResponse processSecurityRequest(FoxRequest pFoxRequest)
    throws ExInternal {
    StringBuffer lURLStringBuffer = pFoxRequest.getRequestURIStringBuffer();
    String lSubCmd = lURLStringBuffer.substring(lURLStringBuffer.lastIndexOf("/") + 1);

    if ("DEFAULT".equals(lSubCmd.toUpperCase())){
      // !DEFAULT generates keys for you
      KeyPair lKeys = XFUtil.generateRSAKeys();
      return new FoxResponseCHAR("text/html", new StringBuffer(
        "<html><body><form name='secure' method='post' action='../!HANDLESECURITY'>" +
          "Method:<br /><select name='method'><option value='LITERAL'>Literal</option></select><br />" +
          "Encryption Key<small>(public key)</small>: <small>(Save this for use with !CONFIGURE)</small><br /><textarea rows='6' cols='78' readonly='readonly' name='encryptionKey' >" + XFUtil.encodeBASE64(lKeys.getPublic().getEncoded()) + "</textarea><br />" +
          "Decryption Key<small>(private key)</small>:<br /><textarea rows='7' cols='78' readonly='readonly' name='decryptionKey' >" + XFUtil.encodeBASE64(lKeys.getPrivate().getEncoded()) + "</textarea><br />" +
          "<input type='hidden' name='xfsessionid' value='"+ pFoxRequest.getHttpRequest().getParameter("xfsessionid") + "'/>" +
          "<input type='submit' name='save' value='Save' /></form></body></html>"), 0);
    }
    else if ("GENERATE".equals(lSubCmd.toUpperCase())) {
      KeyPair lKeys = XFUtil.generateRSAKeys();

      // Generate DOM to write out
      DOM lSecurityDOM = generateSecurityDOM("LITERAL", XFUtil.encodeBASE64(lKeys.getPrivate().getEncoded()));

      JSONObject lJSONEntryKey = new JSONObject();
      try {
        writePrivateSecurityKey(lSecurityDOM);
        lJSONEntryKey.put("status","true");
        lJSONEntryKey.put("generated_key", XFUtil.encodeBASE64(lKeys.getPublic().getEncoded()));
      }
      catch (ExFoxConfiguration e) {
        lJSONEntryKey.put("status","false");
        lJSONEntryKey.put("message","An error occured while trying to get the generated key: " + e.getMessage());
        lJSONEntryKey.put("stack", XFUtil.getJavaStackTraceInfo(e));

        return new FoxResponseCHAR("application/json", new StringBuffer(lJSONEntryKey.toJSONString()), 0);
      }

      return new FoxResponseCHAR("text/json", new StringBuffer(lJSONEntryKey.toJSONString()), 0);
    }
    else {
      return new FoxResponseCHAR("text/html", new StringBuffer(
        "<html><body><form name='secure' method='post' action='../!HANDLESECURITY'>" +
          "Method:<br /><select name='method'><option value='LITERAL'>Literal</option></select><br />" +
          "Decryption Key<small>(private key)</small>:<br /><textarea rows='7' cols='78' name='decryptionKey' ></textarea><br />" +
          "<input type='hidden' name='xfsessionid' value='"+ pFoxRequest.getHttpRequest().getParameter("xfsessionid") + "'/>" +
          " <input type='submit' name='save' value='Save' /></form></body></html>"), 0);
    }
  }

  private static DOM generateSecurityDOM(String pMethod, String pDecryptionKey) {
    DOM lSecurityDOM = DOM.createDocument("KEY");
    lSecurityDOM.addElem("METHOD").setText(pMethod);
    lSecurityDOM.addElem("DECRYPTION_KEY").setText(pDecryptionKey);

    return lSecurityDOM;
  }

  private static void writePrivateSecurityKey(DOM pSecurityDOM) throws ExFoxConfiguration {
    createFOXFolder();

    File lSecurityFile = new File(FoxGlobals.getInstance().getBootSecurityFilePath());
    if (lSecurityFile.exists() && lSecurityFile.length() > 0) {
      throw new ExFoxConfiguration("Cannot overwrite security file '" + lSecurityFile.getAbsolutePath() + "', you must manually remove it from the server.");
    }

    // Attempt to lock file
    FileOutputStream lSecurityFileStream;
    FileLock lLock;
    try {
      lSecurityFileStream = new FileOutputStream(lSecurityFile);
      lLock = lSecurityFileStream.getChannel().tryLock();
      lSecurityFileStream.close();
    }
    catch (IOException e) {
      throw new ExFoxConfiguration("Cannot overwrite security file '" + lSecurityFile.getAbsolutePath() + "', you must manually remove it from the server.", e);
    }

    // Attempt to write dom to file and unlock, or just unlock if possible
    FileWriter lSecurityFileWriter = null;
    try{
      lSecurityFileWriter = new FileWriter(FoxGlobals.getInstance().getBootSecurityFilePath());
      pSecurityDOM.outputDocumentToWriter(lSecurityFileWriter, false);
    }
    catch (Exception e){
      FoxLogger.getLogger().error("Failed to write FOX config file", e);
      try {
        if(lLock != null){
          lLock.release();
          lSecurityFileStream.close();
        }
      }
      catch (IOException ex) {
      }
    }
    finally {
      if (lSecurityFileWriter != null) {
        try {
          lSecurityFileWriter.close();
        } catch (IOException e) {} // ignore
      }
    }
  }

  private static void createFOXFolder() {
    File lFoxFolder = new File(System.getProperty("user.home") + File.separatorChar + ".fox");
    if (!lFoxFolder.exists()) {
      lFoxFolder.mkdir();
    }
  }
}
