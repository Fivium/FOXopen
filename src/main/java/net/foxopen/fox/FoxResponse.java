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

import org.apache.commons.httpclient.Header;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public abstract class FoxResponse {
  private final List<Header> mHttpHeaderList = new ArrayList<>();
  private final List<BeforeResponseAction> mBeforeResponseActions = new ArrayList<>();

  protected int mStatusCode = HttpServletResponse.SC_OK;

  /**
   * Sets HTTP headers on a response based on whats contained in the header list
   */
  protected void setResponseHttpHeaders(HttpServletResponse pResponse) {
    for(Header lHeader : mHttpHeaderList) {
      // Overwrites a previous header with the same name. Use addHeader to allow multiple values
      pResponse.setHeader(lHeader.getName(), lHeader.getValue());
    }
  }


  protected List getHttpHeaderList() {
    return mHttpHeaderList;
  }

  /**
   * Adds an HTTP header to a local list. The headers are not applied to a response
   * until setResponseHttpHeaders is called
   */
  public void setHttpHeader(String pName, String pValue) {
    mHttpHeaderList.add(new Header(pName, pValue));
  }

  public void setStatus(int pStatusCode) {
    mStatusCode = pStatusCode;
  }

  public int getStatusCode() {
    return mStatusCode;
  }

  public abstract void respond(FoxRequest pRequest);

  /**
   * Registers a BeforeResponseAction which will be invoked before this response is sent.
   * @param pAction Action to register.
   */
  public void addBeforeResponseAction(BeforeResponseAction pAction) {
    mBeforeResponseActions.add(pAction);
  }

  /**
   * Executes all BeforeResponseActions registered on this FoxResponse in the order they were added. Consumers MUST
   * call this before any response is sent.
   */
  protected void runBeforeResponseActions() {
    mBeforeResponseActions.forEach(e -> e.beforeResponse(this));
  }

  /**
   * Actions which should be performed before a response starts to be sent to the user, i.e. JIT setting of headers.
   */
  public interface BeforeResponseAction {
    void beforeResponse(FoxResponse pFoxResponse);
  }
}
