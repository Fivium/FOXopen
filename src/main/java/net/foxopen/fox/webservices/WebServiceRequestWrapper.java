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
