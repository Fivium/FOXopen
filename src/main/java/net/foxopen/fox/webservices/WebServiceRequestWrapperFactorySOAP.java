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
