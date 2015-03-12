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

import net.foxopen.fox.ComponentImage;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.image.ImageUtils;
import net.foxopen.fox.image.XBufferedImage;
import net.foxopen.fox.track.Track;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;


public class RendererPNG extends Renderer {
  private Graphics2D mG2D;
  private XBufferedImage mCanvas;

  public RendererPNG (int pWidth, int pHeight, int pDPI, Color pBackGroundColour) {
    //Create canvas and graphics object
    if (pDPI == 72) {
      mCanvas = new XBufferedImage(pWidth, pHeight, BufferedImage.TYPE_INT_ARGB);
    }
    else {
      mCanvas = new XBufferedImage(pWidth, pHeight, BufferedImage.TYPE_INT_ARGB, pDPI, "pixels");
    }
    mG2D = mCanvas.createGraphics();

    //Add hints for nicer drawing
    mG2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    mG2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    mG2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    mG2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    mG2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    mG2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    //Paint background
    if (pBackGroundColour != null) {
      Paint lOriginalPaint = mG2D.getPaint();
      mG2D.setPaint(pBackGroundColour);
      mG2D.fillRect(0, 0, pWidth, pHeight);
      mG2D.setPaint(lOriginalPaint);
    }
  }

  public Graphics2D getGraphics2D() {
    return mG2D;
  }

  /**
   * Generate an EMF image from the renderable objects stored on the renderer object
   * @return byte array of the EMF image
   * @throws IOException
   */
  public byte[] generate()
    throws IOException {
    //Render objects, starting with root nodes
    Iterator lI = mRootLevelRenderableObjects.iterator();
    while (lI.hasNext()) {
      renderObject((RenderableObject)mRenderableObjectsMap.get(lI.next()));
    }

    //Convert to PNG and return
    ByteArrayOutputStream lOutputStream = new ByteArrayOutputStream();
    mCanvas.convertToPNG(lOutputStream);
    return lOutputStream.toByteArray();
  } // generate

  /**
   * Render a renderable object to the image along with any objects contained in it
   * @param pRenderableObject RenderableObject to render to the image
   */
  private void renderObject(RenderableObject pRenderableObject) {
    //Draw object to PNG
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
      Track.debug("PNGRenderer", "Null renderable object: " + pRenderableObject.mKey);
    }
    else {
      Track.debug("PNGRenderer", "Unknown renderable object class type: " + pRenderableObject.mKey + ":" + lObject.getClass().getName());
    }

    //Draw children
    if (pRenderableObject.mChildren.size() != 0) {
      Iterator lI = pRenderableObject.mChildren.iterator();
      while (lI.hasNext()) {
        RenderableObject lRO = (RenderableObject)lI.next();
        renderObject(lRO);
      }
    }
  } // renderObject

  /**
   * Draw an area on the image according to pStyle
   * @param pArea Area object
   * @param pStyle XML render style
   */
  private void drawArea(Area pArea, DOM pStyle) {
    //Get original drawing settings
    Color lOriginalColor = mG2D.getColor();
    Stroke lOriginalStroke = mG2D.getStroke();

    //Set up for draw
    String lBGColour = pStyle.get1SNoEx("BACKGROUND_COLOUR");
    String lBGOpacity = pStyle.get1SNoEx("FILL_OPACITY");
    if (XFUtil.isNull(lBGOpacity)) {
      mG2D.setColor(Color.decode(lBGColour));
    }
    else {
      Integer intval = Integer.decode(lBGColour);
      int i = intval.intValue();
      mG2D.setColor(new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, Integer.parseInt(lBGOpacity)));
    }

    //Draw
    mG2D.fill(pArea);

    String lBorderC = pStyle.get1SNoEx("COLOUR");
    if (lBorderC != null && !"".equals(lBorderC)) {
      String lBorderOpacity = pStyle.get1SNoEx("LINE_OPACITY");
      if (XFUtil.isNull(lBorderOpacity)) {
        mG2D.setColor(Color.decode(lBorderC));
      }
      else {
        Integer intval = Integer.decode(lBorderC);
        int i = intval.intValue();
        mG2D.setColor(new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, Integer.parseInt(lBorderOpacity)));
      }

      String lDashPattern = pStyle.get1SNoEx("DASH_PATTERN");
      if (XFUtil.isNull(lDashPattern)) {
        mG2D.setStroke(new BasicStroke(1));
      }
      else {
        String[] lDP = lDashPattern.split(",");
        float[] lDashes = new float[lDP.length];
        for (int i = 0; i < lDP.length; ++i) {
          lDashes[i] = Float.parseFloat(lDP[i]);
        }
        mG2D.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, lDashes, 0.0f));
      }

      mG2D.draw(pArea);
    }

    //Restore original drawing settings
    mG2D.setStroke(lOriginalStroke);
    mG2D.setColor(lOriginalColor);
  } // drawArea

  /**
   * Draw a line on the image according to pStyle
   * @param pLine PolyLine object
   * @param pStyle XML render style
   */
  private void drawLine(PolyLine pLine, DOM pStyle) {
    //Get original drawing settings
    Stroke lOriginalStroke = mG2D.getStroke();
    Color lOriginalColor = mG2D.getColor();

    //Set colour / stroke style for draw
    String lColour = pStyle.get1SNoEx("COLOUR");
    String lOpacity = pStyle.get1SNoEx("LINE_OPACITY");
    if (XFUtil.isNull(lOpacity)) {
      mG2D.setColor(Color.decode(lColour));
    }
    else {
      Integer intval = Integer.decode(lColour);
      int i = intval.intValue();
      mG2D.setColor(new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, Integer.parseInt(lOpacity)));
    }

    String lDashPattern = pStyle.get1SNoEx("DASH_PATTERN");
    float lLineThickness = new Float(XFUtil.nvl(pStyle.get1SNoEx("LINE_THICKNESS"), "1")).floatValue();
    if (XFUtil.isNull(lDashPattern)) {
      mG2D.setStroke(new BasicStroke(lLineThickness));
    }
    else {
      String[] lDP = lDashPattern.split(",");
      float[] lDashes = new float[lDP.length];
      for (int i = 0; i < lDP.length; ++i) {
        lDashes[i] = Float.parseFloat(lDP[i]);
      }
      mG2D.setStroke(new BasicStroke(lLineThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, lDashes, 0.0f));
    }

    mG2D.drawPolyline(pLine.mXPoints, pLine.mYPoints, pLine.mXPoints.length);

    //Draw text on a line if there is any
    if (!XFUtil.isNull(pLine.mAnnotation)) {
      mG2D.setStroke(new TextStroke(pLine.mAnnotation, new Font("Sans Serif", Font.BOLD, 20)));
      mG2D.drawPolyline(pLine.mXPoints, pLine.mYPoints, pLine.mXPoints.length);
    }

    //Restore original drawing settings
    mG2D.setStroke(lOriginalStroke);
    mG2D.setColor(lOriginalColor);
  } // drawLine

  /**
   * Draw a marker on the image according to pStyle
   * @param pMarker Marker object of a point and possible text
   * @param pStyle XML render style
   */
  private void drawMarker(Marker pMarker, DOM pStyle) {
    //Get original drawing settings
    Stroke lOriginalStroke = mG2D.getStroke();
    Color lOriginalColor = mG2D.getColor();

    //Draw Marker
    if ("CIRCLE".equals(pStyle.get1SNoEx("MARKER_STYLE"))) {
      drawPoint(pMarker.mPoint, pStyle);
    }
    else if ("IMAGE".equals(pStyle.get1SNoEx("MARKER_STYLE"))) {
      String lImageComponent = pStyle.get1SNoEx("IMAGE/COMPONENT");
      String lImageLayout = XFUtil.nvl(pStyle.get1SNoEx("IMAGE/LAYOUT"), "CC");
      if (!XFUtil.isNull(lImageComponent)) {
        AffineTransform lOriginalTransform = mG2D.getTransform();
        try {
          ComponentImage lFoxComponent = (ComponentImage)ComponentManager.getComponent(lImageComponent);

          //Set up possible scaling // TODO This is not very robust or cover all options, needs to be polished
          AffineTransform lImageMarkerTransform = mG2D.getTransform();
          double lScaleWidth = 1, lScaleHeight = 1;
          if (pStyle.get1EOrNull("IMAGE/SCALE") != null && getObject(pMarker.mParentObjectKey).mObject instanceof Area) {
            Rectangle pParentBounds = ((Area)getObject(pMarker.mParentObjectKey).mObject).getBounds();
            if ("auto".equals(pStyle.get1SNoEx("IMAGE/SCALE/WIDTH")) && pStyle.get1EOrNull("IMAGE/SCALE") != null && pStyle.get1EOrNull("IMAGE/SCALE").getAttrOrNull("method") != null && "PROPORTIONAL".equals(pStyle.get1EOrNull("IMAGE/SCALE").getAttrOrNull("method").toUpperCase())) {
              double lBuffer = XFUtil.nvl(pStyle.get1SNoEx("IMAGE/SCALE/WIDTH_BUFFER"), 1);
              lScaleWidth = (pParentBounds.getWidth() - lBuffer*2) / lFoxComponent.getWidth();
              lScaleHeight = lScaleWidth;
            }
            else if ("auto".equals(pStyle.get1SNoEx("IMAGE/SCALE/HEIGHT")) && pStyle.get1EOrNull("IMAGE/SCALE") != null && pStyle.get1EOrNull("IMAGE/SCALE").getAttrOrNull("method") != null && "PROPORTIONAL".equals(pStyle.get1EOrNull("IMAGE/SCALE").getAttrOrNull("method").toUpperCase())) {
              double lBuffer = XFUtil.nvl(pStyle.get1SNoEx("IMAGE/SCALE/HEIGHT_BUFFER"), 1);
              lScaleHeight = (pParentBounds.getWidth() - lBuffer*2) / lFoxComponent.getWidth();
              lScaleWidth = lScaleHeight;
            }
            lImageMarkerTransform.scale(lScaleWidth, lScaleHeight);
            mG2D.setTransform(lImageMarkerTransform);
          }

          Point2D lPoint = new Point2D.Double();
          lImageMarkerTransform.createInverse().transform( pMarker.mPoint, lPoint);

          Point2D lImagePosition = getMarkerPoint(lPoint, lImageLayout, new Rectangle(0, 0, new Double(lFoxComponent.getWidth()).intValue(), lFoxComponent.getHeight()));

          mG2D.drawImage(ImageIO.read(new ByteArrayInputStream(lFoxComponent.getByteArray())), new Double(lImagePosition.getX()).intValue(), new Double(lImagePosition.getY()).intValue(), null);
        }
        catch (Exception e) {
          throw new ExInternal("Couldn't find the image \"" + lImageComponent + "\" to render as a marker", e);
        }
        finally {
          mG2D.setTransform(lOriginalTransform);
        }
      }
    }

    if (!XFUtil.isNull(pMarker.mText)) {
      //Set up and draw text
      String lColour = pStyle.get1SNoEx("TEXT/COLOUR");
      String lOpacity = pStyle.get1SNoEx("TEXT/OPACITY");
      if (XFUtil.isNull(lOpacity)) {
        mG2D.setColor(Color.decode(lColour));
      }
      else {
        Integer intval = Integer.decode(lColour);
        int i = intval.intValue();
        mG2D.setColor(new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, Integer.parseInt(lOpacity)));
      }

      String lJustification = pStyle.get1SNoEx("TEXT/JUSTIFICATION").toUpperCase();

      int lFontStyle = Font.PLAIN;
      if ("bold".equals(pStyle.get1EOrNull("TEXT/FONT").getAttrOrNull("style"))) {
        lFontStyle = Font.BOLD;
      }

      mG2D.setFont(new Font(pStyle.get1SNoEx("TEXT/FONT"), lFontStyle, pMarker.mFontSize));

      //Set pen starting point based on layout and bounds
      Point2D lTextStart = getMarkerPoint(pMarker.mPoint, pStyle.get1SNoEx("TEXT/LAYOUT"), pMarker.mBounds);

      //Draw text
      Rectangle lBounds = ImageUtils.drawString(pMarker.mText, mG2D, (int)lTextStart.getX(),(int)lTextStart.getY(), pMarker.mBounds.width, pMarker.mBounds.height, true, true, lJustification, true);

      if ("TRUE".equals(pStyle.get1SNoEx("TEXT/DEBUG").toUpperCase())) {
        mG2D.setColor(new Color(255,0,0));
        mG2D.setStroke(new BasicStroke(1));
        mG2D.draw(lBounds);
      }
    }

    //Restore original drawing settings
    mG2D.setStroke(lOriginalStroke);
    mG2D.setColor(lOriginalColor);
  } // drawMarker

  /**
   * Draw an icon on the image at pPoint according to pStyle
   * @param pPoint Point2D of the centre of the icon to draw
   * @param pStyle XML render style
   */
  private void drawPoint(Point2D pPoint, DOM pStyle) {
    //Get original drawing settings
    Stroke lOriginalStroke = mG2D.getStroke();
    Color lOriginalColor = mG2D.getColor();

    //Set drawing style
    String lColour = pStyle.get1SNoEx("COLOUR");
    String lOpacity = pStyle.get1SNoEx("FILL_OPACITY");
    if (XFUtil.isNull(lOpacity)) {
      mG2D.setColor(Color.decode(lColour));
    }
    else {
      Integer intval = Integer.decode(lColour);
      int i = intval.intValue();
      mG2D.setColor(new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, Integer.parseInt(lOpacity)));
    }

    //Draw ( TODO Circle hard coded for now, should be configurable )
    mG2D.fillOval(new Double(pPoint.getX()).intValue()-2, new Double(pPoint.getY()).intValue()-2, 5, 5);

    //Restore original drawing settings
    mG2D.setStroke(lOriginalStroke);
    mG2D.setColor(lOriginalColor);
  } // drawPoint
}
