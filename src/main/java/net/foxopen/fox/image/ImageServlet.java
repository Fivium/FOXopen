package net.foxopen.fox.image;

import com.google.common.base.Splitter;
import net.foxopen.fox.App;
import net.foxopen.fox.ComponentImage;
import net.foxopen.fox.FoxResponseBytes;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.UnauthenticatedFoxSession;
import net.foxopen.fox.entrypoint.servlets.EntryPointServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles image processing requests, e.g. image combining, rotating, etc.
 */
public class ImageServlet
extends EntryPointServlet {

  public static final String SERVLET_PATH = "image";

  private static final String ACTION_COMBINE = "combine";
  private static final String ACTION_DISPLAY = "display";

  private static final String APP_MNEM_PARAM = "app_mnem";
  private static final String IMAGE_ID_PARAM = "image_id";
  private static final String IMAGE_WIDTH_PARAM = "width";
  private static final String IMAGE_HEIGHT_PARAM = "height";

  private static final String IMAGE_ROTATION_PARAM = "rotation";
  private static final PathParamTemplate COMBINATOR_PATH_PARAMS = new PathParamTemplate("/" + ACTION_COMBINE + "/{" + APP_MNEM_PARAM + "}/{image_list}");
  private static final PathParamTemplate DISPLAY_PATH_PARAMS = new PathParamTemplate("/" + ACTION_DISPLAY + "/{" + APP_MNEM_PARAM + "}/{" +  IMAGE_ID_PARAM +"}");

  private static final String FORWARD_SLASH_SUBSTITUTE = "$fs$";
  private static final String BACK_SLASH_SUBSTITUTE = "$bs$";
  private static final String SEMICOLON_SUBSTITUTE = "$sc$";
  /**
   * Generates a URI path suffix for a combined image URL. This should be appended to this servlet's servlet path.
   * @param pAppMnem App which the images will be read from.
   * @param pImageList Semicolon separated list of image names to combine.
   * @return Encoded URI path suffix such as "/combine/foxapp/img_base;img_layer1".
   */
  public static String generateImageCombinatorURISuffix(String pAppMnem, String pImageList) {
    Map<String, String> lParamMap = new HashMap<>(2);
    lParamMap.put(APP_MNEM_PARAM, pAppMnem);
    //We cannot have encoded slashes or semicolons as part of the URI due to Apache config; replace into placeholders
    pImageList = pImageList.replace("/", FORWARD_SLASH_SUBSTITUTE);
    pImageList = pImageList.replace("\\", BACK_SLASH_SUBSTITUTE);
    pImageList = pImageList.replace(";", SEMICOLON_SUBSTITUTE);

    //Trim out spaces as these aren't handled properly when part of the URI (i.e. "+" is not interpreted as a space)
    pImageList = pImageList.replace(" " , "");

    lParamMap.put("image_list", pImageList);
    return COMBINATOR_PATH_PARAMS.generateURIFromParamMap(lParamMap);
  }

  public static String generateImageDisplayURI(RequestURIBuilder pURIBuilder, String pImageId, Integer pWidth, Integer pHeight, int pRotation) {

    //App mnem doesn't matter at the moment
    pURIBuilder.setParam(APP_MNEM_PARAM, FoxGlobals.getInstance().getFoxEnvironment().getDefaultAppMnem());
    pURIBuilder.setParam(IMAGE_ID_PARAM, pImageId);

    pURIBuilder.setParam(IMAGE_ROTATION_PARAM, Integer.toString(pRotation));

    if(pWidth != null) {
      pURIBuilder.setParam(IMAGE_WIDTH_PARAM, pWidth.toString());
    }

    if(pHeight != null) {
      pURIBuilder.setParam(IMAGE_HEIGHT_PARAM, pHeight.toString());
    }

    return pURIBuilder.buildServletURI(SERVLET_PATH, DISPLAY_PATH_PARAMS);
  }

  @Override
  public FoxSession establishFoxSession(RequestContext pRequestContext) {
    //We don't need any auth for image access
    return UnauthenticatedFoxSession.create();
  }

  @Override
  protected String getContextUConInitialConnectionName() {
    return "IMAGE";
  }

  @Override
  protected String getTrackElementName(RequestContext pRequestContext) {
    return "ImageProcessor";
  }

  /**
   * Takes the action and app mnem from the request param path and returns it in a data object for readability.
   * @param pRequest
   * @return
   */
  private ImageRequestParams getRequestParams(HttpServletRequest pRequest) {
    StringBuilder lRequestPath =  new StringBuilder(pRequest.getPathInfo());
    //Pop the "action" off the URI
    String lAction = XFUtil.pathPopHead(lRequestPath, true);
    String lAppMnem = XFUtil.pathPopHead(lRequestPath, true);

    return new ImageRequestParams(lAction, lAppMnem);
  }

  @Override
  protected String establishAppMnem(HttpServletRequest pRequest) {
    //Use the app mnem from the request URI
    return getRequestParams(pRequest).mAppMnem;
  }

  @Override
  public void processGet(RequestContext pRequestContext) {

    String lAction = getRequestParams( pRequestContext.getFoxRequest().getHttpRequest()).mAction;

    if(ACTION_COMBINE.equals(lAction)) {
      Track.pushInfo("CombineImage");
      try {
        processCombinatorRequest(pRequestContext);
      }
      finally {
        Track.pop("CombineImage");
      }
    }
    else if(ACTION_DISPLAY.equals(lAction)) {
      Track.pushInfo("DisplayImage");
      try {
        processDisplayRequest(pRequestContext);
      }
      finally {
        Track.pop("DisplayImage");
      }
    }
    else {
      throw new ExInternal("Unknown image request action " + lAction);
    }
  }

  /**
   * Resovles parameters into images, invokes the combinator code and responds with a binary result.
   * @param pRequestContext
   */
  private void processCombinatorRequest(RequestContext pRequestContext) {
    Map<String, String> lURIParams = COMBINATOR_PATH_PARAMS.parseURI(pRequestContext.getFoxRequest().getRequestURI());
    String lAppMnem = lURIParams.get(APP_MNEM_PARAM);
    String lImageList = lURIParams.get("image_list");

    //Resolve an app from the URI
    App lApp;
    try {
      lApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(lAppMnem);
    }
    catch (ExServiceUnavailable | ExApp e) {
      throw e.toUnexpected();
    }

    //Replace escaped characters back in (these characters aren't allowed in URIs by apache)
    lImageList = lImageList.replace(FORWARD_SLASH_SUBSTITUTE, "/");
    lImageList = lImageList.replace(BACK_SLASH_SUBSTITUTE, "\\");
    lImageList = lImageList.replace(SEMICOLON_SUBSTITUTE, ";");

    //Resolve image names to components (split and trim image names based on semicolon)
    List<ComponentImage> lComponentImages = new ArrayList<>();
    for(String lImageName : Splitter.on(";").trimResults().split(lImageList)) {
      lComponentImages.add(lApp.getImage(lImageName));
    }

    //Combine images
    ImageCombinator lImageCombinator = ImageCombinator.createAndCombine(lComponentImages);

    //Respond immediately - currently we are not caching anything in the engine - this should be done externally
    FoxResponseBytes lImageResponse = new FoxResponseBytes(lImageCombinator.getContentType(), lImageCombinator.getOutputByteArray(), ComponentManager.getComponentBrowserCacheMS());
    lImageResponse.respond(pRequestContext.getFoxRequest());
  }

  /**
   * Locates an image from the FOX4 processed images table with the given rotation and dimensions, and streams the image
   * as the response.
   * @param pRequestContext
   */
  private void processDisplayRequest(RequestContext pRequestContext) {

    Map<String, String> lURIParams = DISPLAY_PATH_PARAMS.parseURI(pRequestContext.getFoxRequest().getRequestURI());
    String lImageId = lURIParams.get(IMAGE_ID_PARAM);

    Integer lImageWidth = null;
    Integer lImageHeight = null;
    try {
      String lWidthParam = pRequestContext.getFoxRequest().getParameter(IMAGE_WIDTH_PARAM);
      if(!XFUtil.isNull(lWidthParam)) {
        lImageWidth = Integer.parseInt(lWidthParam);
      }

      String lHeightParam = pRequestContext.getFoxRequest().getParameter(IMAGE_HEIGHT_PARAM);
      if(!XFUtil.isNull(lHeightParam)) {
        lImageHeight = Integer.parseInt(lHeightParam);
      }
    }
    catch (NumberFormatException e) {
      throw new ExInternal("Invalid image dimension specified", e);
    }

    ImageLocator lImageLocator = new ImageLocator(lImageId, lImageWidth, lImageHeight, 0);
    lImageLocator.locateAndRespond(pRequestContext);
  }

  /** Holds action and app mnem params from URI path */
  private static class ImageRequestParams {
    final String mAction;
    final String mAppMnem;

    ImageRequestParams(String pAction, String pAppMnem) {
      mAction = pAction;
      mAppMnem = pAppMnem;
    }
  }
}
