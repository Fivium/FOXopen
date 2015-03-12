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


import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExUserRequest;

/**
 * Provides simple API access to the web service request data found in
 * a FoxRequest.
 */
public interface WebServiceRequestWrapper 
{
  /**
   * Extract the operation that the request is attempting to call.
   * @return name of operation (should map to a service-based entry theme).
   * @throws ExUserRequest
   * @throws ExActionFailed
   */
  public String getOperationName ()
  throws ExUserRequest, ExActionFailed;
  
  /**
   * Extract the FOX thread ref (if present on the request).
   * @return thread id as a String
   */
  public String getFoxThreadRef()
  throws ExUserRequest, ExActionFailed;

  /**
   * Extract the FOX XfsessionId (if present on the request).
   * @return thread id as a String
   */
  public String getXfsessionId()
  throws ExUserRequest, ExActionFailed;

  /**
   * Extract the request payload (the parameters to the operation).
   * @return xml request body
   * @throws ExUserRequest
   * @throws ExActionFailed
   */
  public DOMList getRequestData ()
  throws ExUserRequest, ExActionFailed;
  
  /**
   * Extract the request header.
   * @return xml request header
   * @throws ExUserRequest
   * @throws ExActionFailed
   */
  public DOMList getRequestHeader()
  throws ExUserRequest, ExActionFailed;

  /**
   * Creates a response based on a response data DOM.
   * @param pResponseDataDOM data to wrap in a web service response
   * @return the prepared response
   */
  public FoxResponse createResponse (DOM pResponseDataDOM);
  
  /**
   * Creates a response based on a response data DOMList.
   * @param pResponseDataDOMList data to wrap in a web service response
   * @return the prepared response
   */
  public FoxResponse createResponse (DOMList pResponseDataDOMList);
  
  /**
   * Creates a response based on a response data DOMList.
   * @param pResponseDataDOMList data to wrap in a web service response
   * @param pResponseHeaderDOMList headers to wrap in a web service response
   * @return the prepared response
   */
  public FoxResponse createResponse (DOMList pResponseDataDOMList, DOMList pResponseHeaderDOMList);
  
  /**
   * Creates a response based on a response data DOMList.
   * @param pResponseDataDOMList data to wrap in a web service response
   * @param pResponseHeaderDOMList headers to wrap in a web service response
   * @param pOptionalFoxThreadRef optional thread ref to append to outgoing headers
   * @param pOptionalXfsessionId optional xfsessionid to append to outgoing headers
   * @return the prepared response
   */
  public FoxResponse createResponse (DOMList pResponseDataDOMList, DOMList pResponseHeaderDOMList, String pOptionalFoxThreadRef, String pOptionalXfsessionId);

  /**
   * Creates a response based on an thrown exception.
   * @param pThrowable the exception to wrap
   * @return the prepared response
   */
  public FoxResponse createResponse (Throwable pThrowable);
  
  /**
   * Creates a response based on an thrown exception.
   * @param pThrowable the exception to wrap
   * @param pOptionalFoxThreadRef optional thread ref to append to outgoing headers
   * @param pOptionalXfsessionId optional xfsessionid to append to outgoing headers
   * @return the prepared response
   */
  public FoxResponse createResponse (Throwable pThrowable, String pOptionalFoxThreadRef, String pOptionalXfsessionId);  
  
} // WebServiceRequestWrapper
