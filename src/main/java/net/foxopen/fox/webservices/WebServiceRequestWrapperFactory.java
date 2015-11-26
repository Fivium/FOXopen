package net.foxopen.fox.webservices;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.Mod;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.HashMap;


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

    //TODO PN this has been modified from original implementation and not tested
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
