package net.foxopen.fox.banghandler;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxRequestHttp;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.enginestatus.StatusBangHandler;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.VersionBangHandler;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.logging.BoomerangBangHandler;
import net.foxopen.fox.plugin.PluginBangHandler;
import net.foxopen.fox.thread.devtoolbar.DebugPageBangHandler;
import net.foxopen.fox.thread.devtoolbar.DevToolbarOptionsBangHandler;
import net.foxopen.fox.thread.devtoolbar.ViewDOMBangHandler;
import net.foxopen.fox.thread.devtoolbar.XPathBangHandler;
import net.foxopen.fox.track.LatestTrackBangHandler;
import net.foxopen.fox.track.ShowTrackBangHandler;
import net.foxopen.fox.track.TrackSummaryBangHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BangHandlerServlet
extends HttpServlet {

  private static final Collection<? extends BangHandler> KNOWN_HANDLERS = Arrays.asList(
    BoomerangBangHandler.instance(),
    FlushCacheBangHandler.instance(),
    DebugPageBangHandler.instance(),
    DevToolbarOptionsBangHandler.instance(),
    FlushBangHandler.instance(),
    FlushInternalBangHandler.instance(),
    LatestTrackBangHandler.instance(),
    LoginBangHandler.instance(),
    PluginBangHandler.instance(),
    ShowTrackBangHandler.instance(),
    StatusBangHandler.instance(),
    TrackSummaryBangHandler.instance(),
    VersionBangHandler.instance(),
    ViewDOMBangHandler.instance(),
    XPathBangHandler.instance()
  );

  private static final Map<String, BangHandler> gHandlerMap = new HashMap<>();
  static {
    for(BangHandler lBangHandler : KNOWN_HANDLERS) {
      if(!gHandlerMap.containsKey(lBangHandler.getAlias())) {
        gHandlerMap.put(lBangHandler.getAlias(), lBangHandler);
      }
      else {
        //Catch any silly dev time errors
        throw new ExInternal("Bang handler alias " + lBangHandler.getAlias() + " already registered");
      }
    }
  }

  public static String getServletPath() {
    return "handle";
  }

  public static FoxResponse basicHtmlResponse(String pMessage) {
    return new FoxResponseCHAR("text/html", new StringBuffer("<HTML><BODY><PRE>"+pMessage+"</PRE></BODY></HTML>"), 0);
  }

  @Override
  protected void doPost(HttpServletRequest pHttpServletRequest, HttpServletResponse pHttpServletResponse)
  throws ServletException, IOException {
    respond(pHttpServletRequest, pHttpServletResponse);
  }

  @Override
  protected void doGet(HttpServletRequest pHttpServletRequest, HttpServletResponse pHttpServletResponse)
  throws ServletException, IOException {
    respond(pHttpServletRequest, pHttpServletResponse);
  }

  private void respond(final HttpServletRequest pHttpServletRequest, HttpServletResponse pHttpServletResponse)
  throws ServletException, IOException {

    //RequestContextImpl lFromHttpRequest = RequestContextImpl.createFromHttpRequest(req, resp,);
    FoxRequest lFoxRequest = new FoxRequestHttp(pHttpServletRequest, pHttpServletResponse);

    //TODO disallow access if not configured (except !LOGIN)

    String lBangCommand = XFUtil.pathPopTail(lFoxRequest.getRequestURIStringBuilder());
    if(XFUtil.isNull(lBangCommand) || lBangCommand.charAt(0) != '!') {
      throw new ExInternal("Invalid request URI syntax");
    }
    else {
      lBangCommand = lBangCommand.substring(1);

      BangHandler lBangHandler = gHandlerMap.get(lBangCommand);
      if(lBangHandler == null) {
        throw new ExInternal("Command !" + lBangCommand + " not known");
      }
      else {

        boolean lAccessAllowed;
        if(lBangHandler.isDevAccessAllowed() && FoxGlobals.getInstance().isDevelopment()) {
          lAccessAllowed = true;
        }
        else {
          lAccessAllowed = InternalAuthentication.instance().authenticate(lFoxRequest, lBangHandler.getRequiredAuthLevel());
        }

        //If access was not allowed, the authenticator should have already responded with a 401 challenge
        if(lAccessAllowed) {

          //Validate params
          for(String lParam : lBangHandler.getParamList()) {
            if(XFUtil.isNull(lFoxRequest.getParameter(lParam))) {
              //TODO improve error handling/reporting
              throw new ExInternal("Handler " + lBangHandler.getAlias() + " requires parameter " + lParam + " to be specified on request");
            }
          }

          FoxResponse lFoxResponse = lBangHandler.respond(lFoxRequest);
          lFoxResponse.respond(lFoxRequest);
        }
      }
    }
  }
}
