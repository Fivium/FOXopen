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
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackProperty;

import java.io.IOException;
import java.io.OutputStream;


public class DownloadServlet
extends EntryPointServlet {

  public DownloadServlet() {}

  private static final String SERVLET_PATH = "download";
  private static final String CONNECTION_NAME = "DOWNLOAD";
  private static final PathParamTemplate DOWNLOAD_PATH_TEMPLATE = new PathParamTemplate("/thread/{thread_id}/{parcel_id}/{filename}");

  public static String buildDownloadURI(RequestURIBuilder pURIBuilder, String pThreadId, String pParcelId, String pFilename) {
    pURIBuilder.setParam("thread_id", pThreadId);
    pURIBuilder.setParam("parcel_id", pParcelId);
    pURIBuilder.setParam("filename", pFilename);
    return pURIBuilder.buildServletURI(SERVLET_PATH, DOWNLOAD_PATH_TEMPLATE);
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

    //TODO PN/NP - ensure request is authenticated

    StringBuilder lRequestURL = pRequestContext.getFoxRequest().getRequestURIStringBuilder();
    String lFilename = XFUtil.pathPopTail(lRequestURL);
    String lParcelId = XFUtil.pathPopTail(lRequestURL);
    String lThreadId = XFUtil.pathPopTail(lRequestURL);

    DownloadParcel lDownloadParcel;
    DownloadManager lDownloadManager;
    StatefulXThread lXThread = StatefulXThread.getAndLockXThread(pRequestContext, lThreadId);
    try {
      //Ramp up the thread to force a authentication of the fox_session_id cookie
      //TODO this should be more explicit/disabled for "public" downloads
      lXThread.rampAndRun(pRequestContext, new StatefulXThread.XThreadRunnable() {
        @Override
        public void run(ActionRequestContext pRequestContext) {
          Track.info("DownloadAuthentication", "Ramping thread to force authentication");
        }
      }, "DownloadAutenticate");
      pRequestContext.getContextUCon().commit(CONNECTION_NAME);

      //Ensure any checking/reading is done while we hold a thread lock to avoid concurrency problems
      lDownloadManager = lXThread.getDownloadManager();
      lDownloadParcel = lDownloadManager.getDownloadParcel(lParcelId);
    }
    finally {
      StatefulXThread.unlockThread(pRequestContext, lXThread);
    }
    Track.setProperty(TrackProperty.THREAD_ID, lThreadId);

    String lModeParam = pRequestContext.getFoxRequest().getParameter(lDownloadManager.getDownloadModeParamName());
    DownloadMode lMode = DownloadMode.fromParameterString(lModeParam);

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
        lContextUCon.popConnection(CONNECTION_NAME);
      }
      catch (Throwable th) {
        //Failed to properly close - roll back everything and log the error
        lContextUCon.rollbackAndCloseAll(true);
        Track.recordSuppressedException("DownloadServletCommit", new ExInternal("Improper ContextUCon usage detected in download servlet", th));
      }
    }
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
