package net.foxopen.fox.entrypoint.ws;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.plugin.api.ws.FxpWebServiceResponse;

/**
 * Wrapper for a response object from a WebService request. WebService EndPoints should use the {@link GenericWebServiceResponse} type
 * if they are returning response type agonstic data. The WebService servlet converts the generic map to the correct type
 * and adds extra API data. Consumers requiring more control should return a more specific response type (XML or JSON).
 * This is not manipulated at all by the servlet and is passed directly back to the client. If an EndPoint returns a
 * WebServiceResponse which is not compatible with the response type requested by the client, an error will be raised.
 */
public abstract class WebServiceResponse {

  /**
   * Response types a WebService client can request.
   */
  public static enum Type {
    JSON,
    XML;

    public static Type fromParamString(String pParamString) {
      try {
        return valueOf(pParamString.toUpperCase());
      }
      catch (IllegalArgumentException e) {
        throw new ExInternal("Response Type " + pParamString + " not recognised");
      }
    }

    /**
     * Gets a response type which is compatible with the Plugin API.
     * @return
     */
    public FxpWebServiceResponse.Type getPluginAPIType() {
      return FxpWebServiceResponse.Type.valueOf(toString());
    }
  }

  protected static final String JSON_CONTENT_TYPE = "application/json";
  protected static final String XML_CONTENT_TYPE = "text/xml";

  /**
   * Returns true if this response can create a FoxResponse conforming to the given type.
   * @param pType
   * @return
   */
  abstract boolean isTypeSupported(Type pType);

  /**
   * Creates a new FoxResponse containing the response object, serialised to the correct format. This is only invoked if
   * {@link #isTypeSupported}returns true for the required type.
   * @param pFoxRequest Request to respond to.
   * @param pType Return type required.
   * @return A new FoxResponse ready to be sent.
   */
  abstract FoxResponse generateResponse(FoxRequest pFoxRequest, Type pType);

}
