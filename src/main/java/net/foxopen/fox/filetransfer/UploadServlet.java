package net.foxopen.fox.filetransfer;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.CookieBasedFoxSession;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.servlets.EntryPointServlet;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUpload;
import net.foxopen.fox.module.UploadedFileInfo;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.util.SizeUtil;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletResponse;


public class UploadServlet
extends EntryPointServlet {

  public static final String THREAD_ID_PARAM_NAME = "thread_id";
  public static final String CALL_ID_PARAM_NAME = "call_id";
  public static final String CONTEXT_REF_PARAM_NAME = "context_ref";

  public static final String UPLOAD_SERVLET_PATH = "upload";

  static final String UPLOAD_CONNECTION_NAME = "FileUpload";
  static final String UPLOAD_INFO_ID_PARAM = "uploadInfoId";

  @Override
  protected String getContextUConInitialConnectionName() {
    return UPLOAD_CONNECTION_NAME;
  }

  @Override
  public FoxSession establishFoxSession(RequestContext pRequestContext) {
    return CookieBasedFoxSession.getOrCreateFoxSession(pRequestContext);
  }

  @Override
  protected String getTrackElementName(RequestContext pRequestContext) {
    return "UploadRequest";
  }

  private FoxResponse processStart(RequestContext pRequestContext) {

    FoxRequest lFoxRequest = pRequestContext.getFoxRequest();
    try {
      String lThreadId = lFoxRequest.getParameter(THREAD_ID_PARAM_NAME).trim();
      String lCallId = lFoxRequest.getParameter(CALL_ID_PARAM_NAME).trim();
      String lContextRef = lFoxRequest.getParameter(CONTEXT_REF_PARAM_NAME).trim();

      UploadInitialiser lUploadInitialiser = new UploadInitialiser(lThreadId, lCallId, lContextRef);

      UploadInfo lUploadInfo = lUploadInitialiser.startUpload(pRequestContext);

      JSONObject lResponseObject = new JSONObject();
      lResponseObject.put(UPLOAD_INFO_ID_PARAM, lUploadInfo.getUploadInfoId());
      lResponseObject.put(UploadedFileInfo.DOM_REF_KEY_NAME, lUploadInfo.getTargetContextRef());

      //Don't send application/json as IE8 can't deal with it
      return new FoxResponseCHAR("text/plain", new StringBuffer(lResponseObject.toJSONString()), 0L);
    }
    catch (Throwable th) {
      return generateErrorResponse(th, null, true);
    }
  }

  private FoxResponse processReceive(RequestContext pRequestContext) {

    UploadProcessor lUploadProcessor;
    try {
      FoxRequest lFoxRequest = pRequestContext.getFoxRequest();
      // Check that we have a multipart file upload request
      if (!ServletFileUpload.isMultipartContent(lFoxRequest.getHttpRequest())) {
        throw new ExInternal("File upload must be a multipart request");
      }

      lUploadProcessor = new UploadProcessor(pRequestContext);
    }
    catch (Throwable th) {
      //Catch errors caused by an invalid request, missing UploadInfo, etc
      return generateErrorResponse(th, null, true);
    }

    //Start receiving the upload
    //UploadProcessor does its own error handling
    return lUploadProcessor.processUpload(pRequestContext);
  }

  private UploadInfo getUploadInfo(FoxRequest pFoxRequest) {
    String lUploadInfoId = pFoxRequest.getParameter(UPLOAD_INFO_ID_PARAM);
    return getUploadInfo(lUploadInfoId);
  }

  static UploadInfo getUploadInfo(String pUploadInfoId) {
    if(XFUtil.isNull(pUploadInfoId)) {
      throw new ExInternal("uploadInfoId cannot be null");
    }

    return UploadInfo.getUploadInfoCache().get(pUploadInfoId);
  }

  /**
   * Generates a JSON response based on an error for display using client side JavaScript.
   * @param pError
   * @param pUploadInfo
   * @param pSetErrorCode
   * @return
   */
  static FoxResponse generateErrorResponse(Throwable pError, UploadInfo pUploadInfo, boolean pSetErrorCode) {

    JSONObject lErrorJSONResult = new JSONObject();
    lErrorJSONResult.put(UploadedFileInfo.ERROR_MESSAGE_KEY_NAME, getReadableErrorMessage(pError, pUploadInfo));

    if(FoxGlobals.getInstance().canShowStackTracesOnError()) {
      lErrorJSONResult.put("errorStack", XFUtil.getJavaStackTraceInfo(pError));
    }

    if(pUploadInfo != null) {
      if(pUploadInfo.isForceFailRequested()) {
        lErrorJSONResult.put("errorReason", "cancelled");
      }

      pUploadInfo.cleanupAfterError();
    }

    //Don't send application/json as IE8 can't deal with it
    FoxResponseCHAR lErrorResponse = new FoxResponseCHAR("text/plain", new StringBuffer(lErrorJSONResult.toJSONString()), 0L);
    if(pSetErrorCode) {
      lErrorResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
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
    String lReadableMessage = "an unexpected problem occurred";

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

  private FoxResponse processStatus(FoxRequest pFoxRequest) {

    UploadInfo lUploadInfo = getUploadInfo(pFoxRequest);
    JSONObject lStatusObject = new JSONObject();

    if(lUploadInfo != null) {
      String lStatus = lUploadInfo.getStatusMsg();

      lStatusObject.put("statusText", lStatus);

      if(lUploadInfo.getStatus() != UploadStatus.NOT_STARTED && lUploadInfo.getStatus() != UploadStatus.STARTED && lUploadInfo.getStatus() != UploadStatus.FAILED) {
        //Upload info only has transfer process when the status is RECEIVING or later
        lStatusObject.put("percentComplete", lUploadInfo.getTransferProgress());
      }
      else {
        lStatusObject.put("percentComplete", 0);
      }

      lStatusObject.put("uploadSpeed", SizeUtil.getBytesSpecificationDescription(lUploadInfo.getTransferRateBytesPerSecond()) + "/sec");
      lStatusObject.put("timeRemaining", lUploadInfo.calculateTimeRemaining());

      if(lUploadInfo.getStatus() == UploadStatus.FAILED) {
        lStatusObject.put(UploadedFileInfo.ERROR_MESSAGE_KEY_NAME, lUploadInfo.getReadableErrorMessage());
      }

      //Don't send application/json as IE8 can't deal with it
      return new FoxResponseCHAR("text/plain", new StringBuffer(lStatusObject.toJSONString()), 0L);
    }
    else {
      return generateErrorResponse(new ExInternal("Failed to locate UploadInfo in cache"), null, true);
    }
  }

  private FoxResponse processCancel(FoxRequest pFoxRequest) {

    UploadInfo lUploadInfo = getUploadInfo(pFoxRequest);
    String lMessage;
    if(lUploadInfo == null) {
      lMessage = "CANCEL FAILED - NO UPLOAD INFO";
    }
    else {
      String lFailParam = pFoxRequest.getHttpRequest().getParameter("reason");
      UploadInfo.ForceFailReason lFailReason = UploadInfo.ForceFailReason.fromURLParam(lFailParam);

      //Check ignores duplicate request from IE
      if(lUploadInfo.getStatus().isInProgress()){
        lUploadInfo.forceFailUpload(lFailReason);
      }

      lMessage = "CANCEL OK";
    }

    //Don't send application/json as IE8 can't deal with it
    return new FoxResponseCHAR("text/plain", new StringBuffer(lMessage), 0L);
  }

  @Override
  public void processGet(RequestContext pRequestContext) {
    processRequest(pRequestContext);
  }

  @Override
  public void processPost(RequestContext pRequestContext) {
    processRequest(pRequestContext);
  }

  protected void processRequest(RequestContext pRequestContext) {
    String lURL = pRequestContext.getFoxRequest().getHttpRequest().getRequestURL().toString();

    FoxResponse lResponse;
    if(lURL.endsWith("/start")) {
      lResponse = processStart(pRequestContext);
    }
    else if(lURL.endsWith("/receive")) {
      lResponse = processReceive(pRequestContext);
    }
    else if(lURL.endsWith("/status")) {
      lResponse = processStatus(pRequestContext.getFoxRequest());
    }
    else if(lURL.endsWith("/cancel")) {
      lResponse = processCancel(pRequestContext.getFoxRequest());
    }
    else {
      throw new ExInternal("Don't know how to handle URL " + lURL);
    }

    lResponse.respond(pRequestContext.getFoxRequest());
  }
}
