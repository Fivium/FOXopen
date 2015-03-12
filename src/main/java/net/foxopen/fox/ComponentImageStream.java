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

import java.io.IOException;
import java.io.InputStream;

import java.io.Reader;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExSecurity;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.io.IOUtil;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;


public class ComponentImageStream extends FoxComponent {
    private final String mName;
    private final ImageInfo mImageInfo;
    private final InputStream mImageStream;;
    private final long mBrowserCacheMilliSec;
    private final String mMimeType;
    private final UCon mUConToClose;



    public ComponentImageStream(
      String pName
    , InputStream pImageStream
    , InputStream pImageStream2 //Optional, needed if pImageStream is not able to be reset
    , String pExpectedMimeType
    , long pBrowserCacheMilliSec
    , UCon pUConToClose //Optional - close this connection after output stream is complete
    )
    throws ExApp // when unsupported image format
    {
      // Record name
      mName = pName;
      mImageStream = pImageStream;
      if(pImageStream2==null)
        pImageStream2 = pImageStream;
      mBrowserCacheMilliSec = pBrowserCacheMilliSec;

      mUConToClose = pUConToClose;

      // Determine Image Properties (size, Mime type etc)
      mImageInfo = new ImageInfo();
      mImageInfo.setInput(pImageStream2); // in can be InputStream or RandomAccessFile
      if (!mImageInfo.check()) {
        throw new ExApp("Not a supported image file format: "+mName);
      }
      try {
        pImageStream2.reset();
      } catch (IOException e) {
      }

  //    if(Bug.gLoggingEnabled) {
  //      Bug.log(mImageInfo.getFormatName() + ", "
  //      +  mImageInfo.getMimeType() + ", " + mImageInfo.getWidth() + " x " + mImageInfo.getHeight() + " pixels, "
  //      + mImageInfo.getBitsPerPixel() + " bits per pixel, " + mImageInfo.getNumberOfImages() + " image(s).");
  //    }

      // Validate MimeType (warning only)
      mMimeType=mImageInfo.getMimeType();
      String lExpectedMimeType = XFUtil.nvl(pExpectedMimeType, "image");
      if(!lExpectedMimeType.equals("image") && !lExpectedMimeType.equals(mMimeType)) {
        Track.debug("FoxSysLogWarning", "Image " + pName + " is stored on database as type " + pExpectedMimeType
        + " but is actually of type " + mMimeType, TrackFlag.FOX_SYS_LOG_WARNING);
      }

      // Build standard binary response
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
      FoxResponseByteStream mFoxResponse = new FoxResponseByteStream(mMimeType, pRequest, mBrowserCacheMilliSec);
      try {
        IOUtil.transfer(mImageStream,mFoxResponse.getHttpServletOutputStream(),64000);
      } catch (IOException e) {
        throw new ExInternal("Error streaming BLOB.", e);
      } finally{
        if(mUConToClose != null)
          mUConToClose.closeForRecycle();
      }
      return mFoxResponse;
    }

  //  public FoxResponse getFoxResponseBytes()
  //  {
  //    return mFoxResponse;
  //  }

    public int getWidth () {
      return mImageInfo.getWidth();
    }

    public int getHeight () {
      return mImageInfo.getHeight();
    }

    public final String getType() {
      return mImageInfo.getMimeType();
    }

    public final InputStream getInputStream() {
      return mImageStream;
    }

  //  public final byte[] getByteArray () {
  //    return mFoxResponse.getBytes();
  //  }
  @Override
  public Reader getReader() {
    // TODO Implement this method
    return null;
  }
}
