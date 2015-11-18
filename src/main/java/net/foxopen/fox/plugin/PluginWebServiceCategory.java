package net.foxopen.fox.plugin;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.entrypoint.ws.EndPoint;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.entrypoint.ws.WebService;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthDescriptor;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthType;
import net.foxopen.fox.entrypoint.ws.WebServiceCategory;
import net.foxopen.fox.entrypoint.ws.WebServiceResponse;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.plugin.api.PluginManagerContext;
import net.foxopen.fox.plugin.api.WebServiceProvidingPlugin;
import net.foxopen.fox.plugin.api.ws.FxpWebServiceEndPoint;
import net.foxopen.fox.plugin.api.ws.FxpWebServiceRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * WebServiceCategory, available at /plugin, which delegates all plugin WebService requests to their providing plugin instance.
 * If the plugin is not currently available, its WebService is not returned. A single plugin can provide at most one WebService.
 */
public class PluginWebServiceCategory
implements WebServiceCategory {

  public static final String CATEGORY_NAME = "plugin";

  private final PluginManager mParentManager;

  PluginWebServiceCategory(PluginManager pParentManager) {
    mParentManager = pParentManager;
  }

  @Override
  public String getName() {
    return CATEGORY_NAME;
  }

  @Override
  public Collection<? extends WebService> getAllWebServices() {

    Collection<WebService> lResult = new HashSet<>();

    //Get a current list of all the WebService providers from the manager
    for(WebServiceProvidingPlugin lPlugin : mParentManager.getAllWebServiceProviders()) {
      lResult.add(new WebServiceWrapper(lPlugin));
    }

    return lResult;
  }

  /**
   * Wrapper to convert an FxpWebService to a WebService.
   */
  private static final class WebServiceWrapper implements WebService {

    private final WebServiceProvidingPlugin mPlugin;
    private final Collection<EndPoint> mWrappedEndPoints;

    public WebServiceWrapper(WebServiceProvidingPlugin pPlugin) {
      mPlugin = pPlugin;

      //Wrap the end points once on construction to avoid repeated effort
      mWrappedEndPoints = new HashSet<>();
      for(FxpWebServiceEndPoint lEndPoint : mPlugin.getAllEndPoints()) {
        mWrappedEndPoints.add(new EndPointWrapper(mPlugin.getName(), lEndPoint));
      }
    }

    @Override
    public String getName() {
      return mPlugin.getWebServiceName();
    }

    @Override
    public WebServiceAuthDescriptor getAuthDescriptor() {

      switch (mPlugin.getAuthType()) {
        case TOKEN:
          return new WebServiceAuthDescriptor(false, WebServiceAuthType.TOKEN);
        case INTERNAL:
          return new WebServiceAuthDescriptor(false, InternalAuthLevel.INTERNAL_ADMIN, WebServiceAuthType.INTERNAL);
        case NONE:
          return WebServiceAuthDescriptor.NO_AUTHENTICATION_REQUIRED;
        default:
          throw new ExInternal("Unknown plugin auth type " + mPlugin.getAuthType());
      }
    }

    @Override
    public String getRequiredConnectionPoolName(FoxRequest pFoxRequest) {
      return mPlugin.getConnectKey();
    }

    @Override
    public Collection<? extends EndPoint> getAllEndPoints() {
     return mWrappedEndPoints;
    }
  }

  /**
   * Wrapper to convert an FxpEndPoint to an EndPoint.
   */
  private static final class EndPointWrapper
  implements EndPoint {

    private final String mPluginName;
    private final FxpWebServiceEndPoint mEndPoint;
    private final PluginManagerContext mPluginManagerContext;

    public EndPointWrapper(String pPluginName, FxpWebServiceEndPoint pEndPoint) {
      mPluginName = pPluginName;
      mEndPoint = pEndPoint;
      mPluginManagerContext = PluginManager.instance().getLoadedPluginManagerContext(pPluginName);
    }

    @Override
    public String getName() {
      return mEndPoint.getName();
    }

    @Override
    public PathParamTemplate getPathParamTemplate() {
      String lParamTemplate = mEndPoint.getPathParamTemplate();
      if(!XFUtil.isNull(lParamTemplate)) {
        return new PathParamTemplate(lParamTemplate);
      }
      else {
        return null;
      }
    }

    @Override
    public Collection<String> getMandatoryRequestParamNames() {
      return mEndPoint.getMandatoryRequestParamNames();
    }

    @Override
    public Collection<String> getAllowedHttpMethods() {
      return mEndPoint.getAllowedHttpMethods();
    }

    @Override
    public WebServiceResponse respond(RequestContext pRequestContext, Map<String, String> pParamMap, String pHttpMethod, WebServiceResponse.Type pDesiredResponseType) {
      Track.pushInfo("WebServiceDelegateToPlugin", mPluginName);
      try {
        FxpWebServiceRequestContext lWSRC = new PluginWebServiceRequestContextWrapper(pRequestContext, mPluginManagerContext);
        //This cast is safe - we know we've wrapped a WebServiceResponse, but we don't want the class name visible to the plugin API
        return (WebServiceResponse) mEndPoint.respond(lWSRC, pParamMap, pHttpMethod, pDesiredResponseType.getPluginAPIType()).getWrappedResponse();
      }
      finally {
        Track.pop("WebServiceDelegateToPlugin");
      }
    }
  }
}
