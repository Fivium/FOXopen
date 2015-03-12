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
package net.foxopen.fox.webservices;

import java.io.IOException;

import java.util.HashMap;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.Mod;

import org.apache.commons.io.IOUtils;


/**
 * Builds a WebServiceRequestWrapper from a static context based on a
 * FoxRequest.
 */
public abstract class WebServiceRequestWrapperFactory
{
  // Holds URI to Factory implementation mapping
  private static HashMap mWebServiceRequestWrapperFactories = new HashMap();
  static {
    mWebServiceRequestWrapperFactories.put(WebServiceRequestWrapperSOAP.SOAP_NAMESPACE_URI, new WebServiceRequestWrapperFactorySOAP());
  }

  /**
   * Builds a WebServiceRequestWrapper from a static context based on a FoxRequest.
   * @param pFoxRequest the request containing a possible web service
   * @param pMod the endpoint module
   * @return a concrete WebServiceRequestWrapper implementation
   * @throws ExUserRequest invalid request
   */
  public static WebServiceRequestWrapper createWebServiceWrapper (FoxRequest pFoxRequest, Mod pMod)
  throws ExUserRequest {

    //TODO PN this has been modified from original impelmentation and not tested
    String lString;
    try {
      lString = IOUtils.toString(pFoxRequest.getHttpRequest().getReader());
    }
    catch (IOException e) {
      throw new ExInternal("Failed to read request body",e);
    }

    // Check that content is not null
    if (lString == null) {
      throw new ExInternal("Null DOM passed to WebServiceWrapper.createWebServiceWrapper");
    }

    // Parse incoming DOM
    DOM lRequestDOM = DOM.createDocumentFromXMLString(lString);

    // No namespace found, we don't support this case
    String lNamespaceURI = lRequestDOM.getNamespaceURI();
    if (XFUtil.isNull(lNamespaceURI)) {
      throw new ExUserRequest ("Namespace not found on web service request root node - cannot ascertain support for request type.");
    }

    // Lookup namespace in map
    WebServiceRequestWrapperFactory lFactory = (WebServiceRequestWrapperFactory) mWebServiceRequestWrapperFactories.get(lNamespaceURI);
    if (lFactory == null) {
      throw new ExInternal("Web service requests with namespace '" + lNamespaceURI + "' are not supported.");
    }
    else {
      // Build a wrapper and return
      return lFactory.createWebServiceWrapper(pFoxRequest, lRequestDOM, pMod);
    }

  } // createWebServiceWrapper

  /**
   * Builds a WebServiceRequestWrapper from a concrete factory based on a FoxRequest
   * and the valid DOM content of that request which has a namespace.
   * @param pFoxRequest the request containing an identified web service
   * @param pRequestDataDOM the parsed request body
   * @param pMod the endpoint module
   * @return a concrete WebServiceRequestWrapper implementation
   * @throws ExUserRequest invalid request
   */
  public abstract WebServiceRequestWrapper createWebServiceWrapper (FoxRequest pFoxRequest, DOM pRequestDataDOM, Mod pMod)
  throws ExUserRequest;

} // WebServiceRequestWrapperFactory
