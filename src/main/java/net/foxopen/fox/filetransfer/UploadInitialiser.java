package net.foxopen.fox.filetransfer;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FileUploadType;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUpload;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoFileItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeEvaluationContext;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RampedThreadRunnable;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.ThreadLockManager;
import net.foxopen.fox.thread.storage.FileStorageLocation;

/**
 * Object for starting an upload, including setting up the target DOM on the thread and creating an UploadInfo for managing
 * the upload.
 */
public class UploadInitialiser {

  private final String mThreadId;
  private final String mCallId;
  /** The node which will "contain" this file upload. This is also the target node for single uploads. */
  private final String mUploadContainerContextRef;

  UploadInitialiser(String pThreadId, String pCallId, String pUploadContainerContextRef) {
    mThreadId = pThreadId;
    mCallId = pCallId;
    mUploadContainerContextRef = pUploadContainerContextRef;
  }

  /**
   * Locks/authenticates the thread, resolves the upload target DOM, constructs an UploadInfo and starts the upload log.
   * This method does not perform any error handling.
   * @param pRequestContext Current RequestContext.
   * @return New UploadInfo for managing the upload.
   */
  public UploadInfo startUpload(RequestContext pRequestContext) {

    final UploadThreadInitialiser lInitialiser = new UploadThreadInitialiser(pRequestContext.getFoxRequest());

    ThreadLockManager<UploadInfo> lThreadLockManager = new ThreadLockManager<>(mThreadId, UploadServlet.UPLOAD_CONNECTION_NAME, false);
    return lThreadLockManager.lockAndPerformAction(pRequestContext, new ThreadLockManager.LockedThreadRunnable<UploadInfo>() {
      @Override
      public UploadInfo doWhenLocked(RequestContext pRequestContext, StatefulXThread pXThread) {

        //Validate that an upload is allowed to the target context (decided by FieldSet)
        if (!pXThread.checkUploadAllowed(mUploadContainerContextRef)) {
          throw new ExInternal("Upload to context ref " + mUploadContainerContextRef + " not allowed");
        }

        //Initialise (clean out) the target DOM and get state info from the thread
        pXThread.rampAndRun(pRequestContext, lInitialiser, "UploadInit");

        return lInitialiser.mUploadInfo;
      }
    });
  }

  private static DOM createUploadTarget(DOM pContainerDOM, NodeInfo pNodeInfo) {
    DOMList lChildNodes = pNodeInfo.getModelDOMElem().getChildNodes();
    if(lChildNodes.size() != 1) {
      throw new ExInternal("Multi-upload target in model DOM should have exactly 1 child element");
    }

    return pContainerDOM.addElem(lChildNodes.get(0).getName());
  }

  private class UploadThreadInitialiser implements RampedThreadRunnable {

    private final FoxRequest mFoxRequest;
    private UploadInfo mUploadInfo;

    private UploadThreadInitialiser(FoxRequest pFoxRequest) {
      mFoxRequest = pFoxRequest;
    }

    @Override
    public void run(ActionRequestContext pRequestContext) {
      //Run client actions on the thread to ensure all deletion requests are processed before anything else happens
      String lClientActionJSON = mFoxRequest.getParameter("clientActions");
      if(!XFUtil.isNull(lClientActionJSON)) {
        pRequestContext.applyClientActions(lClientActionJSON);
      }

      Mod lModule = pRequestContext.getCurrentModule();
      ContextUElem lContextUElem = pRequestContext.getContextUElem();

      DOM lUploadContainerDOM = lContextUElem.getElemByRef(mUploadContainerContextRef);

      NodeInfo lNodeInfo = lModule.getNodeInfo(lUploadContainerDOM);

      //If the upload target is a multi-upload node, create a new list element to be the target DOM - otherwise the target is the container
      DOM lUploadTargetDOM;

      //Branch multi/single upload logic based on whether the target node is a list container
      if(EvaluatedNodeInfoFileItem.isMultiUploadTarget(lNodeInfo)) {
        //Multi upload - check that we don't already have enough files
        int lUploadMaxFiles = EvaluatedNodeInfoFileItem.maxFilesAllowed(lContextUElem, lNodeInfo, lUploadContainerDOM);
        if(lUploadContainerDOM.getChildNodes().size() >= lUploadMaxFiles) {
          throw new ExUpload("you cannot upload any more files into this location"); //Bad grammar is OK - this will be a sentence fragment
        }

        lUploadTargetDOM = createUploadTarget(lUploadContainerDOM, lNodeInfo);
      }
      else {
        //Single upload - no validation required
        lUploadTargetDOM = lUploadContainerDOM;
      }

      //Construct a new logger (log will be started when UploadInfo is constructed)
      String lStateName = pRequestContext.getCurrentState().getName();
      String lWuaId = pRequestContext.getAuthenticationContext().getAuthenticatedUser().getAccountID();
      UploadLogger lUploadLogger = new UploadLogger(pRequestContext.getRequestApp(), lWuaId, lModule.getName(), lStateName);

      //Construct new UploadInfo for handling the upload
      FileStorageLocation lFSL = lModule.getFileStorageLocation(lNodeInfo.getFoxNamespaceAttribute(NodeAttribute.FILE_STORAGE_LOCATION));
      FileUploadType lFileUploadType = getFileUploadType(pRequestContext, lUploadContainerDOM, lNodeInfo);

      mUploadInfo = UploadInfo.createUploadInfo(pRequestContext, lUploadTargetDOM, lFSL, lFileUploadType, mThreadId, mCallId, lUploadLogger);

      //Reset the target DOM ready for the new upload - don't write the file ID as this is used to indicate that an upload was successful.
      mUploadInfo.serialiseFileMetadataToXML(lUploadTargetDOM, true);

      //Start upload request log
      UCon lUCon = pRequestContext.getContextUCon().getUCon("Upload Log Start");
      try {
        lUploadLogger.startLog(lUCon, mUploadInfo, pRequestContext.getFoxRequest().getHttpRequest().getRemoteAddr());
      }
      finally {
        pRequestContext.getContextUCon().returnUCon(lUCon, "Upload Log Start");
      }
    }
  }

  private static FileUploadType getFileUploadType(ActionRequestContext pRequestContext, DOM pUploadContainerDOM, NodeInfo pNodeInfo) {

    String lFileUploadTypeAttr = pNodeInfo.getFoxNamespaceAttribute(NodeAttribute.UPLOAD_FILE_TYPE);
    String lXPathResult = "";
    if(lFileUploadTypeAttr != null) {
      try {
        DOM lEvalContext = NodeEvaluationContext.establishEvaluateContextRuleNode(pUploadContainerDOM, pNodeInfo);
        lXPathResult = pRequestContext.getContextUElem().extendedStringOrXPathString(lEvalContext, lFileUploadTypeAttr);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate file upload type attribute", e);
      }
    }

    //Will use app default if XPath was null or returned empty string
    return pRequestContext.getRequestApp().getFileUploadType(lXPathResult);
  }
}
