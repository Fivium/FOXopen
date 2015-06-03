package net.foxopen.fox.spatial;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.image.ImageUtils;
import net.foxopen.fox.spatial.renderer.Marker;
import net.foxopen.fox.spatial.renderer.PolyLine;
import net.foxopen.fox.spatial.renderer.RenderableObject;
import net.foxopen.fox.spatial.renderer.Renderer;
import net.foxopen.fox.spatial.renderer.RendererEMF;
import net.foxopen.fox.spatial.renderer.RendererPNG;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;
import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.io.OutputStream;
import java.sql.SQLException;

public class FoxInternalSpatialRenderer implements SpatialRenderer {

  protected FoxInternalSpatialRenderer(DOM pRendererConfig) {
  }

  @Override
  public String getRenderXmlKey() {
    return "INTERNAL_MAP_REQUEST";
  }

  @Override
  public void processRenderXML(RequestContext pRequestContext, DOM pRenderXML, OutputStream pOutputStream) {
    Track.pushInfo("SpatialRenderer", "Fox Internal Spatial Rendering started");
    Track.timerStart("SpatialRenderer");
    try {
      int lHeight, lWidth, lDPI;
      lHeight = Integer.parseInt(pRenderXML.get1SNoEx("/INTERNAL_MAP_REQUEST/HEIGHT"));
      lWidth = Integer.parseInt(pRenderXML.get1SNoEx("/INTERNAL_MAP_REQUEST/WIDTH"));
      lDPI = Integer.parseInt(XFUtil.nvl(pRenderXML.get1SNoEx("/INTERNAL_MAP_REQUEST/DPI"), "72"));

      UCon lUCon = pRequestContext.getContextUCon().getUCon("Spatial Renderer");
      try {
        Renderer lRenderer = new RendererPNG(pOutputStream, lWidth, lHeight, lDPI, Color.decode(pRenderXML.get1SNoEx("/INTERNAL_MAP_REQUEST/BGCOLOR")));
        internalRender(pRenderXML, lUCon, lRenderer);
      }
      finally {
        pRequestContext.getContextUCon().returnUCon(lUCon, "Spatial Renderer");
      }
    }
    finally {
      Track.timerPause("SpatialRenderer");
      Track.pop("SpatialRenderer");
    }
  }

  private void internalRender(DOM pRequestDOM, UCon pUCon, Renderer pRenderer) {
    int lHeight, lWidth;
    lHeight = Integer.parseInt(pRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/HEIGHT"));
    lWidth = Integer.parseInt(pRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/WIDTH"));

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

    Track.pushInfo("AddingRenderableObjects", "Internal rendering: Adding renderable objects to renderer");
    try {
      DOMList lRenderNodes = pRequestDOM.getUL("/INTERNAL_MAP_REQUEST/RENDER_LIST/RENDER");
      ParsedStatement lDataQuery;
      for (int lNodeID = 0; lNodeID < lRenderNodes.getLength(); ++lNodeID) {
        DOM lRenderNode = lRenderNodes.item(lNodeID);

        try {
          lDataQuery = StatementParser.parse(lRenderNode.get1SNoEx("DATA_QUERY"), "Data Query");
          java.util.List<UConStatementResult> lDataRows = pUCon.queryMultipleRows(lDataQuery);
          for (UConStatementResult lRow : lDataRows) {
            int lGeoID = lRow.getInteger("ID");
            int lParentGeoID = lRow.getInteger("PARENT_SID_ID");
            STRUCT st = (oracle.sql.STRUCT)lRow.getObject("GEO_DATA");
            JGeometry lGeometry = JGeometry.load(st);

            //Parse geometry
            if (lGeometry.getType() == JGeometry.GTYPE_POINT) {
              drawSpatialMarker(pRenderer, lRenderNode, lGeoID, lParentGeoID, lGeometry, lOrdinateTransform, lRow, lScaleFactorX, lScaleFactorY);
            }
            else if (lGeometry.getType() == JGeometry.GTYPE_MULTIPOINT) {
              drawSpatialNodes(pRenderer, lRenderNode, lGeoID, lParentGeoID, lGeometry, lOrdinateTransform);
            }
            else if (lGeometry.getType() == JGeometry.GTYPE_CURVE) {
              drawSpatialLine(pRenderer, lRenderNode, lGeoID, lParentGeoID, lGeometry, lOrdinateTransform, lRow);
            }
            else if (lGeometry.getType() == JGeometry.GTYPE_POLYGON) {
              drawSpatialArea(pRenderer, lRenderNode, lGeoID, lParentGeoID, lGeometry, lOrdinateTransform, lRow);
            }
          }
        }
        catch (ExParser | ExDB | SQLException e) {
          throw new ExInternal("Fox Internal Spatial Renderer failed when attempting to query the spatial data for rendering", e);
        }
      }
    }
    finally {
      Track.pop("AddingRenderableObjects");
    }

    // Get the renderer to generate the image (goes to the output stream given to the pRenderer constructor)
    Track.pushInfo("GeneratingImage", "Getting the renderer to generate the image");
    try {
      pRenderer.generate();
    }
    finally {
      Track.pop("GeneratingImage");
    }

  } // internalRender

  /**
   * Transform a point from spatial ordinates to pixel ordinates
   *
   * @param pPoint The point to transform
   * @param pTransformation An affine transform to apply
   * @return Transformed point
   */
  private Point2D transformPoint (Point2D pPoint, AffineTransform pTransformation) {
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
   */
  private void drawSpatialArea (Renderer pRenderer, DOM pRenderNode, int pGeoID, int pParentGeoID, JGeometry pGeometry, AffineTransform pTransformation, UConStatementResult pRow) {
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
        Point2D lP = transformPoint(new Point2D.Double(((double[])lOrdinateGroups[lElementID])[lOrdinateID], ((double[])lOrdinateGroups[lElementID])[lOrdinateID + 1]), pTransformation);
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

    // Testing annotations on areas...
    if (pRow.columnExists("ANNO_TEXT")) {
      Point2D lPoint = new Point2D.Double(lPolygon.getBounds().getX(), lPolygon.getBounds().getCenterY());
      pRenderNode.addElem("TEXT").addElem("JUSTIFICATION", "centre");
      pRenderer.addObject(lSelectedSet + Integer.toString(pParentGeoID), new RenderableObject(lSelectedSet + Integer.toString(pGeoID)+"marker", new Marker(lPoint, pRow.getString("ANNO_TEXT"), lPolygon.getBounds(), 12, lSelectedSet + Integer.toString(pParentGeoID)), pRenderNode));
    }
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
   */
  private void drawSpatialLine (Renderer pRenderer, DOM pRenderNode, int pGeoID, int pParentGeoID, JGeometry pGeometry, AffineTransform pTransformation, UConStatementResult pRow) {
    String lSelectedSet = pRenderNode.get1SNoEx("SELECTED_SET");

    Object[] lOrdinateGroups = pGeometry.getOrdinatesOfElements();
    for (int lElementID = 0; lElementID < lOrdinateGroups.length; ++ lElementID) {
      int lCoords = (((double[])lOrdinateGroups[lElementID]).length/2);
      int[] x = new int[lCoords];
      int[] y = new int[lCoords];
      int lCoordID = 0;
      for (int lOrdinateID = 0; lOrdinateID < ((double[])lOrdinateGroups[lElementID]).length; lOrdinateID = lOrdinateID + 2) {
        Point2D lP = transformPoint(new Point2D.Double(((double[])lOrdinateGroups[lElementID])[lOrdinateID], ((double[])lOrdinateGroups[lElementID])[lOrdinateID + 1]), pTransformation);
        x[lCoordID] = new Double(lP.getX()).intValue();
        y[lCoordID] = new Double(lP.getY()).intValue();

        lCoordID++;
      }

      PolyLine lNewLine;
      if (pRow.columnExists("ANNO_TEXT")) {
        lNewLine = new PolyLine(x, y, pRow.getString("ANNO_TEXT"));
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
  private void drawSpatialNodes (Renderer pRenderer, DOM pRenderNode, int pGeoID, int pParentGeoID, JGeometry pGeometry, AffineTransform pTransformation) {
    String lSelectedSet = pRenderNode.get1SNoEx("SELECTED_SET");

    Object[] lOrdinateGroups = pGeometry.getOrdinatesOfElements();
    for (int lElementID = 0; lElementID < lOrdinateGroups.length; ++ lElementID) {
      for (int lOrdinateID = 0; lOrdinateID < ((double[])lOrdinateGroups[lElementID]).length; lOrdinateID = lOrdinateID + 2) {
        Point2D lP = transformPoint(new Point2D.Double(((double[])lOrdinateGroups[lElementID])[lOrdinateID], ((double[])lOrdinateGroups[lElementID])[lOrdinateID + 1]), pTransformation);

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
   * @param pScaleFactorX Scaling factor for keeping text the right size while zooming
   * @param pScaleFactorY Scaling factor for keeping text the right size while zooming
   */
  private void drawSpatialMarker (Renderer pRenderer, DOM pRenderNode, int pGeoID, int pParentGeoID, JGeometry pGeometry, AffineTransform pTransformation, UConStatementResult pRow, double pScaleFactorX, double pScaleFactorY) {
    // Convert spatial point to pixel point
    Point2D lPoint = transformPoint(pGeometry.getJavaPoint(), pTransformation);

    Graphics2D lG2D = pRenderer.getGraphics2D();

    String lSelectedSet = pRenderNode.get1SNoEx("SELECTED_SET");

    if (pRow.columnExists("ANNO_TEXT")) {
      //Set string
      String lAnnotation = pRow.getString("ANNO_TEXT");

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
      JGeometry lParentBoundsGeom = (JGeometry)pRow.getObject("PARENT_GEO");
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
}
