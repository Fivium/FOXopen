package net.foxopen.fox.plugin;

import net.foxopen.fox.plugin.api.PluginManagerContext;
import net.foxopen.fox.plugin.api.database.FxpContextUCon;
import net.foxopen.fox.plugin.api.ws.FxpWebServiceRequestContext;
import net.foxopen.fox.thread.RequestContext;

import javax.servlet.http.HttpServletRequest;

/**
 * Wrapper for a normal RequestContext, exposed as an FxpWebServiceRequestContext.
 */
public class PluginWebServiceRequestContextWrapper
implements FxpWebServiceRequestContext {

  private final RequestContext mRequestContext;
  private final PluginManagerContext mPluginManagerContext;

  PluginWebServiceRequestContextWrapper(RequestContext pRequestContext, PluginManagerContext pPluginManagerContext) {
    mRequestContext = pRequestContext;
    mPluginManagerContext = pPluginManagerContext;
  }

  @Override
  public HttpServletRequest getRawRequest() {
    return mRequestContext.getFoxRequest().getHttpRequest();
  }

  @Override
  public FxpContextUCon getContextUCon() {
    return mRequestContext.getContextUCon();
  }


  @Override
  public PluginManagerContext getPluginManagerContext() {
    return mPluginManagerContext;
  }
}
