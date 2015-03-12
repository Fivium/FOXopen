package net.foxopen.fox.plugin.api.ws;

import net.foxopen.fox.plugin.api.PluginManagerContext;
import net.foxopen.fox.plugin.api.database.FxpContextUCon;

import javax.servlet.http.HttpServletRequest;

/**
 * RequestContext sent to plugin WebService EndPoints.
 */
public interface FxpWebServiceRequestContext {

  /**
   * Gets the original servlet request. This should be used by plugins which need to implement bespoke request handling.
   * @return
   */
  public HttpServletRequest getRawRequest();

  /**
   * Gets the current ContextUCon for this request. This will have been set up in the WebService servlet with the connection
   * specified by the EndPoint's owning WebService.
   * @return
   */
  public FxpContextUCon getContextUCon();

  /**
   * Gets the plugin context manager for this web service plugin which contains the configuration information.
   * @return
   */
  public PluginManagerContext getPluginManagerContext();

}
