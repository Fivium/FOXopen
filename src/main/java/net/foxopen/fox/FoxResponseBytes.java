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

import net.foxopen.fox.ex.ExInternal;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;


public class FoxResponseBytes
extends FoxResponse {
  private final byte[] mByteArray;
  private final String mContentType;
  private final long mBrowserCacheMilliSec;

  public FoxResponseBytes(
    String pContentType
  , byte[] pByteArray
  , long pBrowserCacheMilliSec
  )
  {
    mByteArray = pByteArray;
    mContentType = pContentType;
    mBrowserCacheMilliSec = pBrowserCacheMilliSec;
  }

  public void respond(FoxRequest pRequest)
  throws ExInternal
  {
     HttpServletResponse lHttpServletResponse = pRequest.getHttpResponse();

    // TODO - NP - Not sure about this, why no content-type/length set when other headers set? Could check for collisions?
    // Fox header processing
    if(this.getHttpHeaderList() == null || this.getHttpHeaderList().size() == 0) {
      lHttpServletResponse.setContentType(mContentType);
      lHttpServletResponse.setContentLength(mByteArray.length);
      if(mBrowserCacheMilliSec > 0) {
        lHttpServletResponse.setDateHeader("Expires", mBrowserCacheMilliSec + System.currentTimeMillis());
      }
    }
    // HttpMessage header processing
    else {
      setResponseHttpHeaders(lHttpServletResponse);
    }

    // Get the response content writer
    try {
      OutputStream lOutputStream = lHttpServletResponse.getOutputStream();
      lOutputStream.write(mByteArray);
      lOutputStream.close();
    }
    catch(IOException lIOException) {
      throw new ExInternal("Write ByteArray response content", lIOException);
    }
  }

  public final String getBase64Data() {
    return new String(Base64.encodeBase64(mByteArray));
  }

  public final String getBase64DataURLText() {
    return "data:"+mContentType+";base64,"+getBase64Data();
  }

  public final byte[] getBytes() {
    return mByteArray;
  }

}
