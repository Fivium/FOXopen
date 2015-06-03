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
package net.foxopen.fox.spatial.renderer;

import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

/**
 * This needs a lot more work done to it:
 * <ul>
 *    <li>Stop char overlaps on concave bends</li>
 *    <li>Optional flip over line</li>
 *    <li>Stretch To Fit needs to be thoroughly tested (Should also shrink text too)</li>
 *    <li>Code for conditions where text simply wont fit at any reasonable size</li>
 *    <li>Text Alignment (Especially centred) would be good</li>
 * </ul>
 *
 * Modified from: http://www.jhlabs.com/java/java2d/strokes/
 */
public class TextStroke implements Stroke {
  private String mText;
  private Font mFont;
  private boolean mStretchToFit = false;
  private boolean mRepeat = false;
  private AffineTransform mTransform = new AffineTransform();
  private static final float FLATNESS = 1;

  public TextStroke(String pText, Font pFont) {
    this(pText, pFont, false, false);
  }

  public TextStroke(String pText, Font pFont, boolean pStretchToFit, boolean pRepeat) {
    mText = pText;
    mFont = pFont;
    mStretchToFit = pStretchToFit;
    mRepeat = pRepeat;
  }

  public Shape createStrokedShape(Shape shape) {
    FontRenderContext lFRC = new FontRenderContext(null, true, true);
    GlyphVector lGlyphVector = mFont.createGlyphVector(lFRC, mText);

    GeneralPath lResult = new GeneralPath();
    PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
    float points[] = new float[6];
    float moveX = 0, moveY = 0;
    float lastX = 0, lastY = 0;
    float thisX = 0, thisY = 0;
    int type = 0;
    boolean first = false;
    float next = 0;
    int currentChar = 0;
    int length = lGlyphVector.getNumGlyphs();

    if (length == 0)
    return lResult;

    float lKerningFactor = mStretchToFit ? measurePathLength(shape)/(float)lGlyphVector.getLogicalBounds().getWidth() : 1.0f;
    float nextAdvance = 0;

    while (currentChar < length && !it.isDone()) {
      type = it.currentSegment(points);
      switch(type){
        case PathIterator.SEG_MOVETO:
          moveX = lastX = points[0];
          moveY = lastY = points[1];
          lResult.moveTo(moveX, moveY);
          first = true;
          nextAdvance = lGlyphVector.getGlyphMetrics(currentChar).getAdvance() * 0.5f;
          next = nextAdvance;
          break;

        case PathIterator.SEG_CLOSE:
          points[0] = moveX;
          points[1] = moveY;
          // Fall into....

        case PathIterator.SEG_LINETO:
          thisX = points[0];
          thisY = points[1];
          float dx = thisX-lastX;
          float dy = thisY-lastY;
          float distance = (float)Math.sqrt(dx*dx + dy*dy);
          if (distance >= next) {
            float r = 1.0f/distance;
            float angle = (float)Math.atan2(dy, dx);
            while (currentChar < length && distance >= next) {
                Shape glyph = lGlyphVector.getGlyphOutline(currentChar);
                Point2D p = lGlyphVector.getGlyphPosition(currentChar);
                float px = (float)p.getX();
                float py = (float)p.getY();
                float x = lastX + next * dx * r;
                float y = lastY + next * dy * r;
                float advance = nextAdvance;
                nextAdvance = currentChar < length-1 ? lGlyphVector.getGlyphMetrics(currentChar+1).getAdvance() * 0.5f : 0;
                mTransform.setToTranslation(x, y);
                mTransform.rotate(angle);
                mTransform.translate(-px - advance, -py);
                lResult.append(mTransform.createTransformedShape(glyph), false);
                next += (advance+nextAdvance) * lKerningFactor;
                currentChar++;
                if (mRepeat) {
                  currentChar %= length;
                }
            }
          }
          next -= distance;
          first = false;
          lastX = thisX;
          lastY = thisY;
          break;
      }
      it.next();
    }

    return lResult;
  }

  public float measurePathLength(Shape pShape) {
    PathIterator it = new FlatteningPathIterator(pShape.getPathIterator(null), FLATNESS);
    float points[] = new float[6];
    float moveX = 0, moveY = 0;
    float lastX = 0, lastY = 0;
    float thisX = 0, thisY = 0;
    int type = 0;
    float total = 0;

    while (!it.isDone()) {
      type = it.currentSegment(points);
      switch(type){
        case PathIterator.SEG_MOVETO:
          moveX = lastX = points[0];
          moveY = lastY = points[1];
          break;

        case PathIterator.SEG_CLOSE:
          points[0] = moveX;
          points[1] = moveY;
          // Fall into....

        case PathIterator.SEG_LINETO:
          thisX = points[0];
          thisY = points[1];
          float dx = thisX-lastX;
          float dy = thisY-lastY;
          total += (float)Math.sqrt(dx*dx + dy*dy);
          lastX = thisX;
          lastY = thisY;
          break;
      }
      it.next();
    }

    return total;
  }
}
