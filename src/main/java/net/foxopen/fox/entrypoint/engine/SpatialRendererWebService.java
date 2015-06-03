package net.foxopen.fox.entrypoint.engine;

import net.foxopen.fox.App;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.ws.BinaryWebServiceResponse;
import net.foxopen.fox.entrypoint.ws.EndPoint;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.entrypoint.ws.WebService;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthDescriptor;
import net.foxopen.fox.entrypoint.ws.WebServiceResponse;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.spatial.SpatialEngine;
import net.foxopen.fox.thread.RequestContext;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SpatialRendererWebService
implements WebService {
  private static final String WEB_SERVICE_NAME = "spatial";
  private static final String APP_MNEM_PARAM = "app_mnem";
  private static final String CANVAS_PARAM = "canvas";
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
    return Collections.singleton(new RenderEndPoint());
  }

  public static class RenderEndPoint
  implements EndPoint {
    private static final String END_POINT_NAME = "render";
    private static final PathParamTemplate THREAD_PARAM_TEMPLATE = new PathParamTemplate("/{"+CANVAS_PARAM+"}/{"+WIDTH_PARAM+"}/{"+HEIGHT_PARAM+"}");

    @Override
    public String getName() {
      return END_POINT_NAME;
    }

    @Override
    public PathParamTemplate getPathParamTemplate() {
      return THREAD_PARAM_TEMPLATE;
    }

    @Override
    public Collection<String> getMandatoryRequestParamNames() {
      return Collections.emptySet();
    }

    @Override
    public Collection<String> getAllowedHttpMethods() {
      return Collections.singleton("GET");
    }

    public static String buildEndPointURI(RequestURIBuilder pRequestURIBuilder, String pAppMnem, String pCanvasID, String pWidth, String pHeight) {
      pRequestURIBuilder.setParam(APP_MNEM_PARAM, pAppMnem);
      pRequestURIBuilder.setParam(CANVAS_PARAM, pCanvasID);
      pRequestURIBuilder.setParam(WIDTH_PARAM, pWidth);
      pRequestURIBuilder.setParam(HEIGHT_PARAM, pHeight);
      return pRequestURIBuilder.buildWebServiceURI(EngineWebServiceCategory.CATEGORY_NAME, WEB_SERVICE_NAME, END_POINT_NAME, THREAD_PARAM_TEMPLATE);
    }

    @Override
    public WebServiceResponse respond(RequestContext pRequestContext, Map<String, String> pParamMap, String pHttpMethod, WebServiceResponse.Type pDesiredResponseType) {
      // Find query
      String lCanvasID = pParamMap.get(CANVAS_PARAM);
      int lWidth = Integer.valueOf(pParamMap.get(WIDTH_PARAM));
      int lHeight = Integer.valueOf(pParamMap.get(HEIGHT_PARAM));

      SpatialEngine lSpatialEngine = pRequestContext.getRequestApp().getSpatialEngineOrNull();
      if (lSpatialEngine == null) {
        throw new ExInternal("No spatial renderers configured for app: " + pRequestContext.getRequestApp().getAppMnem());
      }

      // Render the canvas to the output stream
      ByteArrayOutputStream lImageOutput = new ByteArrayOutputStream();
      lSpatialEngine.renderCanvasToOutputStream(pRequestContext, lImageOutput, lCanvasID, lWidth, lHeight);

      // Currently rendering to ByteArrayOutputStream first, then just writing to the response output stream here due to renderer ucon use containment
      return new BinaryWebServiceResponse("image/png", 0, lImageOutput::writeTo);
    }
  }
}
