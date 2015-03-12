package net.foxopen.fox.filetransfer;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.foxopen.fox.App;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;


public class UploadLogger {
  
  private static final String INSERT_LOG_FILENAME = "InsertUploadLog.sql";
  private static final String UPDATE_LOG_FILENAME = "UpdateUploadLog.sql";
  
  private final String mAppMnem;
  private final String mWuaId;
  private final String mModuleName;
  private final String mStateName;
  
  private boolean mLogRowCreated = false;
  private DOM mLogXML = DOM.createDocument("upload-info");  

  public UploadLogger(App pApp, String pWuaId, String pModule, String pState){
    mAppMnem = pApp.getMnemonicName();
    mWuaId = pWuaId;
    mModuleName = pModule;
    mStateName = pState;
  }
  
  public void addLogXML(String pElementName, DOM pXML) {
    pXML.copyContentsTo(mLogXML.addElem(pElementName));
  }
  
  /**
   * Create an entry in the appropriate App's upload-log-table, using an autonomous UCon
   */
  public void startLog(UCon pUCon, UploadInfo pUploadInfo, String pIpAddress){
  
    UConBindMap lBindMap = new UConBindMap();

    try {
      lBindMap.defineBind(":file_id", pUploadInfo.getFileId());
      lBindMap.defineBind(":app_mnem", mAppMnem);
      lBindMap.defineBind(":app_host", InetAddress.getLocalHost().getHostAddress());
      lBindMap.defineBind(":wua_id", mWuaId);
      lBindMap.defineBind(":module", mModuleName);
      lBindMap.defineBind(":state", mStateName);
      lBindMap.defineBind(":client_ip", pIpAddress);

      pUCon.executeAPI(SQLManager.instance().getStatement(INSERT_LOG_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to insert row for upload log of file " +  pUploadInfo.getFileId(), e);
    }
    catch (UnknownHostException e) {
      throw new ExInternal("Failed to determine hostname for upload log of file " +  pUploadInfo.getFileId(), e);
    }
    
    mLogRowCreated = true;
  }
  
  public void updateLog(UCon pUCon, UploadInfo pUploadInfo){
    
    String lStatus;
    switch(pUploadInfo.getStatus()){
      case COMPLETE: 
        lStatus = "SUCCESS"; 
        break;
      case FAILED: 
      case CONTENT_CHECK_FAILED:
      case VIRUS_CHECK_FAILED:
        lStatus = "FAIL"; 
        break;
      default:
        lStatus = "INPROGRESS";
    }
    
    try {
      UConBindMap lBindMap = new UConBindMap();
      
      lBindMap.defineBind(":file_approx_size", Long.toString(pUploadInfo.getHttpContentLength()));
      lBindMap.defineBind(":file_content_type", pUploadInfo.getTrueContentType());
      lBindMap.defineBind(":status", lStatus);
      lBindMap.defineBind(":fail_reason", UCon.bindStringAsClob(pUploadInfo.getSystemMsg()));
      lBindMap.defineBind(":magic_mime_types", pUploadInfo.getMagicContentTypes());
      lBindMap.defineBind(":filename", pUploadInfo.getFilename());
      lBindMap.defineBind(":xml_data", mLogXML);
      lBindMap.defineBind(":file_id", pUploadInfo.getFileId());
      
      pUCon.executeAPI(SQLManager.instance().getStatement(UPDATE_LOG_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Upload logging: Error updating upload log (filename = '" + pUploadInfo.getFilename() + "', file id = '" +  pUploadInfo.getFileId() + "')", e);
    }
  }
}
