package net.foxopen.fox.plugin.api;

import net.foxopen.fox.plugin.api.ws.FxpWebServiceAuthType;
import net.foxopen.fox.plugin.api.ws.FxpWebServiceEndPoint;

import java.util.Collection;

/**
 * Plugins implementing this interface are able to respond to web service requests.
 */
public interface WebServiceProvidingPlugin
extends FoxPlugin {

  /**
   * Gets the name of the web service, as it will appear in a URI.
   * @return
   */
  public String getWebServiceName();

  /**
   * Gets the auth type which all endpoints of this web service require by default.
   * @return
   */
  public FxpWebServiceAuthType getAuthType();

  /**
   * Gets the connect key which WebService requests to this plugin should use. Leave null to use the engine default.
   * @return String connect key or null.
   */
  public String getConnectKey();

  /**
   * Gets all the EndPoints which this plugin provides.
   * @return 1 or more EndPoints.
   */
  public Collection<? extends FxpWebServiceEndPoint> getAllEndPoints();

}
