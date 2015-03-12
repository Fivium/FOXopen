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

import net.foxopen.fox.App;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ajax.AjaxHandler;
import net.foxopen.fox.ajax.AjaxHandlerSpatial;
import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.track.Track;
import oracle.sql.CLOB;


/**
 * Interface between FOX and the database for handling spatial project/widget
 * bootstrap and rendering requests.
 */
public class SpatialEngine {
  private SpatialRenderer mSpatialRenderer;
  private String mConnectKey;
  private String mOwnerAppMnem;

  // Initialise an Ajax Handler for the spatial engine
  private final AjaxHandler mAjaxHandler;

  /**
   * Constructs a SpatialEngine for a given app.
   * @param pOwnerApp the owning App instance
   * @param pWMSUrl the url for an Oracle MapViewer instance
   * @throws ExServiceUnavailable
   */
  public SpatialEngine (App pOwnerApp, String pWMSUrl) throws ExServiceUnavailable {
    // Take care of the obvious errors
    if (XFUtil.isNull(pWMSUrl) || !pWMSUrl.matches("http[s]{0,1}://.+")) {
      throw new ExInternal("Null or invalid Oracle MapViewer URL provided to SpatialEngine constructor");
    }

    // Bootup a renderer instance
    mSpatialRenderer = new SpatialRenderer(pOwnerApp, pWMSUrl);
    mConnectKey = pOwnerApp.getConnectionPoolName();
    mOwnerAppMnem = pOwnerApp.getAppMnem();
    mAjaxHandler = new AjaxHandlerSpatial(this);
  } // SpatialEngine

  /**
   * Get the parent App mnemonic.
   * @return Mnem of owning App instance
   */
  public final String getAppMnem () {
    return mOwnerAppMnem;
  } // getApp

  /**
   * Get the Ajax handler for this SpatialEngine instance.
   * @return reference to the current AjaxHandler
   */
  public final AjaxHandler getAjaxHandler () {
    return mAjaxHandler;
  } // getAjaxHandler

  /**
   * Boots up a spatial canvas (in turn, bootstrapping a project if required).
   * @param pBootstrapDOM the data DOM fragment to pass through to the spatial db code
   * @param pCallId the current module call id
   * @param pWUAID the current user wuaid
   * @param pUCon the current application connection
   * @return canvas DOM
   */
  public DOM bootstrapSpatialCanvas (DOM pBootstrapDOM, String pCallId, String pWUAID, UCon pUCon) {
    // Bootstrap a database connection
    DOM lReturnDOM = null;
    Track.pushInfo("SpatialEngine");
    try {
      // PL/SQL API
      String lWidgetBootstrapStatement =
        "DECLARE\n" +
        "  l_dummy XMLTYPE;\n" +
        "BEGIN\n" +
        "  spatialmgr.spm.operation_start (\n" +
        "    p_calling_module => 'FOX SpatialEngine'\n" +
        "  , p_description    => 'bootstrapSpatialCanvas'\n"+
        "  , p_wua_id         => :1\n" +
        "  );\n" +
        "  l_dummy := spatialmgr.spm_fox.get_canvas(\n" +
        "    p_xml_data    => :2\n" +
        "  , p_call_id     => :3\n" +
        "  , p_wua_id      => :4\n" +
        "  );\n" +
        "  :5 := l_dummy.getClobVal();\n" +
        "  spatialmgr.spm.operation_end();\n" +
        "END;";

      // API params
      Object lParams[] = {
        pWUAID
      , pUCon.createXmlType(pBootstrapDOM)
      , pCallId
      , pWUAID
      , CLOB.class
      };

      pUCon.executeCall(
        lWidgetBootstrapStatement
      , lParams
      , new char[] { 'I','I','I','I','O' }
      );

      lReturnDOM = SQLTypeConverter.clobToDOM((CLOB)lParams[4]); //TODO PN UCON - get DOM straight from query result
    }
    catch (ExDB ex) {
      throw new ExInternal("Error Bootstrapping Spatial Canvas", ex);
    }
    finally {
      Track.pop("SpatialEngine");
    }
    return lReturnDOM;
  } // bootstrapSpatialCanvas

  /**
   * Refreshes the display of a spatial canvas.
   * @param pBootstrapDOM the data DOM fragment to pass through to the spatial db code
   * @param pUCon the connection to use
   * @return canvas DOM
   */
  public DOM refreshSpatialCanvas (DOM pBootstrapDOM, FoxRequest pFoxRequest) {
    DOM lReturnDOM = null;
    UCon lUCon = null;

    try {
      // TODO - NP - Temp made a unique connection, should use contextucon from a request context at some point
      lUCon = ConnectionAgent.getConnection(mConnectKey, "Refresh Spatial Canvas");

      // PL/SQL API
      String lWidgetBootstrapStatement =
        "DECLARE\n" +
        "  l_dummy XMLTYPE;\n" +
        "BEGIN\n" +
        "  l_dummy := spatialmgr.spm_fox.refresh_canvas(\n" +
        "    p_xml_data    => :1\n" +
        "  );\n" +
        "  :2 := l_dummy.getClobVal();\n" +
        "END;";

      DOM lSpatialOpDOM = DOM.createDocument("SPATIAL_OPERATION");
      pBootstrapDOM.copyContentsTo(lSpatialOpDOM);
      lSpatialOpDOM.addElem("TYPE", "CANVAS_REFRESH");

      // API params
      Object lParams[] = {
        lUCon.createXmlType(lSpatialOpDOM)
      , CLOB.class
      };

      lUCon.executeCall(
        lWidgetBootstrapStatement
      , lParams
      , new char[] { 'I','O' }
      );

      lReturnDOM = SQLTypeConverter.clobToDOM((CLOB)lParams[1]); //TODO PN UCON - get DOM straight from query result
      lUCon.commit();
    }
    catch (ExDB ex) {
      throw new ExInternal("Error Refreshing Spatial Canvas", ex);
    }
    catch (ExServiceUnavailable ex) {
      throw new ExInternal("Couldn't commit during SpatialEngine.refreshSpatialCanvas()");
    }
    finally {
      if (lUCon != null) {
        lUCon.closeForRecycle();
        lUCon = null;
      }
    }

    return lReturnDOM;
  } // refreshSpatialCanvas

  /**
   * Raises a spatial event.
   * @param pEventLabel the event label to raise
   * @param pCanvasDOM the canvas DOM to use to reanchor to the appropriate canvas
   * @param pUCon the connection to use
   * @return canvas DOM
   */
  public DOM performSpatialOperation (String pEventLabel, DOM pCanvasDOM, FoxRequest pFoxRequest) {
    DOM lReturnDOM = null;
    UCon lUCon = null;

    try {
      // TODO - NP - Temp made a unique connection, should use contextucon from a request context at some point
      lUCon = ConnectionAgent.getConnection(mConnectKey, "Perform Spatial Operation");

      String lSpatialOperationStatement =
        "DECLARE\n" +
        "  l_dummy XMLTYPE;\n" +
        "BEGIN\n" +
        "  l_dummy := spatialmgr.spm_fox.process_canvas_event(\n" +
        "    p_event_label => :1\n" +
        "  , p_xml_data    => :2\n" +
        "  );\n" +
        "  :3 := l_dummy.getClobVal();\n" +
        "END;";

      // API params
      Object lParams[];

      lParams = new Object[] {
        pEventLabel
      , lUCon.createXmlType(pCanvasDOM)
      , CLOB.class
      };

      lUCon.executeCall(
        lSpatialOperationStatement
      , lParams
      , new char[] { 'I','I','O' }
      );

      lReturnDOM = SQLTypeConverter.clobToDOM((CLOB)lParams[2]); //TODO PN UCON - get DOM straight from query result
      lUCon.commit();

    }
    catch (ExDB ex) {
      throw new ExInternal("Error bootstrapping spatial canvas instance", ex);
    }
    catch (ExServiceUnavailable ex) {
      throw new ExInternal("Couldn't commit during SpatialEngine.performSpatialOperation()", ex);
    }
    finally {
      if (lUCon != null) {
        lUCon.closeForRecycle();
        lUCon = null;
      }
    }
    return lReturnDOM;
  } // performSpatialOperation

  /**
   * Get a canvas rendering.
   * @param pSpatialDOM the canvas DOM
   * @return the spatial rendering result object wrapper
   */
  public SpatialRenderingResult getCanvasRendering (DOM pSpatialDOM, FoxRequest pFoxRequest) {
    UCon lSpatialCon = null;
    try {
      lSpatialCon = ConnectionAgent.getConnection(mConnectKey, "Spatial Canvas Render");
      return mSpatialRenderer.getRendering(pSpatialDOM, lSpatialCon, pFoxRequest);
    }
    catch (ExServiceUnavailable e) {
      throw e.toUnexpected();
    }
    finally {
      if (lSpatialCon != null) {
        lSpatialCon.closeForRecycle();
        lSpatialCon = null;
      }
    }
  }

  /**
   * Get a canvas rendering (for DocGen)
   * @param pXmlRequestBody XML request generated by SPM_FOX
   * @param pApp Application to generate under
   * @return a relative url of the image
   */
  public String getCanvasRendering (String pXmlRequestBody, App pApp) {
    UCon lUCon = null;
    try {
      // Bootstrap database connection
      lUCon = ConnectionAgent.getConnection(mConnectKey, "Spatial canvas render");
      return mSpatialRenderer.getRendering(pXmlRequestBody, lUCon, pApp);
    }
    catch (ExServiceUnavailable ex) {
      throw new ExInternal("Failed to establish database connection for canvas render");
    }
    finally {
      if (lUCon != null) {
        lUCon.closeForRecycle();
      }
    }
  } // getCanvasRendering

} // SpatialEngine
