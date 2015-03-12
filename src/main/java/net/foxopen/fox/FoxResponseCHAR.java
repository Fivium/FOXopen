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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;


// TODO - Could do with less StringBuffers and more StringBuilders
public class FoxResponseCHAR
extends FoxResponse {
  private final StringBuffer mStringBuffer;
  private final String mContentType;
  private final long mBrowserCacheMilliSec;

  public FoxResponseCHAR(
    String pContentType
  , StringBuffer pStringBuffer
  , long pBrowserCacheMilliSec
  )
  {
    mStringBuffer = pStringBuffer;
    mContentType = pContentType;
    mBrowserCacheMilliSec = pBrowserCacheMilliSec;
  }

  public void respond(FoxRequest pRequest)
  throws ExInternal
  {
    // Set the response header
    pRequest.getHttpResponse().setStatus(mStatusCode);
    pRequest.getHttpResponse().setContentType(mContentType);
    pRequest.getHttpResponse().setDateHeader("Expires", mBrowserCacheMilliSec > 0 ? mBrowserCacheMilliSec + System.currentTimeMillis() : 0); // No cache

    setResponseHttpHeaders(pRequest.getHttpResponse());

    // TODO - Is this really the best way to convert the stringbuffer to UTF8? What about other charsets
    // Process UTF-8 Character set encoding translation
    if(mContentType.indexOf("UTF-8") != -1) {
      byte[] lByteArray;
      try {
        lByteArray = mStringBuffer.toString().getBytes("UTF-8");
      }
      catch (UnsupportedEncodingException e) {
        throw new ExInternal("Character encoding error", e);
      }

      pRequest.getHttpResponse().setContentLength(lByteArray.length);

      try {
        OutputStream lOutputStream = pRequest.getHttpResponse().getOutputStream();
        lOutputStream.write(lByteArray);
        lOutputStream.close();
      }
      catch (IOException e) {
        throw new ExInternal("Stream write CHAR response content", e);
      }
    }
    // Old code that did not cope with character sets
    // NB: This could still be used for static HTML from database, CSS, JavaScript etc
    else {
      // Get the response content writer
      pRequest.getHttpResponse().setContentLength(mStringBuffer.length());
      try {
        PrintWriter lPrintWriter = pRequest.getHttpResponse().getWriter();
        lPrintWriter.print(mStringBuffer.toString());
        lPrintWriter.close();
      }
      catch(IOException lIOException) {
        throw new ExInternal("Print CHAR response content", lIOException);
      }
    }
  }

}
