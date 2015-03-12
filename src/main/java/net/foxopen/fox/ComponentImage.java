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
package net.foxopen.fox;

import net.foxopen.fox.ex.*;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;


public final class ComponentImage
extends FoxComponent
{

  private final String mName;
  private final FoxResponseBytes mFoxResponse;
  private final ImageInfo mImageInfo;

  public ComponentImage(
    String pName
  , byte[] pImageBytes
  , String pExpectedMimeType
  , long pBrowserCacheMilliSec
  )
  throws ExApp // when unsupported image format
  {
    // Record name
    mName = pName;

    // Determine Image Properties (size, Mime type etc)
    ByteArrayInputStream lByteArrayInputStream = new ByteArrayInputStream(pImageBytes);
    mImageInfo = new ImageInfo();
    mImageInfo.setInput(lByteArrayInputStream); // in can be InputStream or RandomAccessFile
    if (!mImageInfo.check()) {
      throw new ExApp("Not a supported image file format: "+mName);
    }
    try {
      lByteArrayInputStream.close();
    }
    catch(IOException x) {
      // no action
    }
//    if(Bug.gLoggingEnabled) {
//      Bug.log(mImageInfo.getFormatName() + ", "
//      +  mImageInfo.getMimeType() + ", " + mImageInfo.getWidth() + " x " + mImageInfo.getHeight() + " pixels, "
//      + mImageInfo.getBitsPerPixel() + " bits per pixel, " + mImageInfo.getNumberOfImages() + " image(s).");
//    }

    // Validate MimeType (warning only)
    String lMimeType=mImageInfo.getMimeType();
    String lExpectedMimeType = XFUtil.nvl(pExpectedMimeType, "image");
    if(!lExpectedMimeType.equals("image") && !lExpectedMimeType.equals(lMimeType)) {
      Track.debug("FoxSysLogWarning", "Image " + pName + " is stored on database as type " + pExpectedMimeType
      + " but is actually of type " + lMimeType, TrackFlag.FOX_SYS_LOG_WARNING);
    }

    // Build standard binary response
    mFoxResponse = new FoxResponseBytes(lMimeType, pImageBytes, pBrowserCacheMilliSec);

  }

  public String getName()
  {
    return mName;
  }

  public FoxResponse processResponse(
    FoxRequest pRequest
  , StringBuffer pURLTail
  )
  throws
    ExInternal
  , ExSecurity            // when browser attempting to access thread owned by another session
  , ExUserRequest
  , ExModule
  , ExServiceUnavailable
  {
    return mFoxResponse;
  }

  public FoxResponseBytes getFoxResponseBytes()
  {
    return mFoxResponse;
  }

  public int getWidth () {
    return mImageInfo.getWidth();
  }

  public int getHeight () {
    return mImageInfo.getHeight();
  }

  public final String getType() {
    return mImageInfo.getMimeType();
  }

  public final byte[] getByteArray () {
    return mFoxResponse.getBytes();
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(mFoxResponse.getBytes());
  }

  @Override
  public Reader getReader() {
    return null;
  }
}
