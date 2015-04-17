package net.foxopen.fox.download;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseByteStream;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.entrypoint.CookieBasedFoxSession;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.servlets.EntryPointServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.ThreadActionResultGenerator;
import net.foxopen.fox.thread.ThreadLockManager;
import net.foxopen.fox.thread.RampedThreadRunnable;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackProperty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


public class DownloadServlet
extends EntryPointServlet {

  public static final String DOWNLOAD_MODE_PARAM_NAME = "mode";
  public static final DownloadMode DEFAULT_DOWNLOAD_MODE = DownloadMode.ATTACHMENT;

  private static final String SERVLET_PATH = "download";
  private static final String CONNECTION_NAME = "DOWNLOAD";

  private static final String PARCEL_DOWNLOAD_TYPE = "parcel";
  private static final String ACTION_DOWNLOAD_TYPE = "action";

  private static final String THREAD_ID_URI_PARAM = "thread_id";
  private static final String PARCEL_ID_URI_PARAM = "parcel_id";
  private static final String FILENAME_URI_PARAM = "filename";
  private static final String ACTION_NAME_URI_PARAM = "action_name";
  private static final String CONTEXT_REF_URI_PARAM = "context_ref";

  private static final PathParamTemplate PARCEL_PATH_TEMPLATE = new PathParamTemplate("/" + PARCEL_DOWNLOAD_TYPE + "/{" + THREAD_ID_URI_PARAM + "}/{" + PARCEL_ID_URI_PARAM + "}/{" + FILENAME_URI_PARAM + "}");
  private static final PathParamTemplate ACTION_PATH_TEMPLATE = new PathParamTemplate("/" + ACTION_DOWNLOAD_TYPE + "/{" + THREAD_ID_URI_PARAM + "}/{" + ACTION_NAME_URI_PARAM + "}/{" + CONTEXT_REF_URI_PARAM + "}/{" + FILENAME_URI_PARAM + "}");

  public DownloadServlet() {}

  public static String buildParcelDownloadURI(RequestURIBuilder pURIBuilder, String pThreadId, String pParcelId, String pFilename, DownloadMode pDownloadMode) {
    pURIBuilder.setParam(THREAD_ID_URI_PARAM, pThreadId);
    pURIBuilder.setParam(PARCEL_ID_URI_PARAM, pParcelId);
    pURIBuilder.setParam(FILENAME_URI_PARAM, pFilename);
    if(pDownloadMode != null && pDownloadMode != DEFAULT_DOWNLOAD_MODE) {
      pURIBuilder.setParam(DownloadServlet.DOWNLOAD_MODE_PARAM_NAME, pDownloadMode.getHttpParameterValue());
    }

    return pURIBuilder.buildServletURI(SERVLET_PATH, PARCEL_PATH_TEMPLATE);
  }

  public static String buildActionDownloadURI(RequestURIBuilder pURIBuilder, String pThreadId, String pActionName, String pActionContextRef, String pFilename, DownloadMode pDownloadMode) {
    pURIBuilder.setParam(THREAD_ID_URI_PARAM, pThreadId);
    pURIBuilder.setParam(ACTION_NAME_URI_PARAM, pActionName);
    pURIBuilder.setParam(CONTEXT_REF_URI_PARAM, pActionContextRef);
    pURIBuilder.setParam(FILENAME_URI_PARAM, pFilename);
    if(pDownloadMode != null && pDownloadMode != DEFAULT_DOWNLOAD_MODE) {
      pURIBuilder.setParam(DownloadServlet.DOWNLOAD_MODE_PARAM_NAME, pDownloadMode.getHttpParameterValue());
    }

    return pURIBuilder.buildServletURI(SERVLET_PATH, ACTION_PATH_TEMPLATE);
  }

  @Override
  protected String getContextUConInitialConnectionName() {
    return CONNECTION_NAME;
  }

  @Override
  public FoxSession establishFoxSession(RequestContext pRequestContext) {
    return CookieBasedFoxSession.getFoxSession(pRequestContext.getFoxRequest());
  }

  @Override
  protected String getTrackElementName(final RequestContext pRequestContext) {
    return "Download";
  }

  @Override
  public void processGet(RequestContext pRequestContext) {

    final String lDownloadType = XFUtil.pathPopHead(pRequestContext.getFoxRequest().getRequestURIStringBuilder(), true);

    //Establish download type and get params from URI
    //"action" requires an external action to be run, "parcel" is a download from an existing parcel
    Map<String, String> lURIParams;
    if (ACTION_DOWNLOAD_TYPE.equals(lDownloadType)) {
      lURIParams = ACTION_PATH_TEMPLATE.parseURI(pRequestContext.getFoxRequest().getRequestURI());
    }
    else if (PARCEL_DOWNLOAD_TYPE.equals(lDownloadType)) {
      lURIParams = PARCEL_PATH_TEMPLATE.parseURI(pRequestContext.getFoxRequest().getRequestURI());
    }
    else {
      throw new ExInternal("Unknown download type " + lDownloadType);
    }

    String lThreadId = lURIParams.get(THREAD_ID_URI_PARAM);
    ThreadLockManager<DownloadParcel> lThreadLockManager = new ThreadLockManager<>(lThreadId, CONNECTION_NAME);

    String lModeParam = pRequestContext.getFoxRequest().getParameter(DOWNLOAD_MODE_PARAM_NAME);
    DownloadMode lMode = DownloadMode.fromParameterString(lModeParam);

    Track.setProperty(TrackProperty.THREAD_ID, lThreadId);

    //Lock the thread to retrieve the download parcel, based on the download type
    DownloadParcel lDownloadParcel;
    if (ACTION_DOWNLOAD_TYPE.equals(lDownloadType)) {
      String lActionName = lURIParams.get(ACTION_NAME_URI_PARAM);
      String lContextRef = lURIParams.get(CONTEXT_REF_URI_PARAM);
      lDownloadParcel = lThreadLockManager.lockAndPerformAction(pRequestContext, getActionParcelRetriever(lActionName, lContextRef));
    }
    else if (PARCEL_DOWNLOAD_TYPE.equals(lDownloadType)) {
      String lParcelId = lURIParams.get(PARCEL_ID_URI_PARAM);
      lDownloadParcel = lThreadLockManager.lockAndPerformAction(pRequestContext, getIdParcelRetriever(lParcelId));
    }
    else {
      //This should have been hit above
      throw new ExInternal("Unknown download type " + lDownloadType);
    }

    //Stream the response
    ContextUCon lContextUCon = pRequestContext.getContextUCon();
    try {
      FoxRequest lFoxRequest = pRequestContext.getFoxRequest();
      Track.setProperty(TrackProperty.RESPONSE_TYPE, "DOWNLOAD");
      FoxResponse lResponse = streamDownloadToResponse(lFoxRequest, lContextUCon, lDownloadParcel, lMode);
      lResponse.respond(lFoxRequest);
    }
    catch (Throwable th) {
      //Construct an ExInternal so the exception will definitely be tracked even if the user gets a response
      throw new ExInternal("Failed to serve download", th);
    }
    finally {
      //Attempt to close properly
      try {
        lContextUCon.commit(CONNECTION_NAME);
      }
      catch (Throwable th) {
        //Failed to properly close - log the error (rollback should be handled in entry point)
        Track.recordSuppressedException("DownloadServletCommit", new ExInternal("Improper ContextUCon usage detected in download servlet", th));
      }
    }
  }

  private ThreadLockManager.LockedThreadRunnable<DownloadParcel> getActionParcelRetriever(final String pActionName, final String pContextRef) {
    return new ThreadLockManager.LockedThreadRunnable<DownloadParcel>() {
      @Override
      public DownloadParcel doWhenLocked(RequestContext pRequestContext, final StatefulXThread pXThread) {
        return pXThread.processAction(pRequestContext, pActionName, pContextRef, new ThreadActionResultGenerator<DownloadParcel>() {
          @Override
          public DownloadParcel establishResult(ActionRequestContext pRequestContext) {
            List<DownloadLinkXDoResult> lDownloadResults = pRequestContext.getXDoResults(DownloadLinkXDoResult.class);

            if(lDownloadResults.size() != 1) {
              throw new ExInternal("Download action link should result in a download returning exactly 1 download link, got " + lDownloadResults.size());
            }

            String lParcelId = lDownloadResults.get(0).getParcelId();
            return pXThread.getDownloadManager().getDownloadParcel(lParcelId);
          }
        });
      }
    };
  }

  private ThreadLockManager.LockedThreadRunnable<DownloadParcel> getIdParcelRetriever(final String pParcelId) {
    return new ThreadLockManager.LockedThreadRunnable<DownloadParcel>(){
      @Override
      public DownloadParcel doWhenLocked(RequestContext pRequestContext, final StatefulXThread pXThread) {

        //Ramp up the thread to force a authentication of the fox_session_id cookie
        pXThread.rampAndRun(pRequestContext, new RampedThreadRunnable() {
          @Override
          public void run(ActionRequestContext pRequestContext) {
            Track.info("DownloadAuthentication", "Ramping thread to force authentication");
          }
        }, "DownloadAuthenticate");

        //Use thread download manager to retrieve cookie
        return pXThread.getDownloadManager().getDownloadParcel(pParcelId);
      }
    };
  }

  /**
   * Streams the given DownloadParcel to a response. Any initialisation of the parcel (i.e. query execution) is performed
   * as required. This method may be invoked externally if the consumer wishes to return a download immediately rather than
   * as a separate request.
   * @param pFoxRequest Request being responded to.
   * @param pContextUCon For any required query execution.
   * @param pDownloadParcel Download to stream.
   * @param pMode Attachment disposition to set on the response.
   * @return A FoxResponse with the download streamed to it - the consumer must call {@link FoxResponse#respond}.
   */
  public static FoxResponse streamDownloadToResponse(FoxRequest pFoxRequest, ContextUCon pContextUCon, DownloadParcel pDownloadParcel, DownloadMode pMode) {
    UCon lUCon = pContextUCon.getUCon("Download " + pDownloadParcel.getParcelId());
    try {
      //Allow the download parcel to query in any metadata before setting headers
      pDownloadParcel.prepareForDownload(lUCon);

      FoxResponseByteStream lFoxResponse = createStreamResponse(pFoxRequest, pDownloadParcel, pMode);
      OutputStream lDownloadOutputStream = lFoxResponse.getHttpServletOutputStream();
      try {
        //Defer to the download parcel to stream its output
        pDownloadParcel.streamDownload(lUCon, lDownloadOutputStream);
        //Note: any errors after this point will not appear to the user as the response has been successfully sent
      }
      finally {
        pDownloadParcel.closeAfterDownload(lUCon);
      }

      return lFoxResponse;
    }
    catch (IOException e) {
       throw new ExInternal("Error streaming download parcel", e);
    }
    finally {
      pContextUCon.returnUCon(lUCon, "Download " + pDownloadParcel.getParcelId());
    }
  }

  private static FoxResponseByteStream createStreamResponse (FoxRequest pFoxRequest, DownloadParcel pDownloadParcel, DownloadMode pDownloadMode) {
    FoxResponseByteStream lFoxResponse = new FoxResponseByteStream(pDownloadParcel.getContentType(), pFoxRequest, 0);

    // Set additional response headers
//    lFoxResponse.setHttpHeader("Content-Disposition", pDownloadMode.getHttpContentDispositionHeader() + "; filename=\"" + pDownloadParcel.getFilename() + "\"");

    // TODO - This has been hardcoded due to pentest findings, could possibly be enabled later but should get download mode from download parcel, not get request
    lFoxResponse.setHttpHeader("Content-Disposition", "attachment; filename=\"" + pDownloadParcel.getFilename().replaceAll("\"", "\\\"") + "\"");

    String lContentType = pDownloadParcel.getContentType();
    if (!XFUtil.isNull(lContentType)) {
      lFoxResponse.setHttpHeader("Content-Type", lContentType);
    }

    long lContentLength = pDownloadParcel.getFileSizeBytes();
    if (lContentLength > 0) {
      lFoxResponse.setHttpHeader("Content-Length", Long.toString(lContentLength));
    }

    return lFoxResponse;
  }
}
