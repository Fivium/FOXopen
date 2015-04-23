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
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.ThreadLockManager;
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

  @Override
  protected String getContextUConInitialConnectionName() {
    return MAIN_CONNECTION_NAME;
  }

  private void processHttpRequest(RequestContext pRequestContext) {

    FoxRequest lFoxRequest = pRequestContext.getFoxRequest();
    ContextUCon lContextUCon = pRequestContext.getContextUCon();

    String lThreadId = XFUtil.nvl(lFoxRequest.getParameter(THREAD_ID_PARAM_NAME)).trim();

    FoxResponse lFoxResponse;
    try {
      String lClientInfo = "IP="+InetAddress.getLocalHost().getHostAddress()+", REMOTE-ADDR="+lFoxRequest.getHttpRequest().getRemoteAddr();

      if(XFUtil.isNull(lThreadId)){
        //No ID provided - create a new thread (avoid the locking mechanism - new threads don't need to be locked)
        lFoxResponse = createNewThread(pRequestContext, lClientInfo);

        //Validates all transactions except the MAIN transaction are committed
        lContextUCon.closeAllRetainedConnections();

        //Commit the MAIN connection - commits all work done by thread
        lContextUCon.commit(MAIN_CONNECTION_NAME);
      }
      else {
        //Authenticate, lock thread and run action using the ThreadLockManager
        lFoxResponse = resumeThread(pRequestContext, lThreadId, lClientInfo);
      }
    }
    catch (Throwable th) {
      throw new ExInternal("Error processing request", th);
    }

    Track.pushInfo("SendResponse");
    try {
      lFoxResponse.respond(lFoxRequest);
    }
    finally {
      Track.pop("SendResponse");
    }
  }

  private FoxResponse resumeThread(RequestContext pRequestContext, String pThreadId, final String pClientInfo)
  throws ExServiceUnavailable, ExApp, ExUserRequest {

    Track.setProperty(TrackProperty.THREAD_ID, pThreadId);

    ThreadLockManager<FoxResponse> lThreadLockManager = new ThreadLockManager<>(pThreadId, MAIN_CONNECTION_NAME, false);
    return lThreadLockManager.lockAndPerformAction(pRequestContext, new ThreadLockManager.LockedThreadRunnable<FoxResponse>() {
      @Override
      public FoxResponse doWhenLocked(RequestContext pRequestContext, StatefulXThread pXThread) {

        FoxResponse lFoxResponse;
        try {
          //Check the database WUS for the thread is still valid
          AuthenticationResult lAuthResult;
          Track.pushInfo("RequestAuthentication");
          try {
            AuthenticationContext lAuthContext = pXThread.getAuthenticationContext();
            lAuthResult = lAuthContext.verifySession(pRequestContext, pClientInfo, "TODO thread last module");
          }
          finally {
            Track.pop("RequestAuthentication");
          }

          //TODO - if thread has 0 state calls, interpret as a timeout and redirect to timeout screen (so UX is improved when user exits, hits back and tries another action)

          if(lAuthResult.getCode() != AuthenticationResult.Code.VALID && lAuthResult.getCode() != AuthenticationResult.Code.GUEST){

            //TODO disallow guest access for auth required modules
            App lCurrentApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(pXThread.getThreadAppMnem(), true);

            Track.info("SessionInvalid", "Session is no longer valid (" + lAuthResult.getCode().toString() + "); handling timeout");

            lFoxResponse = handleTimeout(pRequestContext, lCurrentApp);
          }
          else {
            FoxRequest lFoxRequest = pRequestContext.getFoxRequest();
            if(RESUME_PARAM_TRUE_VALUE.equals(lFoxRequest.getParameter(RESUME_PARAM_NAME))) {
              //If this is an external resume, do not attempt to process an action
              lFoxResponse = pXThread.processExternalResume(pRequestContext);
            }
            else {
              //Not an external resume so must be an action request (i.e. form churn) - process the action
              String lActionName = lFoxRequest.getParameter("action_name");
              String lContextRef = lFoxRequest.getParameter("context_ref");
              lFoxResponse = pXThread.processAction(pRequestContext, lActionName, lContextRef, lFoxRequest.getHttpRequest().getParameterMap());
            }
          }
        }
        catch (ExServiceUnavailable | ExApp | ExUserRequest e) {
          throw new ExInternal("Failed to resume thread", e);
        }

        return lFoxResponse;
      }
    });
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

  private FoxResponse createNewThread(RequestContext pRequestContext, String pClientInfo)
  throws ExUserRequest {
    //No thread id - construct a new thread
    EntryTheme lEntryTheme = establishEntryThemeFromRequest(pRequestContext.getFoxRequest().getHttpRequest());
    App lApp = lEntryTheme.getModule().getApp();

    if(lApp.isEntryThemeSecurityOn() && !lEntryTheme.isExternallyAccessible()) {
      //TODO correct status code
      throw new ExUserRequest("Entry theme not externally accessible");
    }

    FoxResponse lFoxResponse;
    try {
      // Attempt to get Auth Context/Result, re-trying with a new FOX Session if the cookie one was timed out/invalid
      AuthenticationContext lAuthContext;
      AuthenticationResult lAuthResult;
      Track.pushInfo("RequestAuthentication");
      try {
        lAuthContext = lEntryTheme.getAuthType().processBeforeEntry(pRequestContext);
        lAuthResult = lAuthContext.verifySession(pRequestContext, pClientInfo, "(direct entry)");
      }
      catch (ExSessionTimeout e) {
        //Sent FOX session ID was stale/expired
        if (lEntryTheme.getModule().isAuthenticationRequired()) {
          // If the module they're trying to access requires auth, throw this error
          Track.info("SessionTimeout", "Session timed out and auth required");
          throw e;
        }
        else {
          // If the module they're trying has guest access, re-try with a fresh FOX Session
          Track.info("SessionTimeout", "Session timed out but auth not required; creating new FOX session");
          pRequestContext.forceNewFoxSession(CookieBasedFoxSession.createNewSession(pRequestContext, false, null));

          lAuthContext = lEntryTheme.getAuthType().processBeforeEntry(pRequestContext);
          lAuthResult = lAuthContext.verifySession(pRequestContext, pClientInfo, "(direct entry)");
        }
      }
      finally {
        Track.pop("RequestAuthentication");
      }

      boolean lNotAuthenticated = lAuthResult.getCode() == AuthenticationResult.Code.INVALID || lAuthResult.getCode() == AuthenticationResult.Code.GUEST;

      if (lNotAuthenticated && lEntryTheme.getModule().isAuthenticationRequired()) {
        // TODO TIMEOUT HANDLING, differentiate between authenticated/guest
        Track.info("SessionTimeout", "Auth result invalid and authentication required on current entry theme; redirecting to timeout module");
        lFoxResponse = handleTimeout(pRequestContext, lApp);
      }
      else if (lAuthResult.getCode() == AuthenticationResult.Code.PASSWORD_EXPIRED && !lEntryTheme.isAllowedPasswordExpiredAccess()) {
        // TODO - Show expired screen
        Track.info("PasswordExpired", "Auth result password expired and current entry theme does not allow access");
        lFoxResponse = new FoxResponseCHAR("text/html", new StringBuffer("Password expired"), 0);
      }
      else {
        //Authentication passed - create the new thread
        XThreadBuilder lXThreadBuilder = new XThreadBuilder(lApp.getMnemonicName(), lAuthContext);

        StatefulXThread lNewXThread =  lXThreadBuilder.createXThread(pRequestContext);
        DOM lParamsDOM = ParamsDOMUtils.paramsDOMFromRequest(pRequestContext.getFoxRequest());

        lFoxResponse = lNewXThread.startThread(pRequestContext, lEntryTheme, lParamsDOM, true);
      }
    }
    catch (ExSessionTimeout e) {
      // Force a new FOX Session on response so subsequent requests don't have the same problem
      Track.info("SessionTimeout", "Forcing new session and handling timeout");
      pRequestContext.forceNewFoxSession(CookieBasedFoxSession.createNewSession(pRequestContext, false, null));

      lFoxResponse = handleTimeout(pRequestContext, lApp);
    }

    return lFoxResponse;
  }

  /**
   * Create a new thread for the timeout module from an existing CallThreadInfo
   *
   * @param pCurrentCallThreadInfo The current thread info that timed out
   * @param pRequestApp The current App, to get the timeout module from
   * @return new CallThreadInfo to override current CallThreadInfo or merge in
   * @throws ExUserRequest Thrown if it fails to get the timeout module
   */
  private FoxResponse handleTimeout(RequestContext pRequestContext, App pRequestApp) throws ExUserRequest {
    EntryTheme lTimeoutEntryTheme = pRequestApp.getTimeoutMod().getDefaultEntryTheme();

    XThreadBuilder lXThreadBuilder = new XThreadBuilder(pRequestApp.getMnemonicName(), new StandardAuthenticationContext(pRequestContext));

    //TODO - mark thread as not requiring persistence

    StatefulXThread lNewThread = lXThreadBuilder.createXThread(pRequestContext);
    return lNewThread.startThread(pRequestContext, lTimeoutEntryTheme, ParamsDOMUtils.defaultEmptyDOM(), true);
  }
}
