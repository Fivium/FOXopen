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

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExUserRequest;


/**
 * Builds a WebServiceRequestWrapperSOAP from a FoxRequest.
 */
public class WebServiceRequestWrapperFactorySOAP extends WebServiceRequestWrapperFactory 
{
  /**
   * Builds a WebServiceRequestWrapper from a SOAP implementation factory based 
   * on a FoxRequest and the valid DOM content of that request which has a 
   * namespace.
   * @param pFoxRequest the request containing an identified web service
   * @param pRequestDataDOM the parsed request body 
   * @param pMod the endpoint module
   * @return a WebServiceRequestWrapperSOAP instance
   * @throws ExUserRequest invalid request
   */
  public WebServiceRequestWrapper createWebServiceWrapper (FoxRequest pFoxRequest, DOM pRequestDataDOM, Mod pMod)
  throws ExUserRequest {
    return new WebServiceRequestWrapperSOAP(pFoxRequest, pRequestDataDOM, pMod);
  } // createWebServiceWrapper
  
} // WebServiceRequestWrapperFactorySOAP
