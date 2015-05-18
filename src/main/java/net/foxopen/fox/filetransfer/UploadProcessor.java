package net.foxopen.fox.filetransfer;


import net.foxopen.fox.App;
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
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUpload;
import net.foxopen.fox.module.UploadedFileInfo;
import net.foxopen.fox.queue.ServiceQueueHandler;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RampedThreadRunnable;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.ThreadLockManager;
import net.foxopen.fox.thread.persistence.xstream.XStreamManager;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;
import net.foxopen.fox.thread.storage.WorkingUploadStorageLocation;
import net.foxopen.fox.track.Track;

import java.sql.Blob;
import java.sql.Savepoint;


/**
 * Object for handling the lifecycle of a single file upload.
 */
public class UploadProcessor {

  private final UploadInfo mUploadInfo;
  private final MultipartUploadReader mMultipartUploadReader;

  public UploadProcessor(RequestContext pRequestContext) {

    //Start reading the request to get form data
    mMultipartUploadReader = new MultipartUploadReader(pRequestContext.getFoxRequest());
    Track.pushInfo("InitialFormRead");
    try {
      mMultipartUploadReader.readFormData();
    }
    finally {
      Track.pop("InitialFormRead");
    }

    String lUploadInfoId = mMultipartUploadReader.getFormFieldValue(UploadServlet.UPLOAD_INFO_ID_PARAM);
    mUploadInfo = UploadServlet.getUploadInfo(lUploadInfoId);
    if(mUploadInfo == null) {
      throw new ExInternal("UploadInfo " + lUploadInfoId + " was not found in cache");
    }
  }

  /**
   * Entry point for upload processing.
   * @param pRequestContext
   * @return JSON response representing upload success or failure.
   */
  public FoxResponse processUpload(RequestContext pRequestContext) {
    try {
      return receiveUpload(pRequestContext);
    }
    catch (Throwable th) {
      //Catch all error handler - most expected errors should have already been handled gracefully above
      return UploadServlet.generateErrorResponse(th, mUploadInfo, true);
    }
  }

  /**
   * Initialise a WorkDoc LOB locator, streams a file upload into it, and handles storage location completion/finalisation.
   * @return JSON response representing upload success or failure.
   */
  FoxResponse receiveUpload(RequestContext pRequestContext) {

    if(mUploadInfo.getStatus() != UploadStatus.NOT_STARTED) {
      throw new ExInternal("Cannot start an upload when UploadInfo status is " + mUploadInfo.getStatus());
    }

    FoxRequest lFoxRequest = pRequestContext.getFoxRequest();
    App lApp = pRequestContext.getRequestApp();

    WorkingUploadStorageLocation lWorkingSL = mUploadInfo.getWorkingSL();
    UploadLogger lUploadLogger = mUploadInfo.getUploadLogger();

    //Note: Currently only BLOB uploads are supported
    LOBWorkDoc<Blob> lWorkDoc = new WriteableLOBWorkDoc<>(Blob.class, lWorkingSL);
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
      lWorkingSL.reEvaluateFileMetadataBinds();

      //Serialise to upload info log xml
      lUploadLogger.addLogXML("working-file-storage-location", XStreamManager.serialiseObjectToDOM(lWorkingSL));

      // fire upload event for not-started status
      setUploadStatusAndFireEvent(UploadStatus.NOT_STARTED, lUCon, lWorkingSL, lWorkDoc);

      // Build the Upload Work Item
      lUploadWorkItem = new UploadWorkItem(lFoxRequest, mUploadInfo);

      lUploadWorkItem.setAttribute("OriginatingApp", pRequestContext.getRequestApp()); // set a reference to the app on upload work item
      lUploadWorkItem.setAttribute("UCon", lUCon);

      setUploadStatusAndFireEvent(UploadStatus.STARTED, lUCon, lWorkingSL, lWorkDoc);

      // Prepares the work item to start reading the file
      Track.pushInfo("UploadWorkItemInit");
      try {
        lUploadWorkItem.init(mMultipartUploadReader);
      }
      finally {
        Track.pop("UploadWorkItemInit");
      }

      // Set status to receiving and fire event
      // moved to here for proper status update
      setUploadStatusAndFireEvent(UploadStatus.RECEIVING, lUCon, lWorkingSL, lWorkDoc);

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
        setUploadStatusAndFireEvent(UploadStatus.CONTENT_CHECK, lUCon, lWorkingSL, lWorkDoc);
        setUploadStatusAndFireEvent(UploadStatus.VIRUS_CHECK, lUCon, lWorkingSL, lWorkDoc);
        setUploadStatusAndFireEvent(UploadStatus.SIGNATURE_VERIFY, lUCon, lWorkingSL, lWorkDoc);
        setUploadStatusAndFireEvent(UploadStatus.STORING, lUCon, lWorkingSL, lWorkDoc);

        Track.pushInfo("WorkDocClose");
        try {
          // Reload evaluated bind variables in the FileWSL to populate the correct metadata (for update statement in close)
          lWorkingSL.reEvaluateFileMetadataBinds();

          //Run update statement on WSL
          lWorkDoc.close(lUCon);
        }
        finally {
          Track.pop("WorkDocClose");
        }

        //Fire completion event
        setUploadStatusAndFireEvent(UploadStatus.COMPLETE, lUCon, lWorkingSL, lWorkDoc);

        //Write the completed metadata to the DOM and commit
        lUploadedFileInfo = retrieveDOMWriteMetadataAndCommit(pRequestContext, false, lWorkingSL);
      }
      catch (ExUpload e){
        //Catch for any anticipated errors - we can deal with these in a controlled way
        //Rollback to the point before upload started (don't want to roll back all API firing etc)
        lUCon.rollbackTo(lUploadStartSavepoint);
        lAllowLogFailure = false;
        return handleUploadException(pRequestContext, e, lWorkingSL, lUCon, lWorkDoc);
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
        Track.recordSuppressedException("Upload failure rollback", e);
      }
      lAllowLogFailure = false;
      return handleUploadException(pRequestContext, th, lWorkingSL, lUCon, lWorkDoc);
    }
    finally {
      Track.pop("ReceiveUpload");
      pRequestContext.getContextUCon().returnUCon(lUCon, "Upload");
      logCompletion(pRequestContext, lUploadLogger, lAllowLogFailure);
    }

    //Remove the UploadInfo from the cache so we're not retaining it unnecessarily (note: it needs to hang around if the upload failed)
    UploadInfo.getUploadInfoCache().remove(mUploadInfo.getUploadInfoId());

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
   * Updates the UploadInfo to reflect the given exception, writes failure metadata to the target DOM and attempts to
   * close the target LOB.
   * @param pRequestContext
   * @param pException
   * @param pWorkingSL
   * @param pUploadUCon
   * @param pWorkDoc
   * @return An appropriate JSON response containing the error message for display by client JavasSript.
   */
  private FoxResponse handleUploadException(RequestContext pRequestContext, Throwable pException, WorkingUploadStorageLocation pWorkingSL, UCon pUploadUCon, LOBWorkDoc pWorkDoc) {

    Track.pushInfo("HandleUploadError");
    try {
      Track.logAlertText("ErrorStack", XFUtil.getJavaStackTraceInfo(pException));

      UploadStatus lStatusAtCatch = mUploadInfo.getStatus();
      mUploadInfo.failUpload(pException);

      //Re-Run events which will have been rolled back due to the failure
      if(lStatusAtCatch == UploadStatus.CONTENT_CHECK_FAILED || lStatusAtCatch == UploadStatus.VIRUS_CHECK_FAILED){
        setUploadStatusAndFireEvent(UploadStatus.CONTENT_CHECK, pUploadUCon, pWorkingSL, pWorkDoc);
      }

      if(lStatusAtCatch == UploadStatus.VIRUS_CHECK_FAILED){
        setUploadStatusAndFireEvent(UploadStatus.VIRUS_CHECK, pUploadUCon, pWorkingSL, pWorkDoc);
      }

      //Failure event is last to run
      mUploadInfo.setStatusMsg("A problem occurred uploading the file: " + pException.getMessage() + " If problems persist, contact Technical Support.");
      mUploadInfo.setSystemMsg("Upload ERROR\n" + pException.getMessage() + XFUtil.getJavaStackTraceInfo(pException));
      setUploadStatusAndFireEvent(UploadStatus.FAILED, pUploadUCon, pWorkingSL, pWorkDoc);

      //Write diagnostic leg of metadata
      retrieveDOMWriteMetadataAndCommit(pRequestContext, true, pWorkingSL);

      //Attempt to clean up the WorkDoc LOB, ignoring any problems
      try {
        pWorkDoc.closeOnError();
      }
      catch (Throwable th) {
        Track.recordSuppressedException("Upload WorkDoc close on error", th);
      }

      return UploadServlet.generateErrorResponse(pException, mUploadInfo, true);
    }
    finally {
      Track.pop("HandleUploadError");
    }
  }

  /**
   * Writes completion information to the UploadLogger. This must be performed at the end of every upload regardless
   * of whether it was successful.
   * @param pUploadInfo
   * @param pRequestContext
   * @param pUploadLogger
   * @param pAllowFailure If true, any failures caused by writing the upload log will be propogated out of this method.
   */
  private void logCompletion(RequestContext pRequestContext, UploadLogger pUploadLogger, boolean pAllowFailure) {

    Track.pushInfo("LogUploadCompletion");
    try {
      pRequestContext.getContextUCon().pushConnection("LOG_UPLOAD_COMPLETE");
      try {
        //Log upload metadata to the log table
        pUploadLogger.addLogXML("file-upload-metadata", mUploadInfo.getMetadataDOM());

        UCon lLogUCon = pRequestContext.getContextUCon().getUCon("Log Upload Complete");
        try {
          pUploadLogger.updateLog(lLogUCon, mUploadInfo);
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
   * @param pWasError
   * @param pWFSL
   * @return UploadedFileInfo if the upload was successful, or null if not. This can be passed back to the client side
   * JS as a JSON object.
   */
  private UploadedFileInfo retrieveDOMWriteMetadataAndCommit(RequestContext pRequestContext, final boolean pWasError, final WorkingFileStorageLocation pWFSL) {

    final String lUploadTargetNodeRef = mUploadInfo.getTargetContextRef();

    class ThreadUpdater implements RampedThreadRunnable {
      UploadedFileInfo mUploadedFileInfo;
      public void run(ActionRequestContext pRequestContext) {
        //Note: this will cause a failure if the upload target cannot be found
        DOM lTargetDOM = pRequestContext.getContextUElem().getElemByRef(lUploadTargetNodeRef);

        if(!pWasError) {
          mUploadInfo.serialiseFileMetadataToXML(lTargetDOM, false);

          //Regenerate a WFSL for downloading (this will also contain the latest binds for the cache key)
          WorkingFileStorageLocation lDownloadWSL = pWFSL.getStorageLocation().createWorkingStorageLocationForUploadDownload(pRequestContext.getContextUElem(), lTargetDOM);

          mUploadedFileInfo = pRequestContext.getDownloadManager().addFileDownload(pRequestContext, lDownloadWSL, lUploadTargetNodeRef, mUploadInfo);
        }
        else {
          mUploadInfo.serialiseDiagnosticFileMetadataToXML(lTargetDOM);
        }
      }
    }

    ThreadUpdater lThreadUpdater = new ThreadUpdater();
    //Create a new ThreadLockManager which uses a separate connection to lock the thread - don't want to commit the main connection (with the upload on)
    ThreadLockManager<Object> lThreadLockManager = new ThreadLockManager<>(mUploadInfo.getThreadId(), UploadServlet.UPLOAD_CONNECTION_NAME, true);
    //Lock thread, update the DOM and get the resultant UploadedFileInfo - this also commits the upload
    lThreadLockManager.lockRampAndRun(pRequestContext, "UploadComplete", lThreadUpdater);

    return lThreadUpdater.mUploadedFileInfo;
  }

  private void setUploadStatusAndFireEvent(UploadStatus pNewStatus, UCon pUCon, WorkingUploadStorageLocation pWorkingSL, WorkDoc pWorkDoc) {
    mUploadInfo.setStatus(pNewStatus);

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
