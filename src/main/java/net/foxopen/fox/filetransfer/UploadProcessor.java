package net.foxopen.fox.filetransfer;


import net.foxopen.fox.App;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FileUploadType;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableAPI;
import net.foxopen.fox.database.storage.WorkDoc;
import net.foxopen.fox.database.storage.lob.LOBWorkDoc;
import net.foxopen.fox.database.storage.lob.WriteableLOBWorkDoc;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUpload;
import net.foxopen.fox.filetransfer.UploadInfo.ForceFailReason;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.UploadedFileInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.queue.ServiceQueueHandler;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.RampedThreadRunnable;
import net.foxopen.fox.thread.persistence.xstream.XStreamManager;
import net.foxopen.fox.thread.storage.FileStorageLocation;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;
import net.foxopen.fox.thread.storage.WorkingUploadStorageLocation;
import net.foxopen.fox.track.Track;
import org.json.simple.JSONObject;

import java.sql.Blob;
import java.sql.Savepoint;


/**
 * Object for handling the lifecycle of a single file upload.
 */
public class UploadProcessor {

  private final String mThreadId;
  private final String mCallId;
  /** The node which will "contain" this file upload. This is also the target node for single uploads. */
  private final String mUploadContainerContextRef;
  /** The node which the upload metadata is to be written to. Populated just in time when the upload cardinality is known. */
  private String mUploadTargetNodeRef;

  private static final boolean INTERRUPT_EXISTING_UPLOADS = true;

  public UploadProcessor(String pThreadId, String pCallId, String pUploadContainerContextRef) {
    mThreadId = pThreadId;
    mCallId = pCallId;
    mUploadContainerContextRef = pUploadContainerContextRef;
  }

  private UploadInfo constructUploadInfo(FileUploadType pFileUploadType, App pApp) {

    UploadInfo lExistingUploadInfo = UploadInfoCache.getUploadInfo(mThreadId, mCallId, mUploadContainerContextRef);

    if(lExistingUploadInfo != null) {
      if(INTERRUPT_EXISTING_UPLOADS) {
        //NOTE: we used to wait for an upload to finish if already in progress, but this caused problems when the upload got stuck.
        lExistingUploadInfo.forceFailUpload(ForceFailReason.NEW_UPLOAD);
      }
    }

    //TODO PN IMAGE UPLOADS - create image upload info if required
    UploadInfo lNewUploadInfo = new UploadInfo(mThreadId, mCallId, mUploadContainerContextRef, pApp, pFileUploadType);
    UploadInfoCache.cacheUploadInfo(mThreadId, mCallId, mUploadContainerContextRef, lNewUploadInfo);

    return lNewUploadInfo;
  }

  private class UploadThreadInitialiser implements RampedThreadRunnable {

    private final MultipartUploadReader mMultipartUploadReader;

    private String mWuaId;
    private String mStateName;
    private Mod mModule;

    private WorkingUploadStorageLocation mWorkingSL;
    private UploadInfo mUploadInfo;

    private UploadThreadInitialiser(MultipartUploadReader pMultipartUploadReader) {
      mMultipartUploadReader = pMultipartUploadReader;
    }

    @Override
    public void run(ActionRequestContext pRequestContext) {
      //Run client actions on the thread to ensure all deletion requests are processed before anything else happens
      String lClientActionJSON = mMultipartUploadReader.getFormFieldValue("clientActions");
      if(!XFUtil.isNull(lClientActionJSON)) {
        pRequestContext.applyClientActions(lClientActionJSON);
      }

      //Get references to the current state of the thread
      mStateName = pRequestContext.getCurrentState().getName();
      mWuaId = pRequestContext.getAuthenticationContext().getAuthenticatedUser().getAccountID();

      ContextUElem lContextUElem = pRequestContext.getContextUElem();
      mModule = pRequestContext.getCurrentModule();

      DOM lUploadContainerDOM = lContextUElem.getElemByRef(mUploadContainerContextRef);

      NodeInfo lNodeInfo = mModule.getNodeInfo(lUploadContainerDOM);
      String lFileUploadType = lNodeInfo.getFoxNamespaceAttribute(NodeAttribute.UPLOAD_FILE_TYPE);

      mUploadInfo = constructUploadInfo(pRequestContext.getRequestApp().getFileUploadType(lFileUploadType), pRequestContext.getRequestApp());

      //If the upload target is a multi-upload node, create a new list element to be the target DOM - otherwise the target is the container
      DOM lUploadTargetDOM;
      if(lNodeInfo.getListMaxCardinality() > 0) {
        //Validate that we don't already have enough files
        if(lUploadContainerDOM.getChildNodes().size() >= lNodeInfo.getListMaxCardinality()) {
          throw new ExUpload("you cannot upload any more files into this location"); //Bad grammar is OK - this will be a sentence fragment
        }

        lUploadTargetDOM = createUploadTarget(lUploadContainerDOM, lNodeInfo);
      }
      else {
        lUploadTargetDOM = lUploadContainerDOM;
      }

      //Set the target node ref now we know the container's cardinality
      mUploadTargetNodeRef = lUploadTargetDOM.getRef();

      FileStorageLocation lFSL = mModule.getFileStorageLocation(lNodeInfo.getFoxNamespaceAttribute(NodeAttribute.FILE_STORAGE_LOCATION));
      mWorkingSL = lFSL.createWorkingUploadStorageLocation(lContextUElem, lUploadTargetDOM, mUploadInfo);

      //Reset the target DOM ready for the new upload - don't write the file ID as this is used to indicate that an upload was successful.
      mUploadInfo.serialiseFileMetadataToXML(lUploadTargetDOM, true);
    }
  }

  private static DOM createUploadTarget(DOM pContainerDOM, NodeInfo pNodeInfo) {
    DOMList lChildNodes = pNodeInfo.getModelDOMElem().getChildNodes();
    if(lChildNodes.size() != 1) {
      throw new ExInternal("Multi-upload target in model DOM should have exactly 1 child element");
    }

    return pContainerDOM.addElem(lChildNodes.get(0).getName());
  }

  /**
   * Entry point for upload processing.
   * @param pRequestContext
   * @return JSON response representing upload success or failure.
   */
  public FoxResponse processUpload(RequestContext pRequestContext) {

    try {

      //Start reading the request to get form data
      MultipartUploadReader lMultipartUploadReader = new MultipartUploadReader(pRequestContext.getFoxRequest());
      Track.pushInfo("InitialFormRead");
      try {
        lMultipartUploadReader.readFormData();
      }
      finally {
        Track.pop("InitialFormRead");
      }

      UploadThreadInitialiser lInitialiser = new UploadThreadInitialiser(lMultipartUploadReader);
      UploadLogger lUploadLogger;

      StatefulXThread lXThread = StatefulXThread.getAndLockXThread(pRequestContext, mThreadId);
      try {
        //Validate that an upload is allowed to the target context (decided by FieldSet)
        if(!lXThread.checkUploadAllowed(mUploadContainerContextRef)) {
          throw new ExInternal("Upload to context ref " + mUploadContainerContextRef + " not allowed");
        }

        //Initialise (clean out) the target DOM and get state info from the thread
        lXThread.rampAndRun(pRequestContext, lInitialiser, "UploadInit");

        //Construct a new logger and insert the log start
        lUploadLogger = new UploadLogger(pRequestContext.getRequestApp(), lInitialiser.mWuaId, lInitialiser.mModule.getName(), lInitialiser.mStateName);
        UCon lUCon = pRequestContext.getContextUCon().getUCon("Upload Log Start");
        try {
          lUploadLogger.startLog(lUCon, lInitialiser.mUploadInfo, pRequestContext.getFoxRequest().getHttpRequest().getRemoteAddr());
        }
        finally {
          pRequestContext.getContextUCon().returnUCon(lUCon, "Upload Log Start");
        }

        //Commit the DOM write and upload log
        pRequestContext.getContextUCon().commit(UploadServlet.UPLOAD_CONNECTION_NAME);
      }
      finally {
        //Unlock thread (commits again!)
        StatefulXThread.unlockThread(pRequestContext, lXThread);
      }

      return receiveUpload(pRequestContext, lMultipartUploadReader, lInitialiser.mUploadInfo, lInitialiser.mWorkingSL, lUploadLogger);
    }
    catch(Throwable th) {
      //Catch all error handler - most expected errors should have already been handled gracefully above
      return generateErrorResponse(th, null);
    }
  }

  /**
   * Initialise a WorkDoc LOB locator, streams a file upload into it, and handles storage location completion/finalisation.
   * @param pRequestContext
   * @param pUploadInfo
   * @param pWorkingSL
   * @param pUploadLogger
   * @return JSON response representing upload success or failure.
   */
  private FoxResponse receiveUpload(RequestContext pRequestContext, MultipartUploadReader pMultipartUploadReader, final UploadInfo pUploadInfo,
                                    WorkingUploadStorageLocation pWorkingSL, UploadLogger pUploadLogger) {

    FoxRequest lFoxRequest = pRequestContext.getFoxRequest();
    App lApp = pRequestContext.getRequestApp();

    //Note: Currently only BLOB uploads are supported
    LOBWorkDoc<Blob> lWorkDoc = new WriteableLOBWorkDoc<>(Blob.class, pWorkingSL);
    UploadWorkItem lUploadWorkItem;
    Blob lWFSLUploadBLOB;

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Upload");
    pRequestContext.getContextUCon().startRetainingUCon();

    boolean lAllowLogFailure = true;
    UploadedFileInfo lUploadedFileInfo = null;

    // Try and handle the upload using a new connection
    UPLOAD_AND_STORE_TRY:
    Track.pushInfo("ReceiveUpload");
    try {
      //Re-evaluate binds for upload events etc
      pWorkingSL.reEvaluateFileMetadataBinds();

      //Serialise to upload info log xml
      pUploadLogger.addLogXML("working-file-storage-location", XStreamManager.serialiseObjectToDOM(pWorkingSL));

      // fire upload event for not-started status
      setUploadStatusAndFireEvent(UploadStatus.NOT_STARTED, pUploadInfo, lUCon, pWorkingSL, lWorkDoc);

      // Build the Upload Work Item
      lUploadWorkItem = new UploadWorkItem(lFoxRequest, pUploadInfo);

      lUploadWorkItem.setAttribute("OriginatingApp", pRequestContext.getRequestApp()); // set a reference to the app on upload work item
      lUploadWorkItem.setAttribute("UCon", lUCon);

      setUploadStatusAndFireEvent(UploadStatus.STARTED, pUploadInfo, lUCon, pWorkingSL, lWorkDoc);

      // Prepares the work item to start reading the file
      Track.pushInfo("UploadWorkItemInit");
      try {
        lUploadWorkItem.init(pMultipartUploadReader);
      }
      finally {
        Track.pop("UploadWorkItemInit");
      }

      // Set status to receiving and fire event
      // moved to here for proper status update
      setUploadStatusAndFireEvent(UploadStatus.RECEIVING, pUploadInfo, lUCon, pWorkingSL, lWorkDoc);

      //Savepoint here after not-started -> started -> receiving
      Savepoint lUploadStartSavepoint = lUCon.savepoint("UPLOAD_START");

      //Retrieve the blob pointer to stream upload to.
      //This might perform an INSERT which needs to be rolled back along with the blob write.
      //But if the insert goes wrong the whole upload should fail with no events raised so
      //put it outside the try.

      Track.pushInfo("WorkDocOpen");
      try {
        lWorkDoc.open(lUCon);
      }
      finally {
        Track.pop("WorkDocOpen");
      }
      lWFSLUploadBLOB = lWorkDoc.getLOB();

      //Add BLOB reference to work item now it has been selected for update
      lUploadWorkItem.initBLOB(lWFSLUploadBLOB);

      //In this try we anticipate exceptions such as virus check, content check or broken pipe/timeout errors.
      //These could occur over the course of the upload and we want to capture these and notify the consuming
      //FOX module by raising events on the storage location API. Such API calls should be part of the transaction
      //and committed. However, the BLOB insert and any data written to it needs to be rolled back.
      UPLOAD_TRY:
      Track.pushInfo("DoFileUpload");
      try {
        //Sync on class to ensure static QueueHandler map is populated (might not be during a flush)
        //and that the FileTransfer QueueHandler has not been marked as stale by a flush
        ServiceQueueHandler lFileTransferQueueHandler;
        synchronized(ServiceQueueHandler.class){

          //This will rebuild any queues which could have been jetissoned from the QueueHandler by a flush and not immediately rebuilt
          FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(lApp.getMnemonicName());

          // Get the queue and indicate that we plan to add work to it soon (second param)
          lFileTransferQueueHandler = ServiceQueueHandler.getQueueHandlerByName("FileTransfer", true);
          if (lFileTransferQueueHandler == null) {
            throw new ExInternal("Tried to handle an upload but no FileTransfer queue handler defined.  Ensure the resource master has the relevant service queue entries.");
          }
        }

        //#####################################
        // current thread will suspend in here until it has been given control back by a worker thread
        // this will throw any ExUpload errors we need to deal with
        Track.pushInfo("UploadInQueue");
        try {
          lFileTransferQueueHandler.addItemToQueue(lUploadWorkItem, ServiceQueueHandler.UPLOAD_WORKITEM_TYPE );
        }
        finally {
          Track.pop("UploadInQueue");
        }
        //#####################################


        // When thread regains control ensure that upload work item completed - belt & braces, should never happen
        if (!lUploadWorkItem.isComplete()) {
          throw lUploadWorkItem.mErrorException != null ? lUploadWorkItem.mErrorException : new ExInternal("Upload work item failed to complete successfully. Upload was terminated before storing file.");
        }

        // Status updates retained from old implementation
        setUploadStatusAndFireEvent(UploadStatus.CONTENT_CHECK, pUploadInfo, lUCon, pWorkingSL, lWorkDoc);
        setUploadStatusAndFireEvent(UploadStatus.VIRUS_CHECK, pUploadInfo, lUCon, pWorkingSL, lWorkDoc);
        setUploadStatusAndFireEvent(UploadStatus.SIGNATURE_VERIFY, pUploadInfo, lUCon, pWorkingSL, lWorkDoc);
        setUploadStatusAndFireEvent(UploadStatus.STORING, pUploadInfo, lUCon, pWorkingSL, lWorkDoc);

        Track.pushInfo("WorkDocClose");
        try {
          // Reload evaluated bind variables in the FileWSL to populate the correct metadata (for update statement in close)
          pWorkingSL.reEvaluateFileMetadataBinds();

          //Run update statement on WSL
          lWorkDoc.close(lUCon);
        }
        finally {
          Track.pop("WorkDocClose");
        }

        //Fire completion event
        setUploadStatusAndFireEvent(UploadStatus.COMPLETE, pUploadInfo, lUCon, pWorkingSL, lWorkDoc);

        //Write the completed metadata to the DOM and commit
        lUploadedFileInfo = retrieveDOMWriteMetadataAndCommit(pRequestContext, pUploadInfo, false, pWorkingSL);
      }
      catch (ExUpload e){
        //Catch for any anticipated errors - we can deal with these in a controlled way
        //Rollback to the point before upload started (don't want to roll back all API firing etc)
        lUCon.rollbackTo(lUploadStartSavepoint);
        lAllowLogFailure = false;
        return handleUploadException(pRequestContext, e, pUploadInfo, pWorkingSL, lUCon, lWorkDoc);
      }
      finally {
        Track.pop("DoFileUpload");
      }
    }
    catch (Throwable th){
      //Unexpected error - rollback everything
      try {
        lUCon.rollback();
      }
      catch (ExDB e) {
        Track.recordSuppressedException("Upload failure rollback", th);
      }
      lAllowLogFailure = false;
      return handleUploadException(pRequestContext, th, pUploadInfo, pWorkingSL, lUCon, lWorkDoc);
    }
    finally {
      Track.pop("ReceiveUpload");
      pRequestContext.getContextUCon().returnUCon(lUCon, "Upload");
      logCompletion(pRequestContext, pUploadLogger, pUploadInfo, lAllowLogFailure);
    }

    return generateSuccessResponse(lUploadedFileInfo);
  }

  /**
   * Generates a JSON response for a successful upload, for display using client side JavaScript.
   * @param pUploadedFileInfo
   * @return
   */
  private FoxResponse generateSuccessResponse(UploadedFileInfo pUploadedFileInfo) {
    return new FoxResponseCHAR("text/plain", new StringBuffer(pUploadedFileInfo.asJSONObject().toJSONString()), 0L);
  }

  /**
   * Generates a JSON response based on an error for display using client side JavaScript.
   * @param pError
   * @param pUploadInfo
   * @return
   */
  private FoxResponse generateErrorResponse(Throwable pError, UploadInfo pUploadInfo) {

    JSONObject lErrorJSONResult = new JSONObject();
    //Error object still needs to know the DOM ref so client side deletes can happen
    lErrorJSONResult.put(UploadedFileInfo.DOM_REF_KEY_NAME, mUploadTargetNodeRef);
    lErrorJSONResult.put(UploadedFileInfo.ERROR_MESSAGE_KEY_NAME, getReadableErrorMessage(pError, pUploadInfo));

    if(FoxGlobals.getInstance().canShowStackTracesOnError()) {
      lErrorJSONResult.put("errorStack", XFUtil.getJavaStackTraceInfo(pError));
    }

    if(pUploadInfo != null && pUploadInfo.isForceFailRequested()) {
      lErrorJSONResult.put("errorReason", "cancelled");
    }

    FoxResponseCHAR lErrorResponse = new FoxResponseCHAR("text/plain", new StringBuffer(lErrorJSONResult.toJSONString()), 0L);
    //IMPORTANT: Don't set an error response code (i.e. 500) as this will cause the upload JS library to abort all uploads
    return lErrorResponse;
  }

  /**
   * Generates a user-appropriate error message for display on the client side.
   * @param pError Original error.
   * @param pUploadInfo
   * @return Readable error message.
   */
  static String getReadableErrorMessage(Throwable pError, UploadInfo pUploadInfo) {
    String lFilename;
    if(pUploadInfo != null && !XFUtil.isNull(pUploadInfo.getFilename())) {
      lFilename = pUploadInfo.getFilename();
    }
    else {
      lFilename = "your file";
    }

    //Default error message if we don't have an ExUpload with a better one
    String lReadableMessage = "an unexpected problem occured";

    //Look through the error stack for an ExUpload with a readable message to report to the user
    Throwable lError = pError;
    do {
      if(lError instanceof ExUpload) {
        lReadableMessage = ((ExUpload) lError).getReadableMessage();
        break;
      }
      lError = lError.getCause();
    }
    while(lError != null);

    return "Could not upload " + lFilename + ": " + lReadableMessage + ".";
  }

  /**
   * Updates the UploadInfo to reflect the given exception, writes failure metadata to the target DOM and attempts to
   * close the target LOB.
   * @param pRequestContext
   * @param pException
   * @param pUploadInfo
   * @param pWorkingSL
   * @param pUploadUCon
   * @param pWorkDoc
   * @return An appropriate JSON response containing the error message for display by client JavasSript.
   */
  private final FoxResponse handleUploadException(RequestContext pRequestContext, Throwable pException, UploadInfo pUploadInfo, WorkingUploadStorageLocation pWorkingSL, UCon pUploadUCon, LOBWorkDoc pWorkDoc) {

    Track.pushInfo("HandleUploadError");
    try {
      Track.logAlertText("ErrorStack", XFUtil.getJavaStackTraceInfo(pException));

      UploadStatus lStatusAtCatch = pUploadInfo.getStatus();
      pUploadInfo.failUpload(pException);

      //Re-Run events which will have been rolled back due to the failure
      if(lStatusAtCatch == UploadStatus.CONTENT_CHECK_FAILED || lStatusAtCatch == UploadStatus.VIRUS_CHECK_FAILED){
        setUploadStatusAndFireEvent(UploadStatus.CONTENT_CHECK, pUploadInfo, pUploadUCon, pWorkingSL, pWorkDoc);
      }

      if(lStatusAtCatch == UploadStatus.VIRUS_CHECK_FAILED){
        setUploadStatusAndFireEvent(UploadStatus.VIRUS_CHECK, pUploadInfo, pUploadUCon, pWorkingSL, pWorkDoc);
      }

      //Failure event is last to run
      pUploadInfo.setStatusMsg("A problem occurred uploading the file: " + pException.getMessage() + " If problems persist, contact Technical Support.");
      pUploadInfo.setSystemMsg("Upload ERROR\n" + pException.getMessage() + XFUtil.getJavaStackTraceInfo(pException));
      setUploadStatusAndFireEvent(UploadStatus.FAILED, pUploadInfo, pUploadUCon, pWorkingSL, pWorkDoc);

      //Write diagnostic leg of metadata
      retrieveDOMWriteMetadataAndCommit(pRequestContext, pUploadInfo, true, pWorkingSL);

      //Attempt to clean up the WorkDoc LOB, ignoring any problems
      try {
        pWorkDoc.closeOnError();
      }
      catch (Throwable th) {
        Track.recordSuppressedException("Upload WorkDoc close on error", th);
      }

      return generateErrorResponse(pException, pUploadInfo);
    }
    finally {
      Track.pop("HandleUploadError");
    }
  }

  /**
   * Writes completion information to the UploadLogger. This must be performed at the end of every upload regardless
   * of whether it was successful.
   * @param pRequestContext
   * @param pUploadLogger
   * @param pUploadInfo
   * @param pAllowFailure If true, any failures caused by writing the upload log will be propogated out of this method.
   * Otherwise, all errors will be suppressed.
   */
  private void logCompletion(RequestContext pRequestContext, UploadLogger pUploadLogger, UploadInfo pUploadInfo, boolean pAllowFailure) {

    Track.pushInfo("LogUploadCompletion");
    try {
      pRequestContext.getContextUCon().pushConnection("LOG_UPLOAD_COMPLETE");
      try {
        //Log upload metadata to the log table
        pUploadLogger.addLogXML("file-upload-metadata", pUploadInfo.getMetadataDOM());

        UCon lLogUCon = pRequestContext.getContextUCon().getUCon("Log Upload Complete");
        try {
          pUploadLogger.updateLog(lLogUCon, pUploadInfo);
          lLogUCon.commit();
        }
        catch (ExServiceUnavailable e) {
          throw new ExInternal("Failed to commit log update");
        }
        finally {
          pRequestContext.getContextUCon().returnUCon(lLogUCon, "Log Upload Complete");
        }
      }
      finally {
        pRequestContext.getContextUCon().popConnection("LOG_UPLOAD_COMPLETE");
      }
    }
    catch (Throwable th) {
      if(pAllowFailure) {
        throw new ExInternal("Error updating upload log", th);
      }
      else {
        Track.recordSuppressedException("Update upload log failure", th);
      }
    }
    finally {
      Track.pop("LogUploadCompletion");
    }
  }

  /**
   * Locks the upload thread, writes the upload metadata to the relevant DOM location and commits. Also registers a new download
   * on the thread's download manager so the uploaded file can be downloaded immediately.
   * @param pRequestContext
   * @param pUploadInfo
   * @param pWasError
   * @param pWFSL
   * @return UploadedFileInfo if the upload was successful, or null if not. This can be passed back to the client side
   * JS as a JSON object.
   */
  private UploadedFileInfo retrieveDOMWriteMetadataAndCommit(RequestContext pRequestContext, final UploadInfo pUploadInfo, final boolean pWasError, final WorkingFileStorageLocation pWFSL) {

    final String LOCK_CONNECTION_NAME = "LOCK_THREAD";
    final String lUploadTargetNodeRef = mUploadTargetNodeRef;

    class ThreadUpdater implements RampedThreadRunnable {
      UploadedFileInfo mUploadedFileInfo;
      public void run(ActionRequestContext pRequestContext) {
        //Note: this will cause a failure if the upload target cannot be found
        DOM lTargetDOM = pRequestContext.getContextUElem().getElemByRef(mUploadTargetNodeRef);

        if(!pWasError) {
          pUploadInfo.serialiseFileMetadataToXML(lTargetDOM, false);

          //Regenerate a WFSL for downloading (this will also contain the latest binds for the cache key)
          WorkingFileStorageLocation lDownloadWSL = pWFSL.getStorageLocation().createWorkingStorageLocationForUploadDownload(pRequestContext.getContextUElem(), lTargetDOM);

          mUploadedFileInfo = pRequestContext.getDownloadManager().addFileDownload(pRequestContext, lDownloadWSL, lUploadTargetNodeRef, pUploadInfo);
        }
        else {
          pUploadInfo.serialiseDiagnosticFileMetadataToXML(lTargetDOM);
        }
      }
    }

    ContextUCon lContextUCon = pRequestContext.getContextUCon();
    lContextUCon.pushRetainedConnection(LOCK_CONNECTION_NAME);
    StatefulXThread lXThread;
    try {
      lXThread = StatefulXThread.getAndLockXThread(pRequestContext, mThreadId);
    }
    finally {
      lContextUCon.popConnection(LOCK_CONNECTION_NAME);
    }

    try {
      ThreadUpdater lThreadUpdater = new ThreadUpdater();
      lXThread.rampAndRun(pRequestContext, lThreadUpdater, "UploadComplete");
      lContextUCon.commit(UploadServlet.UPLOAD_CONNECTION_NAME);

      return lThreadUpdater.mUploadedFileInfo;
    }
    finally {
      lContextUCon.pushConnection(LOCK_CONNECTION_NAME);
      try {
        StatefulXThread.unlockThread(pRequestContext, lXThread);
      }
      finally {
        lContextUCon.popConnection(LOCK_CONNECTION_NAME);
      }
    }
  }

  private void setUploadStatusAndFireEvent(UploadStatus pNewStatus, UploadInfo pUploadInfo, UCon pUCon, WorkingUploadStorageLocation pWorkingSL, WorkDoc pWorkDoc) {
    pUploadInfo.setStatus(pNewStatus);

    ExecutableAPI lAPIStatement = pWorkingSL.getExecutableAPIStatementOrNull(pWorkDoc);
    if(lAPIStatement != null) {
      try {
        lAPIStatement.executeAndClose(pUCon);
      }
      catch (Throwable th) {
        throw new ExInternal("Error executing fm:api at stage " + pNewStatus + " for upload to " + pWorkingSL.getStorageLocationName(), th);
      }
    }
  }
}
