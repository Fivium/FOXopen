package net.foxopen.fox.spatial;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.RequestContext;

import java.io.OutputStream;

/**
 * Spatial Renderer implemenetations are created by the SpatialEngine class based on Resource Master level configuration
 * and are stored in a map against their RenderXMLKey so that they can process the Render XML for a given canvas.
 */
public interface SpatialRenderer {

  String getRenderXmlKey();

  /**
   * Render an image defined by the Render XML to the output stream
   *
   * @param pRequestContext
   * @param pRenderXML
   * @param pOutputStream
   */
  void processRenderXML(RequestContext pRequestContext, DOM pRenderXML, OutputStream pOutputStream);
}
