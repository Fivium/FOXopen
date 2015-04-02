package net.foxopen.fox.entrypoint.servlets;

import net.foxopen.fox.App;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.auth.StandardAuthenticationContext;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.CookieBasedFoxSession;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.ParamsDOMUtils;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExSessionTimeout;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.RequestContextImpl;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.XThreadBuilder;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackProperty;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

// TODO - Guest access returns VALID result, needs better test to stop AuthRequired modules (should also enum that up)
public class FoxMainServlet
extends EntryPointServlet {

  public static final String SERVLET_PATH = "fox";

  public static final String APP_MNEM_PARAM_NAME = "app_mnem";
  private static final String MODULE_PARAM_NAME = "module_name";
  private static final String ENTRY_THEME_PARAM_NAME = "entry_theme";

  /** Entry GET request URIs should conform to one of these patterns */
  private static final PathParamTemplate THEME_ENTRY_PATH_TEMPLATE = new PathParamTemplate("/{" + APP_MNEM_PARAM_NAME + "}/{" + MODULE_PARAM_NAME + "}/{" + ENTRY_THEME_PARAM_NAME + "}");
  private static final PathParamTemplate MODULE_ENTRY_PATH_TEMPLATE = new PathParamTemplate("/{" + APP_MNEM_PARAM_NAME + "}/{" + MODULE_PARAM_NAME + "}");

  /** Form POST request URIs should conform to this pattern */
  private static final PathParamTemplate APP_MNEM_SUFFIX_PATH_TEMPLATE = new PathParamTemplate("/{" + APP_MNEM_PARAM_NAME + "}/");

  public static final String MAIN_CONNECTION_NAME = "MAIN";
  private static final String RESUME_PARAM_NAME = "resume";
  private static final String RESUME_PARAM_TRUE_VALUE = "1";
  private static final String THREAD_ID_PARAM_NAME = "thread_id";

  public static String buildGetEntryURI(RequestURIBuilder pRequestURIBuilder, String pAppMnem, String pModuleName) {
    return buildGetEntryURI(pRequestURIBuilder, pAppMnem, pModuleName, null);
  }

  public static String buildGetEntryURI(RequestURIBuilder pRequestURIBuilder, String pAppMnem, String pModuleName, String pEntryTheme) {
    pRequestURIBuilder.setParam(APP_MNEM_PARAM_NAME, pAppMnem);
    pRequestURIBuilder.setParam(MODULE_PARAM_NAME, pModuleName);
    pRequestURIBuilder.setParam(ENTRY_THEME_PARAM_NAME, pEntryTheme);
    return pRequestURIBuilder.buildServletURI(SERVLET_PATH, pEntryTheme != null ? THEME_ENTRY_PATH_TEMPLATE : MODULE_ENTRY_PATH_TEMPLATE);
  }

  public static String buildThreadResumeEntryURI(RequestURIBuilder pRequestURIBuilder, String pThreadId, String pAppMnem) {
    pRequestURIBuilder.setParam(THREAD_ID_PARAM_NAME, pThreadId);
    pRequestURIBuilder.setParam(APP_MNEM_PARAM_NAME, pAppMnem);
    pRequestURIBuilder.setParam(RESUME_PARAM_NAME, RESUME_PARAM_TRUE_VALUE);
    return pRequestURIBuilder.buildServletURI(SERVLET_PATH, APP_MNEM_SUFFIX_PATH_TEMPLATE);
  }

  public static String buildFormPostDestinationURI(RequestURIBuilder pRequestURIBuilder, String pAppMnem) {
    pRequestURIBuilder.setParam(APP_MNEM_PARAM_NAME, pAppMnem);
    return pRequestURIBuilder.buildServletURI(FoxMainServlet.SERVLET_PATH, APP_MNEM_SUFFIX_PATH_TEMPLATE);
  }

  public static String getAppMnemFromRequestPath(HttpServletRequest pRequest) {
    return XFUtil.pathPopHead(new StringBuilder(XFUtil.nvl(pRequest.getPathInfo())), true);
  }

  /**
   * Determines if the given request is being used to resume an existing thread.
   * @param pRequest Request to check.
   * @return True if the request is a "thread resume" request.
   */
  public static boolean isThreadResumeRequest(HttpServletRequest pRequest) {
    return "GET".equals(pRequest.getMethod()) && pRequest.getParameterMap().containsKey(THREAD_ID_PARAM_NAME);
  }

  @Override
  protected String establishAppMnem(HttpServletRequest pRequest) {
    return getAppMnemFromRequestPath(pRequest);
  }

  @Override
  protected String getTrackElementName(RequestContext pRequestContext) {
    return "FoxHttp" + XFUtil.initCap(pRequestContext.getFoxRequest().getHttpRequest().getMethod());
  }

  @Override
  public FoxSession establishFoxSession(RequestContext pRequestContext) {
    return CookieBasedFoxSession.getOrCreateFoxSession(pRequestContext);
  }

  @Override
  public void processGet(RequestContext pRequestContext) {
    processHttpRequest(pRequestContext);
  }

  @Override
  public void processPost(RequestContext pRequestContext) {
    processHttpRequest(pRequestContext);
  }

  private class CallThreadInfo {
    public String mThreadID = null;
    public StatefulXThread mXThread = null;
    public RequestContext mRequestContext = null;
    public FoxResponse mResponse = null;
  }

  @Override
  protected String getContextUConInitialConnectionName() {
    return MAIN_CONNECTION_NAME;
  }

  private void processHttpRequest(RequestContext pRequestContext) {
    CallThreadInfo lCurrentCallThreadInfo = new CallThreadInfo();

    lCurrentCallThreadInfo.mRequestContext = pRequestContext;
    FoxRequest lFoxRequest = pRequestContext.getFoxRequest();
    ContextUCon lContextUCon = lCurrentCallThreadInfo.mRequestContext.getContextUCon();

    lCurrentCallThreadInfo.mThreadID = lFoxRequest.getParameter(THREAD_ID_PARAM_NAME);

    try {
      String lClientInfo = "IP="+InetAddress.getLocalHost().getHostAddress()+", REMOTE-ADDR="+lFoxRequest.getHttpRequest().getRemoteAddr();
      String lAppInfo = "FOX-SYSTEM: " + XFUtil.nvl("TODO thread last module", "(direct entry)");

      if(lCurrentCallThreadInfo.mThreadID == null){
        lCurrentCallThreadInfo = createNewThread(lFoxRequest, lCurrentCallThreadInfo, lClientInfo, lAppInfo);
      }
      else {
        resumeThread(lFoxRequest, lCurrentCallThreadInfo, lClientInfo, lAppInfo);
      }

      //Validates all but MAIN transaction are committed
      lContextUCon.closeAllRetainedConnections();

      //Commit the MAIN connection - commits all work done by thread
      lContextUCon.commit(MAIN_CONNECTION_NAME);

      //Now release the thread lock - THIS ISSUES ANOTHER COMMIT
      if (lCurrentCallThreadInfo.mXThread != null) {
        StatefulXThread.unlockThread(lCurrentCallThreadInfo.mRequestContext, lCurrentCallThreadInfo.mXThread);
      }

      //This could throw an error but we're already committed everything - TODO what to do - need to report the error but likely to be a developer problem and not too serious
      lContextUCon.popConnection(MAIN_CONNECTION_NAME);
    }
    catch (Throwable th) {
      //On any error, rollback contents of all connections, including main connection and release back to pool
      lContextUCon.rollbackAndCloseAll(true);

      try {
        if(lCurrentCallThreadInfo.mXThread != null) {
          //Ensure we always have a decent connection to unlock the thread
          lContextUCon.pushConnection(MAIN_CONNECTION_NAME);
          StatefulXThread.unlockThread(lCurrentCallThreadInfo.mRequestContext, lCurrentCallThreadInfo.mXThread);
          lContextUCon.popConnection(MAIN_CONNECTION_NAME);
        }
      }
      catch (Throwable th2) {
        Track.recordSuppressedException("ErrorHandlerUnlockThread", th2);
        Track.alert("UnlockThread", "Error unlocking thread:" + th2.getMessage());
      }

      if(lCurrentCallThreadInfo.mThreadID != null) {
        //Only attempt to purge the thread if we got to the point of knowing its ID
        StatefulXThread.purgeThreadFromCache(lCurrentCallThreadInfo.mThreadID);
      }

      throw new ExInternal("Error processing request", th);
    }

    Track.pushInfo("SendResponse");
    try {
      lCurrentCallThreadInfo.mResponse.respond(lFoxRequest);
    }
    finally {
      Track.pop("SendResponse");
    }
  }

  private void resumeThread(FoxRequest pFoxRequest, CallThreadInfo pCurrentCallThreadInfo, String pClientInfo, String pAppInfo)
  throws ExServiceUnavailable, ExApp, ExUserRequest {

    HttpServletRequest lRequest = pFoxRequest.getHttpRequest();

    Track.setProperty(TrackProperty.THREAD_ID, pCurrentCallThreadInfo.mThreadID);

    //Get XThread from cache - NOTE THIS ISSUES A COMMIT ON CURRENT UCON
    //TODO - if thread has 0 state calls, interpret as a timeout and redirect to timeout screen
    // (so UX is improved when user exits, hits back and tries another action)
    pCurrentCallThreadInfo.mXThread = StatefulXThread.getAndLockXThread(pCurrentCallThreadInfo.mRequestContext, lRequest.getParameter("thread_id").trim());

    AuthenticationResult lAuthResult;
    Track.pushInfo("RequestAuthentication");
    try {
      AuthenticationContext lAuthContext = pCurrentCallThreadInfo.mXThread.getAuthenticationContext();
      lAuthResult = lAuthContext.verifySession(pCurrentCallThreadInfo.mRequestContext, pClientInfo, pAppInfo);
    }
    finally {
      Track.pop("RequestAuthentication");
    }

    if(lAuthResult.getCode() != AuthenticationResult.Code.VALID && lAuthResult.getCode() != AuthenticationResult.Code.GUEST){

      //TODO disallow guest access for auth required modules
      App lCurrentApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(pCurrentCallThreadInfo.mXThread.getThreadAppMnem(), true);

      Track.info("SessionInvalid", "Session is no longer valid (" + lAuthResult.getCode().toString() + "); handling timeout");

      CallThreadInfo lTimeoutThread = handleTimeout(pCurrentCallThreadInfo.mRequestContext, lCurrentApp);
      // Push Timeout Thread ID and response on current thread
      pCurrentCallThreadInfo.mThreadID = lTimeoutThread.mThreadID;
      pCurrentCallThreadInfo.mResponse = lTimeoutThread.mResponse;
    }
    else {
      if(RESUME_PARAM_TRUE_VALUE.equals(lRequest.getParameter(RESUME_PARAM_NAME))) {
        //If this is an external resume, do not attempt to process an action
        pCurrentCallThreadInfo.mResponse = pCurrentCallThreadInfo.mXThread.processExternalResume(pCurrentCallThreadInfo.mRequestContext);
      }
      else {
        //Process the action
        String lActionName = lRequest.getParameter("action_name");
        String lContextRef = lRequest.getParameter("context_ref");
        pCurrentCallThreadInfo.mResponse = pCurrentCallThreadInfo.mXThread.processAction(pCurrentCallThreadInfo.mRequestContext, lActionName, lContextRef, lRequest.getParameterMap());
      }
    }
  }

  private EntryTheme establishEntryThemeFromRequest(HttpServletRequest pRequest) {
    StringBuilder lURITail = new StringBuilder();
    XFUtil.pathPushTail(lURITail, pRequest.getPathInfo());

    String lAppMnem = XFUtil.nvl(pRequest.getParameter("app_mnem"), XFUtil.pathPopHead(lURITail, true));
    App lApp;
    try {
      lApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(lAppMnem, true);
    }
    catch (ExApp | ExServiceUnavailable e) {
      throw new ExInternal("Cannot get app " + lAppMnem, e);
    }

    String lModuleName = XFUtil.nvl(XFUtil.nvl(pRequest.getParameter("module"), XFUtil.pathPopHead(lURITail, true)), lApp.getDefaultModuleName());
    Mod lModule;
    try {
      lModule = lApp.getMod(lModuleName);
    }
    catch (ExUserRequest | ExApp | ExModule | ExServiceUnavailable e) {
      throw new ExInternal("Cannot get mod " + lModuleName, e);
    }

    String lThemeName = XFUtil.nvl(XFUtil.nvl(pRequest.getParameter("theme"), XFUtil.pathPopHead(lURITail, true)), lModule.getDefaultEntryThemeName());
    try {
      return lModule.getEntryTheme(lThemeName);
    }
    catch (ExUserRequest e) {
      throw new ExInternal("Cannot get theme " + lThemeName, e);
    }
  }

  private CallThreadInfo createNewThread(FoxRequest pFoxRequest, CallThreadInfo pCurrentCallThreadInfo, String pClientInfo, String pAppInfo)
  throws ExUserRequest {
    //No thread id - construct a new thread
    EntryTheme lEntryTheme = establishEntryThemeFromRequest(pFoxRequest.getHttpRequest());
    App lApp = lEntryTheme.getModule().getApp();

    if(lApp.isEntryThemeSecurityOn() && !lEntryTheme.isExternallyAccessible()) {
      //TODO correct status code
      throw new ExUserRequest("Entry theme not externally accessible");
    }

    try {
      // Attempt to get Auth Context/Result, re-trying with a new FOX Session if the cookie one was timed out/invalid
      AuthenticationContext lAuthContext;
      AuthenticationResult lAuthResult;
      Track.pushInfo("RequestAuthentication");
      try {
        lAuthContext = lEntryTheme.getAuthType().processBeforeEntry(pCurrentCallThreadInfo.mRequestContext);
        lAuthResult = lAuthContext.verifySession(pCurrentCallThreadInfo.mRequestContext, pClientInfo, pAppInfo);
      }
      catch (ExSessionTimeout e) {
        if (lEntryTheme.getModule().isAuthenticationRequired()) {
          // If the module they're trying to access requires auth, throw this error
          Track.info("SessionTimeout", "Session timed out and auth required");
          throw e;
        }
        else {
          // If the module they're trying has guest access, re-try with a fresh FOX Session
          Track.info("SessionTimeout", "Session timed out but auth not required; creating new FOX session");
          //TODO PN LOOK AT THIS CAREFULLY
          FoxSession lNewSession = CookieBasedFoxSession.createNewSession(pCurrentCallThreadInfo.mRequestContext, false, null);
          pCurrentCallThreadInfo.mRequestContext = RequestContextImpl.createFromExisting(pCurrentCallThreadInfo.mRequestContext, lNewSession);

          lAuthContext = lEntryTheme.getAuthType().processBeforeEntry(pCurrentCallThreadInfo.mRequestContext);
          lAuthResult = lAuthContext.verifySession(pCurrentCallThreadInfo.mRequestContext, pClientInfo, pAppInfo);
        }
      }
      finally {
        Track.pop("RequestAuthentication");
      }

      boolean lNotAuthenticated = lAuthResult.getCode() == AuthenticationResult.Code.INVALID || lAuthResult.getCode() == AuthenticationResult.Code.GUEST;

      if (lNotAuthenticated && lEntryTheme.getModule().isAuthenticationRequired()) {
        // TODO TIMEOUT HANDLING, differentiate between authenticated/guest
        Track.info("SessionTimeout", "Auth result invalid and authentication required on current entry theme; redirecting to timeout module");
        pCurrentCallThreadInfo = handleTimeout(pCurrentCallThreadInfo.mRequestContext, lApp);
      }
      else if (lAuthResult.getCode() == AuthenticationResult.Code.PASSWORD_EXPIRED && !lEntryTheme.isAllowedPasswordExpiredAccess()) {
        // TODO - Show expired screen
        Track.info("PasswordExpired", "Auth result password expired and current entry theme does not allow access");
        pCurrentCallThreadInfo.mResponse = new FoxResponseCHAR("text/html", new StringBuffer("Password expired"), 0);
      }
      else {

        XThreadBuilder lXThreadBuilder = new XThreadBuilder(lApp.getMnemonicName(), lAuthContext);

        pCurrentCallThreadInfo.mXThread =  lXThreadBuilder.createXThread(pCurrentCallThreadInfo.mRequestContext);
        pCurrentCallThreadInfo.mThreadID = pCurrentCallThreadInfo.mXThread.getThreadId();
        DOM lParamsDOM = ParamsDOMUtils.paramsDOMFromRequest(pCurrentCallThreadInfo.mRequestContext.getFoxRequest());

        pCurrentCallThreadInfo.mResponse = pCurrentCallThreadInfo.mXThread.startThread(pCurrentCallThreadInfo.mRequestContext, lEntryTheme, lParamsDOM, true);
      }
    }
    catch (ExSessionTimeout e) {
      // Force new FOX Session on context
      Track.info("SessionTimeout", "Forcing new session and handling timeout");
      FoxSession lNewSession = CookieBasedFoxSession.createNewSession(pCurrentCallThreadInfo.mRequestContext, false, null);
      pCurrentCallThreadInfo.mRequestContext = RequestContextImpl.createFromExisting(pCurrentCallThreadInfo.mRequestContext, lNewSession);

      pCurrentCallThreadInfo = handleTimeout(pCurrentCallThreadInfo.mRequestContext, lApp);
    }
    return pCurrentCallThreadInfo;
  }

  /**
   * Create a new thread for the timeout module from an existing CallThreadInfo
   *
   * @param pCurrentCallThreadInfo The current thread info that timed out
   * @param pRequestApp The current App, to get the timeout module from
   * @return new CallThreadInfo to override current CallThreadInfo or merge in
   * @throws ExUserRequest Thrown if it fails to get the timeout module
   */
  private CallThreadInfo handleTimeout(RequestContext pRequestContext, App pRequestApp) throws ExUserRequest {
    CallThreadInfo lNewThread = new CallThreadInfo();

    EntryTheme lTimeoutEntryTheme = pRequestApp.getTimeoutMod().getEntryTheme(pRequestApp.getTimeoutMod().getDefaultEntryThemeName());

    XThreadBuilder lXThreadBuilder = new XThreadBuilder(pRequestApp.getMnemonicName(), new StandardAuthenticationContext(pRequestContext));
    //TODO - mark thread as not requiring persistence

    lNewThread.mXThread = lXThreadBuilder.createXThread(pRequestContext);
    lNewThread.mThreadID = lNewThread.mXThread.getThreadId();
    lNewThread.mRequestContext = pRequestContext;
    lNewThread.mResponse = lNewThread.mXThread.startThread(pRequestContext, lTimeoutEntryTheme, ParamsDOMUtils.defaultEmptyDOM(), true);

    return lNewThread;
  }
}
