package net.foxopen.fox.entrypoint.engine;

import net.foxopen.fox.App;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.CookieBasedFoxSession;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.ws.BinaryWebServiceResponse;
import net.foxopen.fox.entrypoint.ws.EndPoint;
import net.foxopen.fox.entrypoint.ws.JSONWebServiceResponse;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.entrypoint.ws.WebService;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthDescriptor;
import net.foxopen.fox.entrypoint.ws.WebServiceResponse;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.spatial.SpatialEngine;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RampedThreadRunnable;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.ThreadLockManager;
import net.foxopen.fox.track.Track;
import org.json.simple.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Web service handling spatial render requests and posting spatial events
 */
public class SpatialWebService
implements WebService {
  private static final String WEB_SERVICE_NAME = "spatial";
  private static final String APP_MNEM_PARAM = "app_mnem";
  private static final String THREAD_ID_PARAM = "thread_id";
  private static final String CALL_ID_PARAM = "call_id";
  private static final String CANVAS_ID_PARAM = "canvas_id";
  private static final String WIDTH_PARAM = "width";
  private static final String HEIGHT_PARAM = "height";

  @Override
  public String getName() {
    return WEB_SERVICE_NAME;
  }

  @Override
  public WebServiceAuthDescriptor getAuthDescriptor() {
    return WebServiceAuthDescriptor.NO_AUTHENTICATION_REQUIRED;
  }

  @Override
  public String getRequiredConnectionPoolName(FoxRequest pFoxRequest) {
    String lAppMnem = pFoxRequest.getParameter(APP_MNEM_PARAM);
    try {
      App lApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(lAppMnem, true);
      SpatialEngine lSpatialEngine = lApp.getSpatialEngineOrNull();
      if (lSpatialEngine != null) {
        return lSpatialEngine.getSpatialConnectionPoolName();
      }
      else {
        return null;
      }
    }
    catch (ExApp | ExServiceUnavailable e) {
      return null;
    }
  }

  @Override
  public Collection<? extends EndPoint> getAllEndPoints() {
    Set<EndPoint> lEndPoints = new HashSet<>(2);
    lEndPoints.add(new RenderEndPoint());
    lEndPoints.add(new EventEndPoint());
    return Collections.unmodifiableSet(lEndPoints);
  }

  /**
   * Spatial rendering end point to request images from a spatial renderer for a given canvas ID
   */
  public static class RenderEndPoint
  implements EndPoint {
    private static final String END_POINT_NAME = "render";
    private static final PathParamTemplate RENDER_PARAM_TEMPLATE = new PathParamTemplate("/{"+THREAD_ID_PARAM+"}/{"+CALL_ID_PARAM+"}/{"+ CANVAS_ID_PARAM +"}/{"+WIDTH_PARAM+"}/{"+HEIGHT_PARAM+"}");

    @Override
    public String getName() {
      return END_POINT_NAME;
    }

    @Override
    public PathParamTemplate getPathParamTemplate() {
      return RENDER_PARAM_TEMPLATE;
    }

    @Override
    public Collection<String> getMandatoryRequestParamNames() {
      return Collections.emptySet();
    }

    @Override
    public Collection<String> getAllowedHttpMethods() {
      return Collections.singleton("GET");
    }

    /**
     * The AJAX Search EndPoint ramps up a pre-existing thread and so requires a FOX Session so this method overrides
     * the default to provide a CookieBasedFoxSession
     *
     * @param pRequestContext Current RequestContext.
     * @return A FoxSession object
     */
    @Override
    public FoxSession establishFoxSession(RequestContext pRequestContext) {
      return CookieBasedFoxSession.getOrCreateFoxSession(pRequestContext);
    }

    public static String buildEndPointURI(RequestURIBuilder pRequestURIBuilder, String pAppMnem, String pCallID, String pThreadID, String pCanvasID, String pWidth, String pHeight) {
      pRequestURIBuilder.setParam(APP_MNEM_PARAM, pAppMnem);
      pRequestURIBuilder.setParam(CALL_ID_PARAM, pCallID);
      pRequestURIBuilder.setParam(THREAD_ID_PARAM, pThreadID);
      pRequestURIBuilder.setParam(CANVAS_ID_PARAM, pCanvasID);
      pRequestURIBuilder.setParam(WIDTH_PARAM, pWidth);
      pRequestURIBuilder.setParam(HEIGHT_PARAM, pHeight);
      return pRequestURIBuilder.buildWebServiceURI(EngineWebServiceCategory.CATEGORY_NAME, WEB_SERVICE_NAME, END_POINT_NAME, RENDER_PARAM_TEMPLATE);
    }

    @Override
    public WebServiceResponse respond(RequestContext pRequestContext, Map<String, String> pParamMap, String pHttpMethod, WebServiceResponse.Type pDesiredResponseType) {
      Track.pushInfo("SpatialWebServiceRender");
      try {
        String lCallID = pParamMap.get(CALL_ID_PARAM);
        String lThreadID = pParamMap.get(THREAD_ID_PARAM);
        String lCanvasID = pParamMap.get(CANVAS_ID_PARAM);
        int lWidth = Integer.valueOf(pParamMap.get(WIDTH_PARAM));
        int lHeight = Integer.valueOf(pParamMap.get(HEIGHT_PARAM));

        // To check they have permission to render the canvas we need the WUA ID, and to get that from a web service we
        //   have to ramp up the thread
        class WuaIdGetter implements RampedThreadRunnable {
          private String mWUAID;
          @Override
          public void run(ActionRequestContext pRequestContext) throws ExUserRequest {
            mWUAID = pRequestContext.getAuthenticationContext().getAuthenticatedUser().getAccountID();
          }
        }
        WuaIdGetter lWuaIdGetter = new WuaIdGetter();
        Track.pushInfo("RampingThreadToGetWuaId");
        try {
          ThreadLockManager<FoxResponse> lThreadLockManager = new ThreadLockManager<>(lThreadID, false);
          lThreadLockManager.lockRampAndRun(pRequestContext, "SpatialRender", lWuaIdGetter);
        }
        finally {
          Track.pop("RampingThreadToGetWuaId");
        }

        SpatialEngine lSpatialEngine = pRequestContext.getRequestApp().getSpatialEngineOrNull();
        if (lSpatialEngine == null) {
          throw new ExInternal("No spatial renderers configured for app: " + pRequestContext.getRequestApp().getAppMnem());
        }

        // Render the canvas to the output stream
        Track.pushInfo("RenderingCanvasToOutputStream");
        try {
          ByteArrayOutputStream lImageOutput = new ByteArrayOutputStream();
          lSpatialEngine.renderCanvasToOutputStream(pRequestContext, lImageOutput, lCallID, lWuaIdGetter.mWUAID, lCanvasID, lWidth, lHeight);

          // Currently rendering to ByteArrayOutputStream first, then just writing to the response output stream here due to renderer ucon use containment
          return new BinaryWebServiceResponse("image/png", 0, lImageOutput::writeTo);
        }
        finally {
          Track.pop("RenderingCanvasToOutputStream");
        }
      }
      finally {
        Track.pop("SpatialWebServiceRender");
      }
    }
  }

  /**
   * Spatial event end point to take POSTed events from the client side and perform them against the database
   */
  public static class EventEndPoint
  implements EndPoint {
    private static final String END_POINT_NAME = "event";
    private static final PathParamTemplate EVENT_PARAM_TEMPLATE = new PathParamTemplate("/{"+THREAD_ID_PARAM+"}/{"+CALL_ID_PARAM+"}/{"+ CANVAS_ID_PARAM +"}");

    @Override
    public String getName() {
      return END_POINT_NAME;
    }

    @Override
    public PathParamTemplate getPathParamTemplate() {
      return EVENT_PARAM_TEMPLATE;
    }

    @Override
    public Collection<String> getMandatoryRequestParamNames() {
      return Collections.emptySet();
    }

    @Override
    public Collection<String> getAllowedHttpMethods() {
      return Collections.singleton("POST");
    }

    public static String buildEndPointURI(RequestURIBuilder pRequestURIBuilder, String pAppMnem, String pCallID, String pThreadID, String pCanvasID) {
      pRequestURIBuilder.setParam(APP_MNEM_PARAM, pAppMnem);
      pRequestURIBuilder.setParam(CALL_ID_PARAM, pCallID);
      pRequestURIBuilder.setParam(THREAD_ID_PARAM, pThreadID);
      pRequestURIBuilder.setParam(CANVAS_ID_PARAM, pCanvasID);
      return pRequestURIBuilder.buildWebServiceURI(EngineWebServiceCategory.CATEGORY_NAME, WEB_SERVICE_NAME, END_POINT_NAME, EVENT_PARAM_TEMPLATE);
    }

    @Override
    public WebServiceResponse respond(RequestContext pRequestContext, Map<String, String> pParamMap, String pHttpMethod, WebServiceResponse.Type pDesiredResponseType) {
      Track.pushInfo("SpatialWebServiceEvent");
      try {
        SpatialEngine lSpatialEngine = pRequestContext.getRequestApp().getSpatialEngineOrNull();
        if (lSpatialEngine == null) {
          throw new ExInternal("No spatial renderers configured for app: " + pRequestContext.getRequestApp().getAppMnem());
        }

        // Build up a spatial event DOM in the format the PL/SQL code expects
        DOM lCanvasDOM = DOM.createUnconnectedElement("spatial-event");
        String lEventLabel;
        switch (pParamMap.get("event")) {
          case "move":
            lEventLabel = "!CENTROID-ZOOM";
            lCanvasDOM.addElem("canvas-usage-id", pParamMap.get("canvasUseID"));
            lCanvasDOM.addElem("canvas-hash", pParamMap.get("canvasHash"));
            lCanvasDOM.addElem("zoom-direction", pParamMap.get("zoomDirection"));
            lCanvasDOM.addElem("image-width", pParamMap.get("imageWidth"));
            lCanvasDOM.addElem("image-height", pParamMap.get("imageHeight"));
            DOM lCoordSet = lCanvasDOM.addElem("coord-set");
            lCoordSet.addElem("mnem", lEventLabel);
            lCoordSet.addElem("coord-list").addElem("coord", pParamMap.get("coord"));
            break;
          default:
            throw new ExInternal("Unknown Spatial Event: " + pParamMap.get("event"));
        }

        // Perform the spatial event
        DOM lOperationResultDOM = lSpatialEngine.performSpatialOperation(pRequestContext, lEventLabel, lCanvasDOM);

        // Generate a JSON response as that's all the client side accepts
        JSONObject lJSONResponse = new JSONObject();
        lJSONResponse.put("result", "OK");
        lJSONResponse.put("changeNumber", lOperationResultDOM.get1SNoEx("/*/change-number"));
        return new JSONWebServiceResponse(lJSONResponse);
      }
      finally {
        Track.pop("SpatialWebServiceEvent");
      }
    }
  }
}
