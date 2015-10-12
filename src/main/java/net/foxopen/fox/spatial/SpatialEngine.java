/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.spatial;

import com.google.common.base.Joiner;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.configuration.resourcemaster.definition.AppProperty;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


/**
 * Interface between FOX and the database for handling spatial project/widget
 * bootstrap and rendering requests.
 */
public class SpatialEngine {
  private static final String GENERATE_RENDER_XML_SQL = "GenerateRenderXML.sql";
  private static final String BOOTSTRAP_SPATIAL_CANVAS_SQL = "BootstrapSpatialCanvas.sql";
  private static final String REFRESH_SPATIAL_CANVAS_SQL = "RefreshSpatialCanvas.sql";
  private static final String PERFORM_SPATIAL_OPERATION_SQL = "PerformSpatialOperation.sql";

  private final Map<String, SpatialRenderer> mRenderers = new HashMap<>(2);
  private final String mSpatialConnectionPoolName;

  public SpatialEngine(String pAppMnem, DOM  pSpatialRendererConfigList) throws ExApp {
    mSpatialConnectionPoolName = pSpatialRendererConfigList.getAttrOrNull("connection-pool-name");
    if (XFUtil.isNull(mSpatialConnectionPoolName)) {
      throw new ExApp("connection-pool-name attribute missing or not defined for " + pAppMnem + " on " + AppProperty.SPATIAL_RENDERER_LIST.getPath());
    }

    pSpatialRendererConfigList.getChildElements().forEach(this::configureRenderer);
  }

  /**
   * Configure a renderer with a DOM from the resource master
   *
   * @param pRendererConfig Configuration details in XML
   */
  private void configureRenderer(DOM pRendererConfig) {
    SpatialRenderer lSpatialRenderer;
    switch (pRendererConfig.getName().toLowerCase()) {
      case "oracle-map-viewer-renderer":
        lSpatialRenderer = new OracleMapViewerRenderer(pRendererConfig);
        break;
      case "fox-internal-spatial-renderer":
        lSpatialRenderer = new FoxInternalSpatialRenderer(pRendererConfig);
        break;
      default:
        throw new ExInternal("Unknown spatial renderer configuration type: " + pRendererConfig.getName());
    }
    mRenderers.put(lSpatialRenderer.getRenderXmlKey().toLowerCase(), lSpatialRenderer);
  }

  /**
   * Render a given spatial canvas id to an image which gets written to the given output stream
   *
   * @param pRequestContext Context to get extra information/connections from when rendering
   * @param pOutputStream Stream to write the image output to
   * @param pCallID the current module call id
   * @param pWUAID the current user wuaid
   * @param pCanvasID ID of the spatial canvas to render
   * @param pWidth Width of the image to render, in pixels
   * @param pHeight Height of the image to render, in pixels
   */
  public void renderCanvasToOutputStream(RequestContext pRequestContext, OutputStream pOutputStream, String pCallID, String pWUAID, String pCanvasID, int pWidth, int pHeight) {
    DOM lRenderXML = generateRenderXML(pRequestContext, pCallID, pWUAID, pCanvasID, pWidth, pHeight);

    SpatialRenderer lRenderer = mRenderers.get(lRenderXML.getName().toLowerCase());

    if (lRenderer == null) {
      throw new ExInternal("Render XML is not a format that any of the configured Spatial Renderers [" + Joiner.on(", ").withKeyValueSeparator(" => ").join(mRenderers) + "] know how to handle\r\n\r\n" + lRenderXML.outputDocumentToString(true));
    }

    lRenderer.processRenderXML(pRequestContext, lRenderXML, pOutputStream);
  }

  /**
   * Generate a DOM containing render rules, style information and queries that a rendering engine can use to generate
   * an image
   *
   * @param pRequestContext Context to get a connection from
   * @param pCallID the current module call id
   * @param pWUAID the current user wuaid
   * @param pCanvasID ID of the spatial canvas to render
   * @param pWidth Width of the image to render, in pixels
   * @param pHeight Height of the image to render, in pixels
   * @return DOM containing information a rendering engine can use to generate an image
   */
  private DOM generateRenderXML(RequestContext pRequestContext, String pCallID, String pWUAID, String pCanvasID, int pWidth, int pHeight) {
    UCon lUCon = pRequestContext.getContextUCon().getUCon("Rendering Canvas");
    UConStatementResult lAPIResult;
    try {
      UConBindMap lRenderBindMap = new UConBindMap()
        .defineBind(":spatial_canvas_id", pCanvasID)
        .defineBind(":call_id", pCallID)
        .defineBind(":wua_id", pWUAID)
        .defineBind(":image_width_px", pWidth)
        .defineBind(":image_height_px", pHeight)
        .defineBind(":datasource", mSpatialConnectionPoolName)
        .defineBind(":render_xml", UCon.bindOutXML());

      lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(GENERATE_RENDER_XML_SQL, getClass()), lRenderBindMap);

      return lAPIResult.getDOMFromSQLXML(":render_xml");
    }
    catch (ExDB e) {
      throw e.toUnexpected();
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Rendering Canvas");
    }
  }

  /**
   * Boots up a spatial canvas (in turn, bootstrapping a project if required)
   *
   * @param pRequestContext RequestContext to get a UCon from
   * @param pBootstrapDOM the data DOM fragment to pass through to the spatial db code
   * @param pCallID the current module call id
   * @param pWUAID the current user wuaid
   * @return canvas DOM
   */
  public DOM bootstrapSpatialCanvas(RequestContext pRequestContext, DOM pBootstrapDOM, String pCallID, String pWUAID) {
    Track.pushInfo("BootstrapSpatialCanvas");
    try {
      ContextUCon lContextUCon = pRequestContext.getContextUCon();
      UCon lUCon = lContextUCon.getUCon("Bootstrap Spatial Canvas");
      try {
        UConBindMap lBootstrapBindMap = new UConBindMap()
          .defineBind(":wua_id", pWUAID)
          .defineBind(":xml_data", pBootstrapDOM)
          .defineBind(":call_id", pCallID)
          .defineBind(":bootstrap_xml", UCon.bindOutXML());

        UConStatementResult lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(BOOTSTRAP_SPATIAL_CANVAS_SQL, getClass()), lBootstrapBindMap);
        lUCon.commit();

        return lAPIResult.getDOMFromSQLXML(":bootstrap_xml");
      }
      catch (ExDB | ExServiceUnavailable e) {
        throw new ExInternal("Error Bootstrapping Spatial Canvas", e);
      }
      finally {
        lContextUCon.returnUCon(lUCon, "Bootstrap Spatial Canvas");
      }
    }
    finally {
      Track.pop("BootstrapSpatialCanvas");
    }
  }

  /**
   * Refreshes the display of a spatial canvas
   *
   * @param pRequestContext RequestContext to get a UCon from
   * @param pBootstrapDOM the data DOM fragment to pass through to the spatial db code
   * @return canvas DOM
   */
  public DOM refreshSpatialCanvas(RequestContext pRequestContext, DOM pBootstrapDOM) {
    Track.pushInfo("RefreshSpatialCanvas");
    try {
      DOM lSpatialOpDOM = DOM.createDocument("SPATIAL_OPERATION");
      pBootstrapDOM.copyContentsTo(lSpatialOpDOM);
      lSpatialOpDOM.addElem("TYPE", "CANVAS_REFRESH");


      ContextUCon lContextUCon = pRequestContext.getContextUCon();
      UCon lUCon = lContextUCon.getUCon("Refresh Spatial Canvas");
      try {
        UConBindMap lBootstrapBindMap = new UConBindMap()
          .defineBind(":operation_xml", lSpatialOpDOM)
          .defineBind(":refresh_xml", UCon.bindOutXML());

        UConStatementResult lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(REFRESH_SPATIAL_CANVAS_SQL, getClass()), lBootstrapBindMap);
        lUCon.commit();

        return lAPIResult.getDOMFromSQLXML(":refresh_xml");
      }
      catch (ExDB | ExServiceUnavailable e) {
        throw new ExInternal("Error Refreshing Spatial Canvas", e);
      }
      finally {
        lContextUCon.returnUCon(lUCon, "Refresh Spatial Canvas");
      }
    }
    finally {
      Track.pop("RefreshSpatialCanvas");
    }
  }

  /**
   * Raises a spatial event
   *
   * @param pRequestContext RequestContext to get a UCon from
   * @param pEventLabel the event label to raise
   * @param pCanvasDOM the canvas DOM to use to re-anchor to the appropriate canvas
   * @return canvas DOM
   */
  public DOM performSpatialOperation(RequestContext pRequestContext, String pEventLabel, DOM pCanvasDOM) {
    Track.pushInfo("PerformSpatialOperation");
    try {
      ContextUCon lContextUCon = pRequestContext.getContextUCon();
      UCon lUCon = lContextUCon.getUCon("Perform Spatial Operation");
      try {
        UConBindMap lBootstrapBindMap = new UConBindMap()
          .defineBind(":event_label", pEventLabel)
          .defineBind(":xml_data", pCanvasDOM)
          .defineBind(":operation_xml", UCon.bindOutXML());

        UConStatementResult lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(PERFORM_SPATIAL_OPERATION_SQL, getClass()), lBootstrapBindMap);
        lUCon.commit();

        return lAPIResult.getDOMFromSQLXML(":operation_xml");
      }
      catch (ExDB | ExServiceUnavailable e) {
        throw new ExInternal("Error Performing Spatial Operation", e);
      }
      finally {
        lContextUCon.returnUCon(lUCon, "Perform Spatial Operation");
      }
    }
    finally {
      Track.pop("PerformSpatialOperation");
    }
  }

  public String getSpatialConnectionPoolName() {
    return mSpatialConnectionPoolName;
  }
}
