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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class FoxRequest {

  public abstract String getRequestLogId();

  /**
   * Gets the path info for the current request, i.e. the part after the context name and servlet name. The URI will have
   * a preceding slash.
   * @return Current request path.
   */
  public abstract String getRequestURI();

  /**
   * Gets the path info for the current request, i.e. the part after the context name and servlet name. The URI will have
   * a preceding slash.
   * @return Current request path.
   */
  public abstract StringBuffer getRequestURIStringBuffer();

  /**
   * Gets the path info for the current request, i.e. the part after the context name and servlet name. The URI will have
   * a preceding slash.
   * @return Current request path.
   */
  public abstract StringBuilder getRequestURIStringBuilder();

  public abstract void addCookie (String pName, String pValue);

  public abstract void addCookie (String pName, String pValue, int pMaxAge);

  public abstract void addCookie (String pName, String pValue, boolean pSetPath);

  public abstract void addCookie (String pName, String pValue, boolean pSetPath, boolean pHttpOnly);

  public abstract void addCookie (String pName, String pValue, int pMaxAge, boolean pSetPath);

  public abstract void addCookie (String pName, String pValue, int pMaxAge, boolean pSetPath, boolean pHttpOnly);

  public abstract void removeCookie (String pName, boolean pSetPath);

  public abstract void removeCookie (String pName);

  public abstract String getCookieValue (String pCookieName);

  public abstract HttpServletRequest getHttpRequest ();

  public abstract HttpServletResponse getHttpResponse ();

  /**
   * Gets the named parameter from the request, or null if it is not defined. For parameters with multiple values the
   * first value is returned.
   * @param pParameterName
   * @return
   */
  public abstract String getParameter(String pParameterName);

}
