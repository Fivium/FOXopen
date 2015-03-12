package net.foxopen.fox.entrypoint.servlets;

import net.foxopen.fox.App;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxRequestHttp;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.filter.RequestLogFilter;
import net.foxopen.fox.ex.ExAlreadyHandled;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.RequestContextImpl;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackLogger;
import net.foxopen.fox.track.TrackUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Extension of the regular HttpServlet class to provide blocking while unconfigured and general stats on request counts.
 * Any entry point in to FOX aside from the initial boot servlet, should extend this class.
 */
public abstract class EntryPointServlet
extends HttpServlet {

  public static final String REQUEST_ATTRIBUTE_APP_MNEM = "net.foxopen.fox.entrypoint.servlet.EntryPointServlet.AppMnem";

  private void checkInitialised() throws ExInternal {
    if (!FoxGlobals.getInstance().isEngineInitialised()) {
      if (FoxBootServlet.getLastBootError() != null) {
        throw new ExInternal("This FOX instance has not yet been successfully initialised, last error:", FoxBootServlet.getLastBootError());
      }
      else {
        throw new ExInternal("This FOX instance has not yet been successfully initialised");
      }
    }
  }

  private String establishAppMnemInternal(HttpServletRequest pRequest) {

    String lAppMnem = pRequest.getParameter("app_mnem");
    if(XFUtil.isNull(lAppMnem)) {
      lAppMnem = establishAppMnem(pRequest);
    }

    return XFUtil.nvl(lAppMnem, FoxGlobals.getInstance().getFoxEnvironment().getDefaultAppMnem());
  }

  protected String establishAppMnem(HttpServletRequest pRequest) {
    return null;
  }

  /**
   * Gets the connection pool to construct the request's ContextUCon with. If null, the request app mnem's pool will be used.
   * @param pFoxRequest In case URI needs to be examined.
   * @return A valid pool name or null.
   */
  protected String getConnectionPoolName(FoxRequest pFoxRequest){
    return null;
  }

  public abstract FoxSession establishFoxSession(RequestContext pRequestContext);

  protected String getContextUConPurpose() {
    return "Request";
  }

  protected abstract String getContextUConInitialConnectionName() ;

  protected abstract String getTrackElementName(RequestContext pRequestContext);

  protected TrackLogger getTrackLogger(RequestContext pRequestContext) {
    return TrackUtils.createDefaultTrackLogger(pRequestContext);
  }

  private interface RequestProcessor {
    void processRequest(RequestContext pRequestContext);
  }

  private void doRequest(HttpServletRequest pRequest, HttpServletResponse pResponse, RequestProcessor pRequestProcessor) {

    checkInitialised();

    String lAppMnem = establishAppMnemInternal(pRequest);
    App lApp;
    try {
      //Note: this validates the app mnem exists
      lApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(lAppMnem);
    }
    catch (ExServiceUnavailable | ExApp e) {
      //TODO this should return 404 or similar
      throw new ExInternal("App not found", e);
    }
    pRequest.setAttribute(REQUEST_ATTRIBUTE_APP_MNEM, lAppMnem);

    //Work out if secure cookies should be sent
    boolean lUseSecureCookies = "https".equals(pRequest.getScheme().toLowerCase()) && lApp.isSecureCookies();

    //Construct new FoxRequest
    FoxRequest lFoxRequest = new FoxRequestHttp(pRequest, pResponse, (String) pRequest.getAttribute(RequestLogFilter.REQUEST_ATTRIBUTE_LOG_ID), lUseSecureCookies);

    //Creates a new RequestContext and pushes a new connection onto the ContextUCon
    String lConnectionPoolName = XFUtil.nvl(getConnectionPoolName(lFoxRequest), lApp.getConnectionPoolName());
    RequestContext lRequestContext = RequestContextImpl.createForServlet(this, lFoxRequest, lApp, lConnectionPoolName, getContextUConPurpose(), getContextUConInitialConnectionName());

    Track.startTracking(getTrackLogger(lRequestContext));
    Track.open(getTrackElementName(lRequestContext));
    try {
      try {
        pRequestProcessor.processRequest(lRequestContext);
      }
      finally {
        //Ensure all ContextUCon connections are closed and returned
        try {
          lRequestContext.getContextUCon().rollbackAndCloseAll(true);
        }
        catch (Throwable th) {
          //Don't allow errors from UCon closing to propogate
          Track.recordSuppressedException("EntryPointRollbackAndCloseAll", th);
        }
      }
    }
    catch (Throwable th) {
      //Record the error on track so it can be serialised inline and seen by the error servlet
      Track.setRootException(th);

      //Only rethrow if not already handled (streaming responses will include a JS-based redirect)
      if (!wasExceptionAlreadyHandled(th)) {
        pRequest.setAttribute(RequestLogFilter.REQUEST_ATTRIBUTE_TRACK_ID, Track.currentTrackId());
        throw th;
      }
    }
    finally {
      Track.close();
      Track.stopTracking();
    }
  }

  /**
   * Tests if at least 1 exception in the given error stack indicates that the exception has already been handled.
   * @param pException Root exception.
   * @return True if the exception has already been handled and should not be rethrown.
   */
  private static boolean wasExceptionAlreadyHandled(Throwable pException) {
    if (pException instanceof ExAlreadyHandled) {
      return true;
    }
    else if (pException.getCause() != null) {
      return wasExceptionAlreadyHandled(pException.getCause());
    }
    return false;
  }

  @Override
  public final void doGet(HttpServletRequest pRequest, HttpServletResponse pResponse) {
    doRequest(pRequest, pResponse, new RequestProcessor() {
      @Override
      public void processRequest(RequestContext pRequestContext) {
        processGet(pRequestContext);
      }
    });
  }

  @Override
  public final void doPost(HttpServletRequest pRequest, HttpServletResponse pResponse) {
    doRequest(pRequest, pResponse, new RequestProcessor() {
      @Override
      public void processRequest(RequestContext pRequestContext) {
        processPost(pRequestContext);
      }
    });
  }

  @Override
  public final void doPut(HttpServletRequest pRequest, HttpServletResponse pResponse) {
    doRequest(pRequest, pResponse, new RequestProcessor() {
      @Override
      public void processRequest(RequestContext pRequestContext) {
        processPut(pRequestContext);
      }
    });
  }

  @Override
  public final void doDelete(HttpServletRequest pRequest, HttpServletResponse pResponse) {
    doRequest(pRequest, pResponse, new RequestProcessor() {
      @Override
      public void processRequest(RequestContext pRequestContext) {
        processDelete(pRequestContext);
      }
    });
  }

  @Override
  public final void doTrace(HttpServletRequest pRequest, HttpServletResponse pResponse) {
    doRequest(pRequest, pResponse, new RequestProcessor() {
      @Override
      public void processRequest(RequestContext pRequestContext) {
        processTrace(pRequestContext);
      }
    });
  }

  @Override
  public final void doHead(HttpServletRequest pRequest, HttpServletResponse pResponse) {
    doRequest(pRequest, pResponse, new RequestProcessor() {
      @Override
      public void processRequest(RequestContext pRequestContext) {
        processHead(pRequestContext);
      }
    });
  }

  @Override
  public final void doOptions(HttpServletRequest pRequest, HttpServletResponse pResponse) {
    doRequest(pRequest, pResponse, new RequestProcessor() {
      @Override
      public void processRequest(RequestContext pRequestContext) {
        processOptions(pRequestContext);
      }
    });
  }

  @Override
  public final void init(ServletConfig pServletConfig) throws ServletException {
    super.init(pServletConfig);
    processInit(pServletConfig);
  }

  @Override
  public final void destroy() {
    processDestroy();
  }

  /**
   * Stubbed empty method, override these in extending class to implement the functionality you need.
   *
   * @param pRequest
   * @param pResponse
   */
  public void processGet(RequestContext pRequestContext){
  }

  /**
   * Stubbed empty method, override these in extending class to implement the functionality you need.
   *
   * @param pRequest
   * @param pResponse
   */
  public void processPost(RequestContext pRequestContext){
  }

  /**
   * Stubbed empty method, override these in extending class to implement the functionality you need.
   *
   * @param pRequest
   * @param pResponse
   */
  public void processPut(RequestContext pRequestContext){
  }

  /**
   * Stubbed empty method, override these in extending class to implement the functionality you need.
   *
   * @param pRequest
   * @param pResponse
   */
  public void processDelete(RequestContext pRequestContext){
  }

  /**
   * Stubbed empty method, override these in extending class to implement the functionality you need.
   *
   * @param pRequest
   * @param pResponse
   */
  public void processTrace(RequestContext pRequestContext){
  }

  /**
   * Stubbed empty method, override these in extending class to implement the functionality you need.
   *
   * @param pRequest
   * @param pResponse
   */
  public void processHead(RequestContext pRequestContext){
  }

  /**
   * Stubbed empty method, override these in extending class to implement the functionality you need.
   *
   * @param pRequest
   * @param pResponse
   */
  public void processOptions(RequestContext pRequestContext){
  }

  /**
   * Stubbed empty method, override these in extending class to implement the functionality you need.
   *
   * @param pServletConfig
   */
  public void processInit(ServletConfig pServletConfig) throws ServletException {
  }

  /**
   * Stubbed empty method, override these in extending class to implement the functionality you need.
   */
  public void processDestroy(){
  }

}
