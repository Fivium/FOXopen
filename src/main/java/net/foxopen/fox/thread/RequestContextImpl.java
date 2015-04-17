package net.foxopen.fox.thread;

import net.foxopen.fox.App;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.entrypoint.servlets.EntryPointServlet;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilderImpl;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;


public class RequestContextImpl
implements RequestContext {

  private final FoxRequest mFoxRequest;
  private final ContextUCon mContextUCon;
  private final String mRequestAppMnem;
  private FoxSession mFoxSession;

  private static App getApp(String pAppMnem) {
    App lApp;
    try {
      lApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(pAppMnem, true);
    }
    catch (ExServiceUnavailable | ExApp e) {
      throw new ExInternal("Failed to retrieve app", e);
    }
    return lApp;
  }

  public static RequestContext createForServlet(EntryPointServlet pServlet, FoxRequest pFoxRequest, App pApp, String pConnectionPoolName, String pContextUConPurpose, String pInitialConnectionName) {
    ContextUCon lContextUCon = ContextUCon.createContextUCon(pConnectionPoolName, pContextUConPurpose);
    lContextUCon.pushConnection(pInitialConnectionName);
    return new RequestContextImpl(pServlet, pFoxRequest, lContextUCon, pApp.getMnemonicName());
  }

  public static RequestContextImpl createFromFoxRequest(FoxRequest pFoxRequest, App pApp, String pContextUConPurpose, String pInitialConnectionName, FoxSession pFoxSession)  {
    ContextUCon lContextUCon = ContextUCon.createContextUCon(pApp.getConnectionPoolName(), pContextUConPurpose);
    lContextUCon.pushConnection(pInitialConnectionName);
    return new RequestContextImpl(pFoxRequest, lContextUCon, pApp.getMnemonicName(), pFoxSession);
  }

  private RequestContextImpl(EntryPointServlet pServlet, FoxRequest pFoxRequest, ContextUCon pContextUCon, String pAppMnem)  {
    mFoxRequest = pFoxRequest;
    mContextUCon = pContextUCon;
    mRequestAppMnem = pAppMnem;
    mFoxSession = pServlet.establishFoxSession(this);
  }

  private RequestContextImpl(FoxRequest pFoxRequest, ContextUCon pContextUCon, String pAppMnem, FoxSession pFoxSession)  {
    mFoxRequest = pFoxRequest;
    mContextUCon = pContextUCon;
    mRequestAppMnem = pAppMnem;
    mFoxSession = pFoxSession;
  }

  protected RequestContextImpl(RequestContext pRequestContext)  {
    mFoxRequest = pRequestContext.getFoxRequest();
    mContextUCon = pRequestContext.getContextUCon();
    mRequestAppMnem = pRequestContext.getRequestAppMnem();
    mFoxSession = pRequestContext.getFoxSession();
  }

  public FoxRequest getFoxRequest() {
    return mFoxRequest;
  }

  public ContextUCon getContextUCon() {
    return mContextUCon;
  }

  @Override
  public String getRequestAppMnem() {
    return !XFUtil.isNull(mRequestAppMnem) ? mRequestAppMnem : FoxGlobals.getInstance().getFoxEnvironment().getDefaultAppMnem();
  }

  @Override
  public App getRequestApp() {
    return getApp(getRequestAppMnem());
  }

  @Override
  public FoxSession getFoxSession() {
    return mFoxSession;
  }

  @Override
  public void forceNewFoxSession(FoxSession pNewSession) {
    mFoxSession = pNewSession;
  }

  @Override
  public SecurityScope getCurrentSecurityScope() {
    return SecurityScope.defaultInstance();
  }

  @Override
  public RequestURIBuilder createURIBuilder() {
    return RequestURIBuilderImpl.createFromRequestContext(this, true);
  }
}
