package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.entrypoint.engine.SpatialRendererWebService;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoCartographicItem;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

public class CartographicWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfoCartographicItem> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoCartographicItem> INSTANCE = new CartographicWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoCartographicItem> getInstance() {
    return INSTANCE;
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoCartographicItem pEvalNode) {

    String lSpatialRenderWebServiceURL = SpatialRendererWebService.RenderEndPoint.buildEndPointURI(pSerialisationContext.createURIBuilder(),
      pSerialisationContext.getApp().getAppMnem(),
      pEvalNode.getCanvasID(),
      pEvalNode.getCanvasWidth().replaceAll("[^0-9]", ""), // The Web Service just takes the height/width in pixels without units
      pEvalNode.getCanvasHeight().replaceAll("[^0-9]", ""));

    pSerialiser.append("<img width=\"" + pEvalNode.getCanvasWidth() + "\" height=\"" + pEvalNode.getCanvasHeight() + "\" src=\"" + lSpatialRenderWebServiceURL + "\" />");
  }
}
