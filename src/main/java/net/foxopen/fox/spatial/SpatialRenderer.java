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
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.*;
import net.foxopen.fox.image.ImageUtils;
import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.renderer.*;
import oracle.spatial.geometry.JGeometry;
import oracle.sql.CLOB;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;


/**
 * Renders a map using Oracle MapViewer and returns a wrapped response.
 */
public class SpatialRenderer {

  private final String mRequestURI;
  private final String mOwnerAppMnem;
  private final String mConnectKey;

  public SpatialRenderer(App pApp, String pMapServerRequestURI) {
    mRequestURI = pMapServerRequestURI;
    mOwnerAppMnem = pApp.getAppMnem();
    mConnectKey = pApp.getConnectionPoolName();
  }

  //Get rendering for ajax request
  public SpatialRenderingResult getRendering (DOM pRenderRequest, UCon pUCon, FoxRequest pFoxRequest) {

    String lXmlRequestBody;
    String lCanvasId;
    String lWidth;
    String lHeight;
    String lStatement =
      "DECLARE\n" +
      "  l_render_xml XMLTYPE;\n" +
      "BEGIN\n" +
      "  l_render_xml := spatialmgr.spm_fox.generate_render_xml(\n" +
      "    p_sc_id      => :1\n" +
      "  , p_width_px   => :2\n" +
      "  , p_height_px  => :3\n" +
      "  , p_datasource => :4\n" +
      "  );\n" +
      "  :5 := l_render_xml.getClobVal();\n" +
      "END;";

    try {
      // Get canvas id
      lCanvasId = pRenderRequest.get1S("/*/canvas-id");
      lWidth = pRenderRequest.get1S("/*/image-width");
      lHeight = pRenderRequest.get1S("/*/image-height");

      // API params
      Object lParams[] = {
        lCanvasId
      , lWidth
      , lHeight
      , mConnectKey
      , CLOB.class
      };

      pUCon.executeCall(
        lStatement
      , lParams
      , new char[] { 'I','I','I','I','O' }
      );

      lXmlRequestBody =  SQLTypeConverter.clobToString((CLOB) lParams[4]); //TODO PN UCON read string directly from result
    }
    catch (ExDB ex) {
      throw new ExInternal("Query failed in Spatial Renderer", ex);
    }
    catch (ExTooMany ex) {
      throw new ExInternal("Cardinality error resulting from spatial rendering query", ex);
    }
    catch (ExTooFew ex) {
      throw new ExInternal("Cardinality error resulting from spatial rendering query", ex);
    }

    try {
      byte[] lImageBytes = null;

      lImageBytes = getImage(lXmlRequestBody, pUCon);

      //Fail on null returned
      if (lImageBytes == null) {
        throw new ExInternal("Failed to get image bytes");
      }

      // TODO: We should ideally determine this from the request XML before we
      // send off the request, and branch based on the expected response type.
      String lContentType = "image/png";

      App lApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(mOwnerAppMnem);

      // Store in database
      // TODO - PN -  Pete was thinking about temp resources 15/10/14...
      String lResName = "";
//      String lResName = lApp.createTemporaryResource(
//        "spatial-" + XFUtil.unique() // unique name
//        , "0"          // pExpires
//        , "0"          // pClientCache
//        , lImageBytes  // pByteArray
//        , lContentType // pContentType
//        , null         // pOptionalUCon
//        );

      return new SpatialRenderingResult(
        "URI GENERATION TODO"// XFUtil.resolveLink(XFUtil.getFoxServletMnem(pFoxRequest, lApp), lResName)
      , Integer.valueOf(lWidth).intValue()
      , Integer.valueOf(lHeight).intValue()
      );
    }
    catch (ExServiceUnavailable e) {
      throw new ExInternal("Failed getting app ", e);
    }
    catch (ExApp e) {
      throw new ExInternal("Failed getting app ", e);
    }
  } // getRendering

  //Get rendering for DocGen, passing in render XML
  public String getRendering (String pXmlRequestBody, UCon pUCon, App pApp) {
    byte[] lImageBytes = getImage(pXmlRequestBody, pUCon);

    //Fail on null returned
    if (lImageBytes == null) {
      throw new ExInternal("Failed to get image bytes");
    }

    // TODO: We should ideally determine this from the request XML before we
    // send off the request, and branch based on the expected response type.
    String lContentType = "image/png";

    try {
      App lApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(mOwnerAppMnem);

      // Store in database
      // TODO - PN -  Pete was thinking about temp resources 15/10/14...
//      return lApp.createTemporaryResource(
//        "spatial-" + XFUtil.unique() // unique name
//        , "0"          // pExpires
//        , "0"          // pClientCache
//        , lImageBytes  // pByteArray
//        , lContentType // pContentType
//        , null         // pOptionalUCon
//        );
      return null;
    }
    catch (ExServiceUnavailable e) {
      throw new ExInternal("Failed getting app ", e);
    }
    catch (ExApp e) {
      throw new ExInternal("Failed getting app ", e);
    }
  } // getRendering

  private byte[] getImage (String pXmlRequestBody, UCon pUCon) {
    byte[] lImageBytes = null;

    try {
      if (pXmlRequestBody.startsWith("<map_request")) {
        /** Get image from remote mapviewer server */
        FoxLogger.getLogger().info("Map Viewer rendering started");
        long timing = System.currentTimeMillis();
        // Add in standard form post key/value pair emulation
        pXmlRequestBody = "xml_request=" + URLEncoder.encode(pXmlRequestBody);

        // Set up the POST request
        URL lURL = new URL(mRequestURI);
        URLConnection lURLConnection = lURL.openConnection();

        // Append headers
        lURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        lURLConnection.setRequestProperty("Content-Length", String.valueOf(pXmlRequestBody.length()));
        lURLConnection.setDoOutput(true);

        // Write out
        OutputStreamWriter lOutputStreamWriter = new OutputStreamWriter(lURLConnection.getOutputStream());
        lOutputStreamWriter.write(pXmlRequestBody);
        lOutputStreamWriter.flush();

        // Get the response (convert to byte array)
        // TODO: Streaming would be better here, but the images shouldn't be very large
        lImageBytes = XFUtil.toByteArray(lURLConnection.getInputStream(), 2048, -1);

        // Clean-up
        lOutputStreamWriter.close();

        FoxLogger.getLogger().info("Map Viewer rendering took {}ms", (System.currentTimeMillis() - timing));
      }
      else if (pXmlRequestBody.startsWith("<INTERNAL_MAP_REQUEST>")) {
        /** Get image from internal renderer */
        FoxLogger.getLogger().info("Internal spatial rendering started");
        long timing = System.currentTimeMillis();
        DOM lRequestDOM = DOM.createDocumentFromXMLString(pXmlRequestBody);
        int lHeight, lWidth, lDPI;
        lHeight = Integer.parseInt(lRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/HEIGHT"));
        lWidth = Integer.parseInt(lRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/WIDTH"));
        lDPI = Integer.parseInt(XFUtil.nvl(lRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/DPI"), "72"));

        synchronized(SpatialRenderer.class){
           lImageBytes = internalRender(lRequestDOM, pUCon, new RendererPNG(lWidth, lHeight, lDPI, Color.decode(lRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/BGCOLOR"))));
        }

        FoxLogger.getLogger().info("Internal spatial rendering took {}ms", (System.currentTimeMillis() - timing));
      }
      else {
        throw new ExInternal("What kind of render request is this?!\r\n\r\n" + pXmlRequestBody);
      }
    }
    catch (MalformedURLException ex) {
      throw new ExInternal("Bad Request URL", ex);
    }
    catch (IOException ex) {
      throw new ExInternal("Failed to read image from Oracle MapViewer installation", ex);
    }

    return lImageBytes;
  } // getImage



  /*
   * INTERNAL RENDER SCRIPT BELOW, TODO NICK: SPLIT OUT ONE DAY, maybe?
   */



  public static byte[] internalRender(DOM pRequestDOM, UCon pUCon, Renderer pRenderer){
    int lHeight, lWidth, lDPI;
    lHeight = Integer.parseInt(pRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/HEIGHT"));
    lWidth = Integer.parseInt(pRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/WIDTH"));
    lDPI = Integer.parseInt(XFUtil.nvl(pRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/DPI"), "72"));

    Renderer lRenderer = pRenderer;

    //Transform for pixel output
    double lCentreX = Double.parseDouble(pRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/CENTRE/X"));
    double lCentreY = Double.parseDouble(pRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/CENTRE/Y"));
    double lScaledWidth = Double.parseDouble(pRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/SCALED_WIDTH"));
    double lScaledHeight = Double.parseDouble(pRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/SCALED_HEIGHT"));
    double lScaleFactorX = (lWidth/lScaledWidth);
    double lScaleFactorY = (lHeight/lScaledHeight);
    AffineTransform lOrdinateTransform = new AffineTransform();

    lOrdinateTransform.scale(1, -1); // Flip the Y about X=0
    lOrdinateTransform.translate(0, -lHeight); // Move back onto the visible area

    lOrdinateTransform.translate(((lWidth/2)-lCentreX), (((lHeight/2))-lCentreY)); // Initial centering
    lOrdinateTransform.scale(lScaleFactorX, lScaleFactorY); // Scale
    lOrdinateTransform.translate(-(lCentreX*(lScaleFactorX-1))/lScaleFactorX, -(lCentreY*(lScaleFactorY-1))/lScaleFactorY); // Second pass centering


    FoxLogger.getLogger().info("Internal rendering: Adding renderable objects to renderer");
    long timing = System.currentTimeMillis();

    DOMList lRenderNodes = pRequestDOM.getUL("/INTERNAL_MAP_REQUEST/RENDER_LIST/RENDER");
    for (int lNodeID = 0; lNodeID < lRenderNodes.getLength(); ++lNodeID) {
      DOM lRenderNode = lRenderNodes.item(lNodeID);

      //TODO PN - query required reimplementing
      //Run query to get spatial data
//      UCon.ResultIterator lLocalDataIterator;
      List lDataList;
//      try {
        //lLocalDataIterator = null;//pUCon.executeIterator(lRenderNode.get1SNoEx("DATA_QUERY"), null, true);
        lDataList = null;//lLocalDataIterator.selectAllRows(true);
//       }
//      catch (ExDB e) {
//        e.printStackTrace();
//        throw e.toUnexpected("Error running select query");
//      }

      //Attempt to find column positions
      Object[] lHeaders = (Object[])lDataList.get(0);
      int lIDCol = -1, lParentIDCol = -1, lGeometryCol = -1, lAnnotationCol = -1, lParentGeometryCol = -1;
      for (int column = 0; column < lHeaders.length; column++) {
        String lHeading = (String)lHeaders[column];
        if ("ID".equals(lHeading)) {
          lIDCol = column;
        }
        else if ("PARENT_SID_ID".equals(lHeading)) {
          lParentIDCol = column;
        }
        else if ("GEO_DATA".equals(lHeading)) {
          lGeometryCol = column;
        }
        else if ("ANNO_TEXT".equals(lHeading)) {
          lAnnotationCol = column;
        }
        else if ("PARENT_GEO".equals(lHeading)) {
          lParentGeometryCol = column;
        }
      }

      //Loop over the result set (minus the first item which is headers)
      for (int lGeometryID = 1; lGeometryID < lDataList.size(); ++ lGeometryID) {
        //Grab data from the row
        Object[] lRow = (Object[])lDataList.get(lGeometryID);
        int lGeoID = ((Double)lRow[lIDCol]).intValue();
        int lParentGeoID = ((Double)lRow[lParentIDCol]).intValue();
        JGeometry lGeometry = (JGeometry)lRow[lGeometryCol];

        //Parse geometry
        if (lGeometry.getType() == JGeometry.GTYPE_POINT) {
          drawSpatialMarker(lRenderer, lRenderNode, lGeoID, lParentGeoID, lGeometry, lOrdinateTransform, lRow, lAnnotationCol, lParentGeometryCol, lScaleFactorX, lScaleFactorY);
        }
        else if (lGeometry.getType() == JGeometry.GTYPE_MULTIPOINT) {
          drawSpatialNodes(lRenderer, lRenderNode, lGeoID, lParentGeoID, lGeometry, lOrdinateTransform);
        }
        else if (lGeometry.getType() == JGeometry.GTYPE_CURVE) {
          drawSpatialLine(lRenderer, lRenderNode, lGeoID, lParentGeoID, lGeometry, lOrdinateTransform, lRow, lAnnotationCol);
        }
        else if (lGeometry.getType() == JGeometry.GTYPE_POLYGON) {
          drawSpatialArea(lRenderer, lRenderNode, lGeoID, lParentGeoID, lGeometry, lOrdinateTransform, lRow, lAnnotationCol);
        }
      }
    }

    FoxLogger.getLogger().info("Internal rendering: Took {}ms to add renderable objects to renderer", (System.currentTimeMillis() - timing));
    timing = System.currentTimeMillis();

    //Return byte array
    try {
      byte[] lRendering = lRenderer.generate();
      FoxLogger.getLogger().info("Internal rendering: Took {}ms to render with {}", (System.currentTimeMillis() - timing), lRenderer.getClass().getName().substring(lRenderer.getClass().getName().lastIndexOf('.') + 1));
      return lRendering;
    }
    catch (IOException e) {
      return null;
    }
  } // internalRender

  /**
   * Transform a point from spatial ordinates to pixel ordinates
   *
   * @param pPoint The point to transform
   * @param pTransformation An affine transform to apply
   * @return Transformed point
   */
  private static Point2D transformPoint (Point2D pPoint, AffineTransform pTransformation) {
    Point2D lPoint = new Point2D.Double();
    pTransformation.transform(pPoint, lPoint);

    return lPoint;
  } // transformPoint

   /**
   * Draw an area from a spatial geometry
   *
   * @param pRenderer Renderer to add object too
   * @param pRenderNode Render node from generated XML with style information
   * @param pGeoID SID.id of the current object
   * @param pParentGeoID parent id of the current object
   * @param pGeometry Java instance of an Oracle spatial object
   * @param pTransformation Transformation for spatial -> pixel conversion
   * @param pRow Row data to get possible annotation from
   * @param pAnnotationCol Index of annotation column in pRow
   */
  private static void drawSpatialArea (Renderer pRenderer, DOM pRenderNode, int pGeoID, int pParentGeoID, JGeometry pGeometry, AffineTransform pTransformation, Object[] pRow, int pAnnotationCol) {
    String lSelectedSet = pRenderNode.get1SNoEx("SELECTED_SET");

    Area lPolygon = new Area();
    int[] lInfoArray = pGeometry.getElemInfo();
    Object[] lOrdinateGroups = pGeometry.getOrdinatesOfElements();
    for (int lElementID = 0; lElementID < lOrdinateGroups.length; ++ lElementID) {
      int lCoords = (((double[])lOrdinateGroups[lElementID]).length/2);
      int[] x = new int[lCoords];
      int[] y = new int[lCoords];
      int lCoordID = 0;
      for (int lOrdinateID = 0; lOrdinateID < ((double[])lOrdinateGroups[lElementID]).length; lOrdinateID = lOrdinateID + 2) {
        Point2D lP = new Point2D.Double();
        lP = transformPoint(new Point2D.Double(((double[])lOrdinateGroups[lElementID])[lOrdinateID], ((double[])lOrdinateGroups[lElementID])[lOrdinateID + 1]), pTransformation);
        x[lCoordID] = new Double(lP.getX()).intValue();
        y[lCoordID] = new Double(lP.getY()).intValue();
        lCoordID++;
      }

      Shape lShape = new Polygon(x, y, lCoords);
      Area lArea = new Area(lShape);
      //Find out polygon type (1003 = area, 2003 = cutout)
      int lPolygonType = lInfoArray[((lElementID*3)+1)];
      if (lPolygonType == 1003) {
        lPolygon.add(lArea);
      }
      else if (lPolygonType == 2003) {
        lPolygon.subtract(lArea);
      }
    }

    pRenderer.addObject(lSelectedSet + Integer.toString(pParentGeoID), new RenderableObject(lSelectedSet + Integer.toString(pGeoID), lPolygon, pRenderNode));
  } // drawSpatialRender


  /**
   * Draw a line from a spatial geometry
   *
   * @param pRenderer Renderer to add object too
   * @param pRenderNode Render node from generated XML with style information
   * @param pGeoID SID.id of the current object
   * @param pParentGeoID parent id of the current object
   * @param pGeometry Java instance of an Oracle spatial object
   * @param pTransformation Transformation for spatial -> pixel conversion
   * @param pRow Row data to get possible annotation from
   * @param pAnnotationCol Index of annotation column in pRow
   */
  private static void drawSpatialLine (Renderer pRenderer, DOM pRenderNode, int pGeoID, int pParentGeoID, JGeometry pGeometry, AffineTransform pTransformation, Object[] pRow, int pAnnotationCol) {
    String lSelectedSet = pRenderNode.get1SNoEx("SELECTED_SET");

    Object[] lOrdinateGroups = pGeometry.getOrdinatesOfElements();
    for (int lElementID = 0; lElementID < lOrdinateGroups.length; ++ lElementID) {
      int lCoords = (((double[])lOrdinateGroups[lElementID]).length/2);
      int[] x = new int[lCoords];
      int[] y = new int[lCoords];
      int lCoordID = 0;
      for (int lOrdinateID = 0; lOrdinateID < ((double[])lOrdinateGroups[lElementID]).length; lOrdinateID = lOrdinateID + 2) {
        Point2D lP = new Point2D.Double();
        lP = transformPoint(new Point2D.Double(((double[])lOrdinateGroups[lElementID])[lOrdinateID], ((double[])lOrdinateGroups[lElementID])[lOrdinateID + 1]), pTransformation);
        x[lCoordID] = new Double(lP.getX()).intValue();
        y[lCoordID] = new Double(lP.getY()).intValue();

        lCoordID++;
      }

      PolyLine lNewLine;
      if (pAnnotationCol >= 0 && pRow[pAnnotationCol] instanceof String) {
        lNewLine = new PolyLine(x, y, (String)pRow[pAnnotationCol]);
      }
      else {
        lNewLine = new PolyLine(x, y);
      }
      pRenderer.addObject(lSelectedSet + Integer.toString(pParentGeoID), new RenderableObject(lSelectedSet + Integer.toString(pGeoID), lNewLine, pRenderNode));
    }
  } // drawSpatialLine

  /**
   * Draw nodes from spatial geometry
   *
   * @param pRenderer Renderer to add object too
   * @param pRenderNode Render node from generated XML with style information
   * @param pGeoID SID.id of the current object
   * @param pParentGeoID parent id of the current object
   * @param pGeometry Java instance of an Oracle spatial object
   * @param pTransformation Transformation for spatial -> pixel conversion
   */
  private static void drawSpatialNodes (Renderer pRenderer, DOM pRenderNode, int pGeoID, int pParentGeoID, JGeometry pGeometry, AffineTransform pTransformation) {
    String lSelectedSet = pRenderNode.get1SNoEx("SELECTED_SET");

    Object[] lOrdinateGroups = pGeometry.getOrdinatesOfElements();
    for (int lElementID = 0; lElementID < lOrdinateGroups.length; ++ lElementID) {
      for (int lOrdinateID = 0; lOrdinateID < ((double[])lOrdinateGroups[lElementID]).length; lOrdinateID = lOrdinateID + 2) {
        Point2D lP = new Point2D.Double();
        lP = transformPoint(new Point2D.Double(((double[])lOrdinateGroups[lElementID])[lOrdinateID], ((double[])lOrdinateGroups[lElementID])[lOrdinateID + 1]), pTransformation);

        pRenderer.addObject(lSelectedSet + Integer.toString(pParentGeoID), new RenderableObject(lSelectedSet + Integer.toString(pGeoID), lP, pRenderNode));
      }
    }
  } // drawSpatialNodes

  /**
   * Draw a marker from a spatial geometry
   *
   * @param pRenderer Renderer to add object too
   * @param pRenderNode Render node from generated XML with style information
   * @param pGeoID SID.id of the current object
   * @param pParentGeoID parent id of the current object
   * @param pGeometry Java instance of an Oracle spatial object
   * @param pTransformation Transformation for spatial -> pixel conversion
   * @param pRow Row data to get possible annotation from
   * @param pAnnotationCol Index of annotation column in pRow
   * @param pParentGeometryCol Index of the parent geometry column in pRow
   * @param pScaleFactorX Scaling factor for keeping text the right size while zooming
   * @param pScaleFactorY Scaling factor for keeping text the right size while zooming
   */
  private static void drawSpatialMarker (Renderer pRenderer, DOM pRenderNode, int pGeoID, int pParentGeoID, JGeometry pGeometry, AffineTransform pTransformation, Object[] pRow, int pAnnotationCol, int pParentGeometryCol, double pScaleFactorX, double pScaleFactorY) {
    // Convert spatial point to pixel point
    Point2D lPoint = transformPoint(pGeometry.getJavaPoint(), pTransformation);

    Graphics2D lG2D = pRenderer.getGraphics2D();

    String lSelectedSet = pRenderNode.get1SNoEx("SELECTED_SET");

    if (pAnnotationCol >= 0 && pRow[pAnnotationCol] instanceof String) {
      //Set string
      String lAnnotation = (String)pRow[pAnnotationCol];

      //Set up font
      int lFontSize = 10;
      if (!"".equals(pRenderNode.get1SNoEx("TEXT/SIZE")) && pRenderNode.get1EOrNull("TEXT/SIZE") != null && "REAL".equals(pRenderNode.get1EOrNull("TEXT/SIZE").getAttrOrNull("unit").toUpperCase())) {
        lFontSize = (int)(Double.parseDouble(pRenderNode.get1SNoEx("TEXT/SIZE"))*pScaleFactorX);
      }
      else if (!"".equals(pRenderNode.get1SNoEx("TEXT/SIZE"))) {
        lFontSize = Integer.parseInt(pRenderNode.get1SNoEx("TEXT/SIZE"));
      }

      int lFontStyle = Font.PLAIN;
      if ("bold".equals(pRenderNode.get1EOrNull("TEXT/FONT").getAttrOrNull("style"))) {
        lFontStyle = Font.BOLD;
      }

      String lJustification = pRenderNode.get1SNoEx("TEXT/JUSTIFICATION").toUpperCase();

      //Default wrapping
      int lWrappingWidth = 9999;
      int lWrappingHeight = 9999;

      //Get parent geometry
      JGeometry lParentBoundsGeom = (JGeometry)pRow[pParentGeometryCol];
      double[] lPoints = lParentBoundsGeom.getOrdinatesArray();
      int lParentWidth = (int)Math.abs(lPoints[0] - lPoints[2]);
      int lParentHeight = (int)Math.abs(lPoints[1] - lPoints[3]);
      //Figure out width
      if (!"".equals(pRenderNode.get1SNoEx("TEXT/BOUNDS/WIDTH")) && pRenderNode.get1EOrNull("TEXT/BOUNDS") != null && "real".equals(pRenderNode.get1EOrNull("TEXT/BOUNDS").getAttrOrNull("unit"))) {
        if ("AUTO".equals(pRenderNode.get1SNoEx("TEXT/BOUNDS/WIDTH").toUpperCase())) {
          lWrappingWidth = (int)(lParentWidth*pScaleFactorX);
        }
        else if ("HALF-AUTO".equals(pRenderNode.get1SNoEx("TEXT/BOUNDS/WIDTH").toUpperCase())) {
          lWrappingWidth = (int)((lParentWidth/2)*pScaleFactorX);
        }
        else {
          lWrappingWidth = (int)(Integer.parseInt(pRenderNode.get1SNoEx("TEXT/BOUNDS/WIDTH"))*pScaleFactorX);
        }
      }
      else if (!"".equals(pRenderNode.get1SNoEx("TEXT/BOUNDS/WIDTH"))) {
        lWrappingWidth = Integer.parseInt(pRenderNode.get1SNoEx("TEXT/BOUNDS/WIDTH"));
      }
      //Figure out height
      if (!"".equals(pRenderNode.get1SNoEx("TEXT/BOUNDS/HEIGHT")) && pRenderNode.get1EOrNull("TEXT/BOUNDS") != null && "real".equals(pRenderNode.get1EOrNull("TEXT/BOUNDS").getAttrOrNull("unit"))) {
        if ("AUTO".equals(pRenderNode.get1SNoEx("TEXT/BOUNDS/HEIGHT").toUpperCase())) {
          lWrappingHeight = (int)(lParentHeight*pScaleFactorY);
        }
        else if ("HALF-AUTO".equals(pRenderNode.get1SNoEx("TEXT/BOUNDS/HEIGHT").toUpperCase())) {
          lWrappingHeight = (int)((lParentHeight/2)*pScaleFactorY);
        }
        else {
          lWrappingHeight = (int)(Integer.parseInt(pRenderNode.get1SNoEx("TEXT/BOUNDS/HEIGHT"))*pScaleFactorY);
        }
      }
      else if (!"".equals(pRenderNode.get1SNoEx("TEXT/BOUNDS/HEIGHT"))) {
        lWrappingHeight = Integer.parseInt(pRenderNode.get1SNoEx("TEXT/BOUNDS/HEIGHT"));
      }

      //Account for buffers
      if (!"".equals(pRenderNode.get1SNoEx("TEXT/BOUNDS/WIDTH_BUFFER"))) {
        lWrappingWidth -= Integer.parseInt(pRenderNode.get1SNoEx("TEXT/BOUNDS/WIDTH_BUFFER"));
      }
      if (!"".equals(pRenderNode.get1SNoEx("TEXT/BOUNDS/HEIGHT_BUFFER"))) {
        lWrappingHeight -= Integer.parseInt(pRenderNode.get1SNoEx("TEXT/BOUNDS/HEIGHT_BUFFER"));
      }

      //Get bounds based on current values
      lG2D.setFont(new Font(pRenderNode.get1SNoEx("TEXT/FONT"), lFontStyle, lFontSize));

      // Fix for PowerPoint generation:
      // Cut down wrapping width a bit to help when PowerPoint attempts to render the image
      if (pRenderer instanceof RendererEMF) {
        lWrappingWidth *= 0.95;
      }

      Rectangle lBounds = ImageUtils.drawString(lAnnotation, lG2D, 0, 0, lWrappingWidth, lWrappingHeight, true, true, lJustification, false);

      //If the bounds are too tall and the font is shrinkable, shrink it
      if (lBounds.getHeight() > lWrappingHeight && pRenderNode.get1EOrNull("TEXT/SIZE") != null && "AUTO".equals(pRenderNode.get1EOrNull("TEXT/SIZE").getAttrOrNull("shrink").toUpperCase())) {
        int lIterations = 0;
        while (lBounds.getHeight() > lWrappingHeight && lIterations < 512 && lFontSize > 2) {
          lG2D.setFont(new Font(pRenderNode.get1SNoEx("TEXT/FONT"), lFontStyle, --lFontSize));
          lBounds = ImageUtils.drawString(lAnnotation, lG2D,  0, 0, lWrappingWidth, lWrappingHeight, true, true, lJustification, false);

          lIterations++;
        }
      }

      //Draw text
      pRenderer.addObject(lSelectedSet + Integer.toString(pParentGeoID), new RenderableObject(lSelectedSet + Integer.toString(pGeoID), new Marker(lPoint, lAnnotation, lBounds, lFontSize, lSelectedSet + Integer.toString(pParentGeoID)), pRenderNode));
    }
    else {
      pRenderer.addObject(lSelectedSet + Integer.toString(pParentGeoID), new RenderableObject(lSelectedSet + Integer.toString(pGeoID), new Marker(lPoint, null, null, 0, lSelectedSet + Integer.toString(pParentGeoID)), pRenderNode));
    }
  } // drawSpatialMarker

} // SpatialRenderer
