package net.foxopen.fox.filetransfer;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.entrypoint.CookieBasedFoxSession;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.servlets.EntryPointServlet;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.util.SizeUtil;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.JSONObject;


public class UploadServlet
extends EntryPointServlet {

  public static final String THREAD_ID_PARAM_NAME = "thread_id";
  public static final String CALL_ID_PARAM_NAME = "call_id";
  public static final String CONTEXT_REF_PARAM_NAME = "context_ref";

  public static final String UPLOAD_SERVLET_PATH = "upload";

  static final String UPLOAD_CONNECTION_NAME = "FileUpload";

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
    String lThreadId = lFoxRequest.getParameter(THREAD_ID_PARAM_NAME).trim();
    try {
      // Check that we have a multipart file upload request
      if (!ServletFileUpload.isMultipartContent(lFoxRequest.getHttpRequest())) {
        throw new ExInternal("File upload must be a multipart request");
      }

      String lCallId = lFoxRequest.getParameter(CALL_ID_PARAM_NAME).trim();
      String lContextRef = lFoxRequest.getParameter(CONTEXT_REF_PARAM_NAME).trim();

      UploadProcessor lUploadProcessor = new UploadProcessor(lThreadId, lCallId, lContextRef);

      return lUploadProcessor.processUpload(pRequestContext);
    }
    finally {
      pRequestContext.getContextUCon().closeAllRetainedConnections();
    }
  }

  private UploadInfo getUploadInfo(FoxRequest pFoxRequest) {
    String lThreadId = pFoxRequest.getHttpRequest().getParameter(THREAD_ID_PARAM_NAME);
    String lCallId = pFoxRequest.getHttpRequest().getParameter(CALL_ID_PARAM_NAME);
    String lContextRef = pFoxRequest.getHttpRequest().getParameter(CONTEXT_REF_PARAM_NAME);

    return UploadInfoCache.getUploadInfo(lThreadId, lCallId, lContextRef);
  }

  private FoxResponse processStatus(FoxRequest pFoxRequest) {

    UploadInfo lUploadInfo = getUploadInfo(pFoxRequest);
    JSONObject lStatusObject = new JSONObject();

    if(lUploadInfo != null) {
      String lStatus = lUploadInfo.getStatusMsg();

      lStatusObject.put("statusText", lStatus);

      if(lUploadInfo.getStatus() != UploadStatus.NOT_STARTED && lUploadInfo.getStatus() != UploadStatus.STARTED) {
        //Upload info only has transfer process when the status is RECEIVING or later
        lStatusObject.put("percentComplete", lUploadInfo.getTransferProgress());
      }
      else {
        lStatusObject.put("percentComplete", 0);
      }

      lStatusObject.put("uploadSpeed", SizeUtil.getBytesSpecificationDescription(lUploadInfo.getTransferRateBytesPerSecond()) + "/sec");
      lStatusObject.put("timeRemaining", lUploadInfo.calculateTimeRemaining());
    }
    else {
      Track.alert("Upload status request did not find an UploadInfo in cache");
      lStatusObject.put("statusText", "unknown");
    }

    return new FoxResponseCHAR("text/plain", new StringBuffer(lStatusObject.toJSONString()), 0L);
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
