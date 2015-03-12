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

import net.foxopen.fox.ComponentImage;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Joins together multiple images, for example, transparent GIF files. Images
 * can be built up by instantiating with a base layer (Image or BufferedImage)
 * and then adding layers on top of it one by one. At any stage, the caller can
 * use getBufferedImage to retrieve the current combined image.
 * Supports PNG and GIF with transparency - other formats may work - untested.
 *
 * NOTE: Java 1.4 does not support writing GIF files natively due to patent
 * and licensing issues. This has been resolved in Java 1.6. PNG should be used
 * as a workaround for this.
 */
public class ImageCombinator
{
  private final BufferedImage mBufferedImage;
  private String mContentType = null;

  /**
   * Constructs a new ImageCombinator for joining together images.
   * @param pFoxComponent ComponentImage to convert to BufferedImage and use as base layer
   * @throws ExInternal
   * @throws ExServiceUnavailable
   */
  public ImageCombinator (ComponentImage pFoxComponent)
  throws ExInternal, ExServiceUnavailable {
    if (pFoxComponent != null) {
      mContentType = pFoxComponent.getType();
      mBufferedImage = createBufferedImage(pFoxComponent);
    }
    else {
      throw new ExInternal("Null ComponentImage passed to ImageCombinator constructor");
    }
  } // ImageCombinator

  /**
   * Adds an image layer to the existing image.
   * @param pFoxComponent ComponentImage to add as layer
   * @throws ExInternal
   */
  public void addLayer (ComponentImage pFoxComponent)
  throws ExInternal {
    if (pFoxComponent != null) {
      BufferedImage lTemp = createBufferedImage(pFoxComponent);
      // Belt and braces check. Not a necessity (larger images will scale down appropriately), but further
      // metadata would be required to correctly position smaller images on a larger canvas
      if (lTemp.getWidth() == mBufferedImage.getWidth() && lTemp.getHeight() == mBufferedImage.getHeight()) {
        drawImageToBuffer(mBufferedImage, lTemp);
      }
      else {
        // Images could have many layers, so provide a reasonable error message to speed up resolution of size mismatching
        throw new ExInternal("Image dimensions must match: base image is " +  mBufferedImage.getWidth() + "x" + mBufferedImage.getHeight()
          + ", new layer image ('" + pFoxComponent.getName() + "') is " + lTemp.getWidth() + "x" + lTemp.getHeight());
      }
    }
    else {
      throw new ExInternal("Null ComponentImage passed to ImageCombinator.addLayer()");
    }
  } // addLayer

  /**
   * Gets the byte array from the ComponentImage passed and converts to a BufferedImage.
   * @param pFoxComponent ComponentImage to convert to BufferedImage
   * @return new BufferedImage instance
   * @throws ExInternal
   */
  private BufferedImage createBufferedImage (ComponentImage pFoxComponent)
  throws ExInternal
  {
    // Check that added layers match the type
    if (!mContentType.equals(pFoxComponent.getType())) {
      throw new ExInternal("Image types must match: base image is " + mContentType + ", new layer image ("
        + pFoxComponent.getName() + ") is " + pFoxComponent.getType());
    }

    try {
      // Get base image
      BufferedImage lTempComponent = ImageIO.read(new ByteArrayInputStream(pFoxComponent.getByteArray()));

      // Create temporary buffer as image with alpha transparency
      BufferedImage lTempBuffer = new BufferedImage(lTempComponent.getWidth(), lTempComponent.getHeight(), BufferedImage.TYPE_INT_ARGB);

      // Draw image onto transparency (ensures that all transparent layers in the future
      // are drawn correctly - fixes buggy appearance when using non-transparent base image
      drawImageToBuffer(lTempBuffer, lTempComponent);
      return lTempBuffer;
    }
    catch (IOException ex) {
      throw new ExInternal("Couldn't read image in ImageCombinator.createBufferedImage", ex);
    }
  } // createBufferedImage

  /**
   * Draws an Image on top of a BufferedImage.
   * @param pBufferedImage the BufferedImage to use as the base layer
   * @param pImageToDraw the Image to draw over the top
   * @throws ExInternal
   */
  private static void drawImageToBuffer (BufferedImage pBufferedImage, Image pImageToDraw)
  throws ExInternal {
    Graphics lGraphics = pBufferedImage.createGraphics();
    lGraphics.drawImage(pImageToDraw, 0, 0, null);
    lGraphics.dispose();
  } // drawImageToBuffer

  /**
   * Get the current BufferedImage as a byte array.
   * @return byte array
   * @throws ExInternal
   */
  public byte[] getOutputByteArray()
  throws ExInternal {
    return getByteArrayOutputStream().toByteArray();
  } // getOutputByteArray

  /**
   * Get the current BufferedImage via an output stream.
   * @return byte array stream
   * @throws ExInternal
   */
  public ByteArrayOutputStream getByteArrayOutputStream()
  throws ExInternal {
    ByteArrayOutputStream lByteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ImageIO.write(mBufferedImage, mContentType.replaceAll("image/",""), lByteArrayOutputStream);
    }
    catch (IOException ex) {
      throw new ExInternal("Could not write BufferedImage to byte array output stream in ImageCombinator.getOutputByteArray()", ex);
    }
    return lByteArrayOutputStream;
  } // getByteArrayOutputStream

  /**
   * Gets the height of the current base image buffer.
   * @return height
   */
  public int getHeight () {
    return mBufferedImage.getHeight();
  } // getHeight

  /**
   * Gets the width of the current base image buffer.
   * @return width
   */
  public int getWidth () {
    return mBufferedImage.getWidth();
  } // getWidth

  /**
   * Gets the content type of the base image.
   * @return content type as String
   */
  public String getContentType () {
    return mContentType;
  } // getContentType
} // class ImageCombinator
