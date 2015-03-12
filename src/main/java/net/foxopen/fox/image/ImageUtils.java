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
package net.foxopen.fox.image;

import ch.randelshofer.media.jpeg.JPEGImageIO;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;

import java.io.File;
import java.io.IOException;

import java.sql.SQLException;

import javax.imageio.ImageIO;

import javax.imageio.ImageTypeSpecifier;

import oracle.sql.BLOB;

import net.foxopen.fox.ex.ExInternal;


public class ImageUtils {

  private static final Object THUMB_KEY_INTERPOLATION = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
  private static final Object THUMB_KEY_RENDERING = RenderingHints.VALUE_RENDER_QUALITY;
  
  public static Dimension getResizedDimensions(int pCurrentWidth, int pCurrentHeight, double lScalingFactor, int lMaxBoundarySize) {
    if (lScalingFactor != 0) {//If scaling factor specified then calculate the new height and width using this
    } else if (lMaxBoundarySize != 0) {//If no scaling factor specified and max boundary given, calculate new height and width so largest boundary is size given
      lScalingFactor = Math.max(lMaxBoundarySize/(double)pCurrentHeight, lMaxBoundarySize/(double)pCurrentWidth);
    } else {
      return new Dimension(pCurrentWidth,pCurrentHeight);
    }
    return new Dimension((int)Math.round(pCurrentWidth * lScalingFactor),(int)Math.round(pCurrentHeight * lScalingFactor));
  }
  
  public static BufferedImage resizeImage(BufferedImage pBufferedImage, double pScalingFactor, int pMaxBoundarySize) {
    int lWidth = pBufferedImage.getWidth();
    int lHeight = pBufferedImage.getHeight();
    Dimension lDim = getResizedDimensions(lWidth,lHeight,pScalingFactor,pMaxBoundarySize);
    
    return renderNewImage(pBufferedImage, (int)lDim.getWidth(), (int)lDim.getHeight());
  }
  
  private static BufferedImage createBufferedImage(BufferedImage pBufferedImage, int pNewWidth, int pNewHeight) {
    int lImageType = pBufferedImage.getType();
    ColorModel lColorModel = pBufferedImage.getColorModel();
    if(lImageType == BufferedImage.TYPE_CUSTOM) { //This shouldn't really be the case, but we need to treat it specially
      return ImageTypeSpecifier.createFromRenderedImage(pBufferedImage).createBufferedImage(pNewWidth, pNewHeight);
    }
    else if(lColorModel instanceof IndexColorModel) {
      return new BufferedImage(pNewWidth, pNewHeight, lImageType, (IndexColorModel)pBufferedImage.getColorModel());
    }
    else {
      return new BufferedImage(pNewWidth, pNewHeight, lImageType);
    }
  }
  
  private static BufferedImage createBufferedImage(BufferedImage pBufferedImage, Dimension pDim) {
    return createBufferedImage(pBufferedImage,(int)pDim.getWidth(),(int)pDim.getHeight());
  }
  
  //Takes in a BufferedImage item and re-scales it to pNewWidth and pNewHeight
  private static BufferedImage renderNewImage(BufferedImage pBufferedImage, int pNewWidth, int pNewHeight) {
    BufferedImage lTempImage = createBufferedImage(pBufferedImage,pNewWidth,pNewHeight);
    
    Graphics2D g2 = lTempImage.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, THUMB_KEY_INTERPOLATION);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, THUMB_KEY_RENDERING);
    g2.drawImage(pBufferedImage, 0, 0, pNewWidth, pNewHeight, null);
    g2.dispose();
    
    return lTempImage;
  }
  
  public static BufferedImage generateTextImage(int pWidth, int pHeight, String pText) {
    BufferedImage lTempImage = new BufferedImage(pWidth,pHeight,BufferedImage.TYPE_INT_ARGB);
    
    Graphics2D g = lTempImage.createGraphics();
    g.setColor(Color.WHITE);
    g.drawRect(0,0,pWidth,pHeight);
    g.setColor(Color.BLACK);
    g.drawString(pText,0,pHeight/2);
    g.dispose();
    
    return lTempImage;
  }
  
  public static int getNormalisedRotation(int pRotation) {
    pRotation = pRotation % 360;
    if(pRotation < 0) {
      pRotation = pRotation + 360;
    }
    return pRotation;
  }
  
  public static Dimension getRotatedDimensions(int pCurrentWidth, int pCurrentHeight, int pDegrees) {
    pDegrees = getNormalisedRotation(pDegrees);
    if(pDegrees == 0 || pDegrees == 180) {
      return new Dimension(pCurrentWidth,pCurrentHeight);
    }
    else if(pDegrees == 90 || pDegrees == 270) {//PG: Really don't like how much of this is hardcoded but the image doesn't rotate properly otherwise - and it beats just repeating rotate 90's
      return new Dimension(pCurrentHeight,pCurrentWidth);
    }
    else {
      throw new ExInternal("Unsupported rotation operation: "+pDegrees);
    }
  }
  
  public static BufferedImage rotate(BufferedImage pBufferedImage, int pDegrees) {
    int lImageWidth = pBufferedImage.getWidth();
    int lImageHeight = pBufferedImage.getHeight();
    BufferedImage lRotatedBufferedImage = null;
    AffineTransform tx = new AffineTransform();
    
    pDegrees = getNormalisedRotation(pDegrees);
    if(pDegrees==0) return pBufferedImage;

    lRotatedBufferedImage = createBufferedImage(pBufferedImage,getRotatedDimensions(lImageWidth,lImageHeight,pDegrees));
    if(pDegrees==90) {//PG: Really don't like how much of this is hardcoded but the image doesn't rotate properly otherwise - and it beats just repeating rotate 90's
      tx.rotate(Math.toRadians(pDegrees), lImageHeight/2.0, lImageHeight/2.0);
    }
    else if(pDegrees==180) {
      tx.rotate(Math.toRadians(pDegrees), lImageWidth/2.0, lImageHeight/2.0);
    }
    else if(pDegrees==270) {
      tx.rotate(Math.toRadians(pDegrees), lImageWidth/2.0, lImageWidth/2.0);
    }
    else {
      throw new ExInternal("Unsupported rotation operation: "+pDegrees);
    }

    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    op.filter(pBufferedImage, lRotatedBufferedImage);

    return lRotatedBufferedImage;
  }
  
  public static BufferedImage convertBlobToImage (BLOB pBlob) throws SQLException, IOException {
    BufferedImage bi;
    try {
      bi = ImageIO.read(pBlob.getBinaryStream());
    }
    catch(IOException ioe) {
      bi = JPEGImageIO.read(pBlob.getBinaryStream());
      if(bi==null) {
        throw new ExInternal("Reading image failed",ioe);
      }
    }
    if(bi == null) {
      throw new ExInternal("Reading image failed");
    }
    
    return bi;
  }




  //Draw a wrapped sting
  private static boolean isBreakable(char c) {
    return (c == ' ' || c == '\t' || c == '-' || c == '\\' || c == '/' || c == '_');
  }
  private static boolean isNewLine(char c) {
    return (c == '\n' || c == '\r');
  }

  /**
   * Draw a string onto a graphics2d object, wrapping words at pWidth pixels wide.
   * If a word has to be cut, if less than 75% is cut off it's added back on and the word shrunk, if pShrinkWords == true
   * 
   * @param pString Words to draw
   * @param pG Graphics2D object reference used to draw
   * @param pX Top left ordinate of where to start drawing down from
   * @param pY Top left ordinate of where to start drawing down from
   * @param pWidth Width to wrap string at in pixels
   * @param pHeight Height to cut off overflow at in pixels
   * @param pYOverflow Boolean to allow overflow or not
   * @param pShrinkWords Boolean to shrink words that are bigger than the width
   * @param pJustification Left/right/centre line justification
   * @param pDraw Boolean to switch drawing on or off (handy for just getting bounds before drawing)
   * @return Rectangle of the bounding box
   */
  public static Rectangle drawString (String pString, Graphics2D pG, int pX, int pY, int pWidth, int pHeight, boolean pYOverflow, boolean pShrinkWords, String pJustification, boolean pDraw) {
    FontMetrics lFontMetrics = pG.getFontMetrics();
    Font lDrawFont = pG.getFont();
    char[] lCharacters = pString.toCharArray();
    int lFontHeight = lFontMetrics.getHeight();
    int lLeading = 0;//lFontMetrics.getLeading()/2;
    int lY = pY;
    boolean lFirstLine = true;
    boolean lSuppressLastLine = false;
    Integer lLastSpace = null;
    int lCharsInBuffer = 0;
    StringBuffer lLineBuffer = new StringBuffer();

    for (int i = 0; i < lCharacters.length; ++i) {
      if (lFontMetrics.stringWidth(lLineBuffer.toString() + lCharacters[i]) < pWidth) {
        if (!isNewLine(lCharacters[i])) {
          lLineBuffer.append(lCharacters[i]);
          lCharsInBuffer++;
          if (isBreakable(lCharacters[i])) {
            lLastSpace = new Integer(i);
          }
        }
        else {
          //Find the next Y position for the new line
          lY += lFontHeight + (lFirstLine?-lFontMetrics.getDescent():lLeading);

          if ((lY + lFontHeight) > pHeight && pYOverflow == false) {
            //Break loop if it's hit the Y limit
            break;
          }

          //Draw the line
          if (pDraw) {
              drawLine(lLineBuffer.toString(), pG, pX, lY, pWidth, pJustification);
          }

          //Start the new buffer
          lLineBuffer.setLength(0);
          lLineBuffer.append(lCharacters[i]);
          lLastSpace = null;
          lCharsInBuffer = 0;

          lFirstLine = false;
        }
      }
      else {
        //Try and go back to the last space
        if (lLastSpace != null) {
          //Space found in line, cut down buffer and hop i back a bit
          int lNextBreak = 0;
          int j;
          for (j = i;  j < Math.min((i + (int)(lCharsInBuffer * 0.15)), lCharacters.length); ++j) {
            if (isBreakable(lCharacters[j])) {
              lNextBreak = j;
              break;
            }
          }
          if (j == lCharacters.length) {
            lNextBreak = lCharacters.length;
          }

          if (lNextBreak != 0 && pShrinkWords) {
            String lNewString = lLineBuffer.toString() + pString.substring(i, j);
            int lFontSize;
            for (lFontSize = lDrawFont.getSize(); lFontSize > 1; lFontSize--) {
              Font lNewSmallerFont = new Font(lDrawFont.getName(), lDrawFont.getStyle(), lFontSize);
              pG.setFont(lNewSmallerFont);
              if (pG.getFontMetrics().stringWidth(lNewString) < pWidth) {
                lY += pG.getFontMetrics().getHeight() + (lFirstLine?-pG.getFontMetrics().getDescent():lLeading);
                if (pDraw) {
                    drawLine(lNewString, pG, pX, lY, pWidth, pJustification);
                }

                i = j+1;

                pG.setFont(lDrawFont);
                break;

              }
            }
            if (i >= lCharacters.length) {
              lSuppressLastLine = true;
              break;
            }
            pG.setFont(lDrawFont);
            
            lLineBuffer.setLength(0);
            lLineBuffer.append(lCharacters[i]);
            lLastSpace = null;
            lCharsInBuffer = 0;
            lFirstLine = false;
            continue;
          }
          else {
            lLineBuffer.setLength(lCharsInBuffer - (i - lLastSpace.intValue()) + 1);
            i = lLastSpace.intValue() + 1;
          }
        }
        else if (pShrinkWords) {
          //Shrink word if It's not too big
          int lNextBreak = 0;
          int j;
          for (j = i;  j < Math.min((i + (int)(lCharsInBuffer * 0.50)), lCharacters.length); ++j) {
            if (isBreakable(lCharacters[j])) {
              lNextBreak = j;
              break;
            }
          }
          if (j == lCharacters.length) {
            lNextBreak = lCharacters.length;
          }
          if (lNextBreak != 0) {
            String lNewString = lLineBuffer.toString() + pString.substring(i, j);
            int lFontSize;
            for (lFontSize = lDrawFont.getSize(); lFontSize > 1; lFontSize--) {
              Font lNewSmallerFont = new Font(lDrawFont.getName(), lDrawFont.getStyle(), lFontSize);
              pG.setFont(lNewSmallerFont);
              if (pG.getFontMetrics().stringWidth(lNewString) < pWidth) {
                lY += pG.getFontMetrics().getHeight() + (lFirstLine?-pG.getFontMetrics().getDescent():lLeading);
                if (pDraw) {
                    drawLine(lNewString, pG, pX, lY, pWidth, pJustification);
                }

                i = j+1;

                pG.setFont(lDrawFont);
                break;

              }
            }
            if (i >= lCharacters.length) {
              lSuppressLastLine = true;
              break;
            }
            pG.setFont(lDrawFont);
            
            lLineBuffer.setLength(0);
            lLineBuffer.append(lCharacters[i]);
            lLastSpace = null;
            lCharsInBuffer = 0;
            lFirstLine = false;
            continue;
          }
        }

        //Find the next Y position for the new line
        lY += lFontHeight + (lFirstLine?-lFontMetrics.getDescent():lLeading);
        
        if ((lY + lFontHeight) > pHeight && pYOverflow == false) {
          //Break loop if it's hit the Y limit
          break;
        }

        //Draw the line
        if (pDraw) {
            drawLine(lLineBuffer.toString(), pG, pX, lY, pWidth, pJustification);
        }

        //Start the new buffer
        lLineBuffer.setLength(0);
        lLineBuffer.append(lCharacters[i]);
        lLastSpace = null;
        lCharsInBuffer = 0;

        lFirstLine = false;
      }
    }

    if (!lSuppressLastLine) {
      //Find the next Y position for the new line
      lY = lY + lFontHeight + (lFirstLine?-lFontMetrics.getDescent():lLeading);
  
      //Draw the line
      if (pDraw) {
        drawLine(lLineBuffer.toString(), pG, pX, lY, pWidth, pJustification);
      }
    }

    //Calculate final bounding box
    int lHeight = lY + lFontMetrics.getDescent() - pY;
    int lWidth = pWidth;
    Rectangle lR = new Rectangle(pX, pY, lWidth, lHeight);
    return lR;
  } // drawString

  /**
   * Draw a line of text with justifiction
   * 
   * @param pLine String of text to draw
   * @param pG Graphics object to draw on
   * @param pX Position to start at
   * @param pY Position to start at
   * @param pWidth Width of the line, to know where to right align to
   * @param pJustification LEFT|RIGHT|CENTRE justification
   */
  public static void drawLine (String pLine, Graphics2D pG, int pX, int pY, int pWidth, String pJustification) {
    FontMetrics lFontMetrics = pG.getFontMetrics();
    if (pLine.trim().length() > 0) {
      if ("RIGHT".equals(pJustification.toUpperCase())) {
        pG.drawString(pLine.trim(), pX + pWidth - lFontMetrics.stringWidth(pLine.trim()), pY);
      }
      else if ("CENTRE".equals(pJustification.toUpperCase())) {
        pG.drawString(pLine.trim(), pX + (pWidth/2) - (lFontMetrics.stringWidth(pLine.trim())/2), pY);
      }
      else {
        pG.drawString(pLine.trim(), pX, pY);
      }
    }
  } // drawLine

}
