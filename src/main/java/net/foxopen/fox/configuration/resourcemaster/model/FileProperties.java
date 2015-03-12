package net.foxopen.fox.configuration.resourcemaster.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.foxopen.fox.App;
import net.foxopen.fox.FileUploadType;
import net.foxopen.fox.configuration.resourcemaster.definition.AppProperty;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxApplicationDefinition;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.filetransfer.FileTransferServiceQueue;
import net.foxopen.fox.queue.ServiceQueueHandler;

public class FileProperties {
  private final List<FileTransferQueue> mFileTransferQueueList;
  private String mDefaultFileUploadType;
  private final Map<String, FileUploadType> mFileUploadTypeMap;

  public static FileProperties createFileProperties(String pAppMnem, FoxApplicationDefinition pFoxApplicationDefinition, FoxEnvironment pFoxEnvironment, App pApp) throws ExApp {
    return new FileProperties(pAppMnem, pFoxApplicationDefinition, pFoxEnvironment, pApp);
  }

  public FileProperties(String pAppMnem, FoxApplicationDefinition pFoxApplicationDefinition, FoxEnvironment pFoxEnvironment, App pApp) throws ExApp {
    super();
    // File properties
    DOM lFileTransferQueueListDOM = pFoxApplicationDefinition.getPropertyAsDOM(AppProperty.FILE_TRANSFER_QUEUE_LIST);
    mFileTransferQueueList = Collections.unmodifiableList(processFileTransferQueueListDOM(lFileTransferQueueListDOM, pFoxEnvironment, pAppMnem, pApp));
    DOM lFileUploadTypeList = pFoxApplicationDefinition.getPropertyAsDOM(AppProperty.FILE_UPLOAD_TYPE_LIST);
    mFileUploadTypeMap = Collections.unmodifiableMap(processFileUploadTypeList(lFileUploadTypeList));
  }

  private List<FileTransferQueue> processFileTransferQueueListDOM(DOM pFileTransferQueueListDOM, FoxEnvironment pFoxEnvironment, String pAppMnem, App pApp) {
    // Create any file transfer queues for this app
    ServiceQueueHandler lFileTransferQueueHandler;

    //Sync on class to ensure static QueueHandler map is populated (might not be during a flush)
    synchronized(ServiceQueueHandler.class){
      lFileTransferQueueHandler = ServiceQueueHandler.getQueueHandlerByName("FileTransfer");
    }

    List<FileTransferQueue> lFileTransferQueueList = new ArrayList<>();
    DOMList lFileTransferQueueListDOM = pFileTransferQueueListDOM.getUL("*");

    for (int i = 0; i < lFileTransferQueueListDOM.getLength(); i++) {
      DOM lFileTransferQueueDOM = lFileTransferQueueListDOM.item(i);
      FileTransferQueue lFileTransferQueue = FileTransferQueue.createFileTransferQueue(lFileTransferQueueDOM);
      lFileTransferQueueList.add(lFileTransferQueue);

      // Construct the queue mnem
      String lQueueMnem = pAppMnem + "-" + lFileTransferQueue.getName();

      // Skip this queue item if filetransfer handler already contains the specified key
      // Don't want to replace queues that are being used on a flush...
      // NEED TO COME BACK TO THIS TO PREVENT APP MEMORY LEAK... AT THE MIN PREVENTS FLUSH CLEANING UP
      if (lFileTransferQueueHandler.queueNameExists(lQueueMnem)) continue;

      // build service queue from definition
      FileTransferServiceQueue lServiceQueue = new FileTransferServiceQueue(
        lQueueMnem
      , pApp // Pass a handle to this App instance to the upload queue
      , ""+lFileTransferQueue.getMaxUploadChannels()
      , ""+lFileTransferQueue.getMaxDownloadChannels()
      , ""+lFileTransferQueue.getMinFileBytes()
      , ""+lFileTransferQueue.getMaxFileBytes()
      , lFileTransferQueueHandler
      );

      // Place the queue in the file transfer queue handlers list of queues to maintain
      lFileTransferQueueHandler.assignServiceQueue(lServiceQueue);
    }

    return lFileTransferQueueList;
  }

  private Map<String, FileUploadType> processFileUploadTypeList(DOM pFileUploadTypeList) {
    Map<String, FileUploadType> lFileUploadTypeList = new HashMap<>();
    DOMList lFileUploadTypeDOMList = pFileUploadTypeList.getUL("*");
    for (int i = 0; i < lFileUploadTypeDOMList.getLength(); i++) {
      DOM lFileUploadTypeDOM = lFileUploadTypeDOMList.item(i);
      String lName = lFileUploadTypeDOM.get1SNoEx("name");
      if (lFileUploadTypeDOM.getAttrOrNull("default") != null) {
        mDefaultFileUploadType = lName;
      }
      lFileUploadTypeList.put(lName, FileUploadType.constructFromXML(lFileUploadTypeDOM));
    }
    return lFileUploadTypeList;
  }

  public List<FileTransferQueue> getFileTransferQueueList() {
    return mFileTransferQueueList;
  }

  public String getDefaultFileUploadType() {
    return mDefaultFileUploadType;
  }

  public Map<String, FileUploadType> getFileUploadTypeMap() {
    return mFileUploadTypeMap;
  }
}
