package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.engine.SpatialWebService;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoCartographicItem;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import org.json.simple.JSONObject;

import java.util.Map;

public class CartographicWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfoCartographicItem> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoCartographicItem> INSTANCE = new CartographicWidgetBuilder();
  private static final String CARTOGRAPHIC_MUSTACHE_TEMPLATE = "html/CartographicWidget.mustache";

  public static WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoCartographicItem> getInstance() {
    return INSTANCE;
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoCartographicItem pEvalNode) {
    String lSpatialRenderWebServiceURL = SpatialWebService.RenderEndPoint.buildEndPointURI(pSerialisationContext.createURIBuilder(),
      pSerialisationContext.getApp().getAppMnem(),
      pSerialisationContext.getThreadInfoProvider().getCurrentCallId(),
      pSerialisationContext.getThreadInfoProvider().getThreadId(),
      pEvalNode.getCanvasUsageID(),
      pEvalNode.getCanvasWidth().replaceAll("[^0-9]", ""), // The Web Service just takes the height/width in pixels without units
      pEvalNode.getCanvasHeight().replaceAll("[^0-9]", ""));
    String lSpatialEventWebServiceURL = SpatialWebService.EventEndPoint.buildEndPointURI(pSerialisationContext.createURIBuilder());

    String lFieldName = pEvalNode.getExternalFieldName();

    Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);
    lTemplateVars.put("ImgSrc", lSpatialRenderWebServiceURL);
    lTemplateVars.put("Class", "map " + XFUtil.nvl(lTemplateVars.get("Class"), ""));
    lTemplateVars.put("Style", "width:" + pEvalNode.getCanvasWidth() + "; height: " + pEvalNode.getCanvasHeight() + ";" + XFUtil.nvl(lTemplateVars.get("Style"), ""));

    JSONObject lSpatialInfo = new JSONObject();
    lSpatialInfo.put("errorImageURL", pSerialisationContext.getStaticResourceURI("img/map-error.png"));
    lSpatialInfo.put("target", lFieldName);
    lSpatialInfo.put("imageURL", lSpatialRenderWebServiceURL);
    lSpatialInfo.put("eventURL", lSpatialEventWebServiceURL);
    lSpatialInfo.put("canvasUsageID", pEvalNode.getCanvasUsageID());
    lSpatialInfo.put("canvasHash", pEvalNode.getCanvasHash());
    lSpatialInfo.put("mouseZoom", pEvalNode.isMouseZoom());
    lSpatialInfo.put("zoomButtons", pEvalNode.isZoomButtons());
    lSpatialInfo.put("mousePan", pEvalNode.isMousePan());
    lSpatialInfo.put("panButtons", pEvalNode.isPanButtons());

    MustacheFragmentBuilder.applyMapToTemplate(CARTOGRAPHIC_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    pSerialisationContext.addConditionalLoadJavascript("FOXspatial.init(" + lSpatialInfo.toJSONString() + ");");
  }
}
