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
package net.foxopen.fox.renderer;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.image.ImageUtils;
import net.foxopen.fox.track.Track;
import org.freehep.graphicsio.emf.EMFGraphics2D;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;


public class RendererEMF extends Renderer {
  private EMFGraphics2D mEMF;
  private ByteArrayOutputStream mOS = new ByteArrayOutputStream();

  public RendererEMF(int pWidth, int pHeight, int pDPI, Color pBackGroundColour) {
    //Set up EMF:
    mEMF = new EMFGraphics2D(mOS, new Dimension(pWidth, pHeight));
    mEMF.setDeviceIndependent(true);

    if (pBackGroundColour != null) {
      Paint lOriginalPaint = mEMF.getPaint();
      mEMF.setPaint(pBackGroundColour);
      mEMF.fillRect(0, 0, pWidth, pHeight);
      mEMF.setPaint(lOriginalPaint);
    }
  }

  public Graphics2D getGraphics2D() {
    return mEMF;
  }

  /**
   * Generate an EMF image from the renderable objects stored on the renderer object
   * @return byte array of the EMF image
   * @throws IOException
   */
  public byte[] generate()
    throws IOException {
    mEMF.startExport();
    mEMF.setComposite(AlphaComposite.Clear);

    //Render objects, starting with root nodes
    Iterator lI = mRootLevelRenderableObjects.iterator();
    while (lI.hasNext()) {
      renderObject((RenderableObject)mRenderableObjectsMap.get(lI.next()));
    }

    mEMF.endExport();
    return mOS.toByteArray();
  } // generate

  /**
   * Render a renderable object to the image along with any objects contained in it
   * @param pRenderableObject RenderableObject to render to the image
   */
  private void renderObject(RenderableObject pRenderableObject)
    throws IOException {
    //Start group for possible children
    if (pRenderableObject.mChildren.size() > 0) {
      mEMF.writeStartGroup(pRenderableObject.getBounds());
    }

    //Draw object to emf
    Object lObject = pRenderableObject.mObject;
    if (lObject instanceof Marker) {
      drawMarker((Marker)lObject, pRenderableObject.mStyle);
    }
    else if (lObject instanceof Area) {
      drawArea((Area)lObject, pRenderableObject.mStyle);
    }
    else if (lObject instanceof Point2D) {
      drawPoint((Point2D)lObject, pRenderableObject.mStyle);
    }
    else if (lObject instanceof PolyLine) {
      drawLine((PolyLine)lObject, pRenderableObject.mStyle);
    }
    else if (lObject == null) {
      Track.debug("EMFRenderer", "Null renderable object: " + pRenderableObject.mKey);
    }
    else {
      Track.debug("EMFRenderer", "Unknown renderable object class type: " + pRenderableObject.mKey + ":" + lObject.getClass().getName());
    }

    //Draw children
    if (pRenderableObject.mChildren.size() != 0) {
      Iterator lI = pRenderableObject.mChildren.iterator();
      while (lI.hasNext()) {
        RenderableObject lRO = (RenderableObject)lI.next();
        renderObject(lRO);
      }
    }

    //End group for possible children
    if (pRenderableObject.mChildren.size() > 0) {
      mEMF.writeEndGroup();
    }
  } // render

  /**
   * Draw an area on the image according to pStyle
   * @param pArea Area object
   * @param pStyle XML render style
   */
  private void drawArea(Area pArea, DOM pStyle)
    throws IOException {
    //Get original drawing settings
    Stroke lOriginalStroke = mEMF.getStroke();
    Color lOriginalColor = mEMF.getColor();

    //Set up for draw
    String lBGColour = pStyle.get1SNoEx("BACKGROUND_COLOUR");
    String lBGOpacity = pStyle.get1SNoEx("FILL_OPACITY");
    Color lBackgroundColour;
    if (XFUtil.isNull(lBGOpacity)) {
      lBackgroundColour = Color.decode(lBGColour);
    }
    else {
      Integer intval = Integer.decode(lBGColour);
      int i = intval.intValue();
      lBackgroundColour = new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, Integer.parseInt(lBGOpacity));
    }

    //Draw
    String lBorderC = pStyle.get1SNoEx("COLOUR");
    if (!XFUtil.isNull(lBorderC)) {
      String lBorderOpacity = pStyle.get1SNoEx("LINE_OPACITY");
      if (XFUtil.isNull(lBorderOpacity)) {
        mEMF.setColor(Color.decode(lBorderC));
      }
      else {
        int intval = Integer.decode(lBorderC).intValue();
        mEMF.setColor(new Color((intval >> 16) & 0xFF, (intval >> 8) & 0xFF, intval & 0xFF, Integer.parseInt(lBorderOpacity)));
      }

      mEMF.writeStartGroup(pArea.getBounds());
      mEMF.fillAndDraw(pArea, lBackgroundColour);
      mEMF.writeEndGroup();
    }
    else {
      mEMF.setColor(lBackgroundColour);
      mEMF.fill(pArea);
    }

    //Restore original drawing settings
    mEMF.setStroke(lOriginalStroke);
    mEMF.setColor(lOriginalColor);
  } // drawArea

  /**
   * Draw a line on the image according to pStyle
   * @param pLine PolyLine object
   * @param pStyle XML render style
   */
  private void drawLine(PolyLine pLine, DOM pStyle) {
    //Get original drawing settings
    Stroke lOriginalStroke = mEMF.getStroke();
    Color lOriginalColor = mEMF.getColor();

    //Set colour / stroke style for draw (//TODO Only solid lines, dashes don't work in powerpoint. Fix this some day...)
    String lColour = pStyle.get1SNoEx("COLOUR");
    String lOpacity = pStyle.get1SNoEx("LINE_OPACITY");
    if (XFUtil.isNull(lOpacity)) {
      mEMF.setColor(Color.decode(lColour));
    }
    else {
      Integer intval = Integer.decode(lColour);
      int i = intval.intValue();
      mEMF.setColor(new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, Integer.parseInt(lOpacity)));
    }

    mEMF.drawPolyline(pLine.mXPoints, pLine.mYPoints, pLine.mXPoints.length);

    //Restore original drawing settings
    mEMF.setStroke(lOriginalStroke);
    mEMF.setColor(lOriginalColor);
  } // drawLine

  /**
   * Draw a marker on the image according to pStyle
   * @param pMarker Marker object of a point and possible text
   * @param pStyle XML render style
   */
  private void drawMarker(Marker pMarker, DOM pStyle)
    throws IOException {
    //Get original drawing settings
    Stroke lOriginalStroke = mEMF.getStroke();
    Color lOriginalColor = mEMF.getColor();

    if (!XFUtil.isNull(pMarker.mText)) {
      mEMF.writeStartGroup(pMarker.getBounds());
    }

    //Draw Marker
    if ("CIRCLE".equals(pStyle.get1SNoEx("MARKER_STYLE"))) {
      drawPoint(pMarker.mPoint, pStyle);
    }
    else if ("IMAGE".equals(pStyle.get1SNoEx("MARKER_STYLE"))) {
      //TODO While the functions are there, freehep doesn't actually draw the image for unknown reasons
//      String lImageURL = pStyle.get1SNoEx("IMAGE");
//      if (!XFUtil.isNull(lImageURL)) {
//        App lApp;
//        try {
//          lApp = App.getAppByMnem(App.getServerResourceDOM().get1SNoEx("document-server/application-mnem"));
//        } catch (Exception e) {
//          throw new ExInternal("Cannot get App for rendering marker image url", e);
//        }
//        try {
//          ComponentImage lFoxComponent = (ComponentImage)lApp.getComponent(lImageURL);
//          Point2D lImagePosition = getMarkerPoint(pMarker.mPoint, pStyle.get1SNoEx("TEXT/LAYOUT"), new Rectangle(0, 0, lFoxComponent.getHeight(), lFoxComponent.getWidth()));
//          mEMF.drawImage(ImageIO.read(new ByteArrayInputStream(lFoxComponent.getByteArray())), new Double(lImagePosition.getX()).intValue(), new Double(lImagePosition.getY()).intValue(), null);
//        }
//        catch (Exception e) {
//          throw new ExInternal("Couldn't find the image \"" + lImageURL + "\" to render as a marker", e);
//        }
//      }
    }

    if (!XFUtil.isNull(pMarker.mText)) {
      //Set up and draw text
      mEMF.writeStartGroup(pMarker.getBounds());

      String lColour = pStyle.get1SNoEx("TEXT/COLOUR");
      String lOpacity = pStyle.get1SNoEx("TEXT/OPACITY");
      if (XFUtil.isNull(lOpacity)) {
        mEMF.setColor(Color.decode(lColour));
      }
      else {
        Integer intval = Integer.decode(lColour);
        int i = intval.intValue();
        mEMF.setColor(new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, Integer.parseInt(lOpacity)));
      }

      String lJustification = pStyle.get1SNoEx("TEXT/JUSTIFICATION").toUpperCase();

      int lFontStyle = Font.PLAIN;
      if ("bold".equals(pStyle.get1EOrNull("TEXT/FONT").getAttrOrNull("style"))) {
        lFontStyle = Font.BOLD;
      }

      mEMF.setFont(new Font(pStyle.get1SNoEx("TEXT/FONT"), lFontStyle, pMarker.mFontSize));

      //Set pen starting point based on layout and bounds
      Point2D lTextStart = getMarkerPoint(pMarker.mPoint, pStyle.get1SNoEx("TEXT/LAYOUT"), pMarker.mBounds);

      //Draw text
      Rectangle lBounds = ImageUtils.drawString(pMarker.mText, mEMF, (int)lTextStart.getX(), (int)lTextStart.getY(), pMarker.mBounds.width, pMarker.mBounds.height, true, true, lJustification, true);

      if ("TRUE".equals(pStyle.get1SNoEx("TEXT/DEBUG").toUpperCase())) {
        mEMF.setColor(new Color(255,0,0));
        mEMF.setStroke(new BasicStroke(1));
        mEMF.draw(lBounds);
      }

      mEMF.writeEndGroup();
    }

    if (!XFUtil.isNull(pMarker.mText)) {
      mEMF.writeEndGroup();
    }

    //Restore original drawing settings
    mEMF.setStroke(lOriginalStroke);
    mEMF.setColor(lOriginalColor);
  } // drawMarker

  /**
   * Draw an icon on the image at pPoint according to pStyle
   * @param pPoint Point2D of the centre of the icon to draw
   * @param pStyle XML render style
   */
  private void drawPoint(Point2D pPoint, DOM pStyle) {
    //Get original drawing settings
    Stroke lOriginalStroke = mEMF.getStroke();
    Color lOriginalColor = mEMF.getColor();

    //Set drawing style
    String lColour = pStyle.get1SNoEx("COLOUR");
    String lOpacity = pStyle.get1SNoEx("FILL_OPACITY");
    if (XFUtil.isNull(lOpacity)) {
      mEMF.setColor(Color.decode(lColour));
    }
    else {
      Integer intval = Integer.decode(lColour);
      int i = intval.intValue();
      mEMF.setColor(new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, Integer.parseInt(lOpacity)));
    }

    //Draw ( TODO Circle hard coded for now, should be configurable )
    mEMF.fillOval(new Double(pPoint.getX()).intValue()-2, new Double(pPoint.getY()).intValue()-2, 5, 5);

    //Restore original drawing settings
    mEMF.setStroke(lOriginalStroke);
    mEMF.setColor(lOriginalColor);
  } // drawPoint
}
