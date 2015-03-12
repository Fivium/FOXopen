package net.foxopen.fox.plugin.api.ws;

import java.util.Collection;
import java.util.Map;

/**
 * A WebService EndPoint provided by a plugin.
 */
public interface FxpWebServiceEndPoint {

  /**
   * Gets the name of the EndPoint as it will appear in a URI.
   * @return
   */
  public String getName();

  /**
   * Gets the URI path parameter string template for this EndPoint. Params in {braces} are parsed out and passed to the
   * {@link #respond} method in the parameter map. For instance given the template string:<br><br>
   *
   * <code>/document/{doc_id}/format/{doc_format}</code><br><br>
   *
   * When invoked as the following URI:<br><br>
   *
   * <code>/document/100/format/pdf</code><br><br>
   *
   * Will result in a parameter map like this:
   *
   * <pre>
   * doc_id => 100
   * doc_format => pdf
   * </pre>
   * Requests to this endpoint must match this pattern. Return null if no parameters are required in the URI path.
   * @return Path param template string, or null.
   */
  public String getPathParamTemplate();

  /**
   * Gets a set of 0 or more parameter names which this endpoint requires as HTTP request parameters (i.e. GET or POST
   * parameters). The incoming request is validated to ensure it contains all these parameters and is rejected if not.
   * Additional parameters are accepted if they are not listed in this set.
   * @return Mandatory request parameter names.
   */
  public Collection<String> getMandatoryRequestParamNames();

  /**
   * Gets a set of HTTP method names which are allowed to be used to access this endpoint (e.g. GET, PUT, POST). Returns
   * an empty set if all HTTP methods are allowed.
   * @return Set of allowed HTTP methods, or an empty set.
   */
  public Collection<String> getAllowedHttpMethods();

  /**
   * Generates a response to an incoming web service request. This will only be invoked if the incoming request conforms
   * to the requirements defined by this EndPoint.
   * @param pRequestContext Contains a ContextUCon if querying is required.
   * @param pParamMap Parameters from both the URI path and the HTTP request (POST/GET params), if provided.
   * @param pHttpMethod The HTTP method (GET, PUT, POST, etc) which was used to invoke the request.
   * @param pDesiredResponseType The response type the client has requested. The EndPoint is free to ignore this if it only
   * supports a certain response type, but it may use the value to tailor a more appropriate response if it can.
   * @return Response object which will be sent back to the client.
   */
  public FxpWebServiceResponse respond(FxpWebServiceRequestContext pRequestContext, Map<String, String> pParamMap, String pHttpMethod, FxpWebServiceResponse.Type pDesiredResponseType);

}
