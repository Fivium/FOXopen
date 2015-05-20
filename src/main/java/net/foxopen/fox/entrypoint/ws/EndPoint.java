package net.foxopen.fox.entrypoint.ws;

import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.UnauthenticatedFoxSession;
import net.foxopen.fox.thread.RequestContext;

import java.util.Collection;
import java.util.Map;

/**
 * An EndPoint is provided by a WebService and is used to process WebService requests.
 */
public interface EndPoint {

  /**
   * Gets the name of the EndPoint as it will appear in a URI.
   * @return EndPoint name.
   */
  public String getName();

  /**
   * Creates a FoxSession implementation which the EndPoint requires for authentication. By default EndPoints return
   * an {@link net.foxopen.fox.entrypoint.UnauthenticatedFoxSession} as typically no auth is required.
   *
   * @param pRequestContext Current RequestContext.
   * @return A FoxSession object
   */
  default FoxSession establishFoxSession(RequestContext pRequestContext) {
    return UnauthenticatedFoxSession.create();
  }

  /**
   * Gets the optional PathParamTemplate for this EndPoint, which defines parameters a client must specify in the URI
   * path when the EndPoint is invoked. This can be null if the EndPoint has no URI parameters.
   * @return PathParamTemplate or null.
   */
  public PathParamTemplate getPathParamTemplate();

  /**
   * Gets a set of 0 or more parameter names which this EndPoint requires as HTTP request parameters (i.e. GET or POST
   * parameters). The incoming request is validated to ensure it contains all these parameters and is rejected if not.
   * Additional parameters are accepted even if they are not listed in this set.
   * @return Mandatory request parameter names, or an empty set. Do not return null.
   */
  public Collection<String> getMandatoryRequestParamNames();

  /**
   * Gets a set of HTTP method names which are allowed to be used to access this endpoint (e.g. GET, PUT, POST). Returns
   * an empty set if all HTTP methods are allowed.
   * @return Set of allowed HTTP methods, or an empty set. Do not return null.
   */
  public Collection<String> getAllowedHttpMethods();

  /**
   * Generates a response to an incoming web service request. This will only be invoked if the incoming request conforms
   * to the requirements defined by this EndPoint.
   * @param pRequestContext Current RequestContext.
   * @param pParamMap Parameters from both the URI path and the HTTP request (POST/GET params), if provided.
   * @param pHttpMethod The HTTP method (GET, PUT, POST, etc) which was used to invoke the request.
   * @param pDesiredResponseType The response type the client has requested. The EndPoint is free to ignore this if it only
   * supports a certain response type, but it may use the value to tailor a more appropriate response if it can.
   * @return Response object which will be sent back to the client.
   */
  public WebServiceResponse respond(RequestContext pRequestContext, Map<String, String> pParamMap, String pHttpMethod, WebServiceResponse.Type pDesiredResponseType);

}
