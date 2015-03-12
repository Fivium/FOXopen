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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class FoxResponseByteStream
extends FoxResponse {
  private final HttpServletResponse mHttpServletResponse;

  public FoxResponseByteStream(
  String pContentType
  , FoxRequest pFoxRequest
  , long pBrowserCacheMilliSec
  )
  {
    this(pContentType, pFoxRequest, pBrowserCacheMilliSec, HttpServletResponse.SC_OK);
  }

  public FoxResponseByteStream(
  String pContentType
  , FoxRequest pFoxRequest
  , long pBrowserCacheMilliSec
  , int pStatusCode
  )
  {
    mHttpServletResponse = pFoxRequest.getHttpResponse();
    mHttpServletResponse.setStatus(pStatusCode);
    mHttpServletResponse.setContentType(pContentType);
    mHttpServletResponse.setDateHeader("Expires", pBrowserCacheMilliSec > 0 ? pBrowserCacheMilliSec + System.currentTimeMillis() : 0); // No cache
  }

  public OutputStream getHttpServletOutputStream() {
    setResponseHttpHeaders(mHttpServletResponse);
    try {
      return mHttpServletResponse.getOutputStream();
    }
    catch (IOException e) {
      throw new ExInternal("Cannot get OutputStream for FoxResponseByteStream", e);
    }
  }

  /**
   * @see javax.servlet.ServletResponse#flushBuffer()
   * @throws IOException
   */
  public void flushBuffer() throws IOException {
    mHttpServletResponse.flushBuffer();
  }

  /**
   * Respond methods have no effect on Stream type response classes
   *
   * @param pRequest
   * @throws ExInternal
   */
  @Deprecated
  public void respond(FoxRequest pRequest)
    throws ExInternal  {
    // Do nothing
  }

  @Deprecated
  public void setStatus(int pStatusCode) {
    throw new ExInternal("Called setStatus on a FoxResponseByteStream which will have no effect, status needs to be set at response construction time");
  }
}
