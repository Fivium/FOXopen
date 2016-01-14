package net.foxopen.fox.auth;

import net.foxopen.fox.StringUtil;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.loginbehaviours.LoginBehaviour;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackProperty;

import java.util.Set;

/**
 *  AuthenticationConext which uses the legacy database session manager to provide authentication details. Session data
 *  is stored in the web_user_sessions table and accessed via PL/SQL APIs. This also provides user DOM caching, by only
 *  refreshing the user DOM when a change is detected on the database.
 *
 *  Note: this implementation assumes verifySession is called at the start of every churn. If it is not, the user DOM
 *  may not be correctly refreshed, as the refresh behaviour depends on the call to verifySession.
 *  Note: objects of this class are serialised by XStream.
 */
public class StandardAuthenticationContext
implements AuthenticationContext {

  private static final String SESSION_VERIFY_FILENAME = "SessionVerify.sql";
  private static final String SESSION_END_FILENAME = "SessionEnd.sql";
  private static final String GET_USER_DOM_FILENAME = "GetUserDOM.sql";

  private final transient UserDOMHandler mUserDOMHandler = new UserDOMHandler(); //Marked as transient for XStream serialise

  /** Null = not authenticated */
  private String mSessionId = null;

  private AuthenticatedUser mAuthenticatedUser;

  private transient String mLatestUserDOMChangeNumber;

  private int mSessionTimeoutMins;

  private boolean mHasLatestUserDOM = false;

  private SecurityScope mLastSecurityScope;

  public StandardAuthenticationContext(RequestContext pRequestContext) {
    refreshUserInfo(pRequestContext, SecurityScope.defaultInstance(), true);
    mLastSecurityScope = SecurityScope.defaultInstance();
  }

  //for deserialising
  public StandardAuthenticationContext(String pSessionId) {
    mSessionId = pSessionId;
  }

  @Override
  public AuthenticationResult login (RequestContext pRequestContext, LoginBehaviour pLoginBehaviour) {
    // Login using the LoginBehaviour
    AuthenticationResult lAuthenticationResult = pLoginBehaviour.login(pRequestContext);

    // create cookie/session for any status that isn't INVALID
    if (lAuthenticationResult.getCode() != AuthenticationResult.Code.INVALID) {
      setSessionId(lAuthenticationResult.getSessionId());
    }

    //TODO update request log - outside
    //Fox.updateRequestLog(FoxRequest.getCurrentFoxRequest(), "wus_id", lAuthDescriptor.getSessionId());

    refreshUserInfo(pRequestContext, pRequestContext.getCurrentSecurityScope(), true);

    //Force a new FOX Session ID to reflect the change in authentication state
    pRequestContext.getFoxSession().forceNewFoxSessionID(pRequestContext, mSessionId);

    return lAuthenticationResult;
  }

  @Override
  public AuthenticationResult verifySession(RequestContext pRequestContext, String pClientInfo, String pAppInfo) {
    //Skip trip to database if session id is known to be null (i.e. this session has guest access)
    if(mSessionId == null) {
      return new AuthenticationResult(AuthenticationResult.Code.GUEST, "Guest Access Authorised", null);
    }

    UConBindMap lBindMap = new UConBindMap()
      .defineBind(":client_info", pClientInfo)
      .defineBind(":app_display_name", pAppInfo)
      .defineBind(":session_id", mSessionId)
      .defineBind(":wua_id_out", UCon.bindOutString())
      .defineBind(":status_out", UCon.bindOutString())
      .defineBind(":message_out", UCon.bindOutString())
      .defineBind(":timeout_mins_out", UCon.bindOut(BindSQLType.NUMBER))
      .defineBind(":user_dom_change_no", mLatestUserDOMChangeNumber)
      .defineBind(":user_dom_current", UCon.bindOutString())
      .defineBind(":session_id_out", UCon.bindOutString());

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Session Verify");
    UConStatementResult lAPIResult;
    try {
      lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(SESSION_VERIFY_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to verify session", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Session Verify");
    }

    String lStatus = lAPIResult.getString(":status_out");
    String lMessage = lAPIResult.getString(":message_out");
    int lTimeoutMins = lAPIResult.getInteger(":timeout_mins_out");
    String lSessionID = lAPIResult.getString(":session_id_out");

    AuthenticationResult.Code lAuthResultCode = AuthenticationResult.Code.fromString(lStatus);

    if(lAuthResultCode == AuthenticationResult.Code.VALID || lAuthResultCode == AuthenticationResult.Code.PASSWORD_EXPIRED){
      //User authenticated and Session Id valid, continue normally
      //Note: password expiry is treated as a "valid" session, but the user should be restricted to the password reset screen
      mSessionTimeoutMins = lTimeoutMins;

      //See if we have the latest user DOM - if we do we can skip the refresh
      mHasLatestUserDOM = "true".equals(lAPIResult.getString(":user_dom_current"));

      //Refresh the session ID - is this necessary? Probably not but was legacy behaviour
      setSessionId(lSessionID);

      return new AuthenticationResult(lAuthResultCode, lMessage, mSessionId);
    }
    else {
      //No way to differentiate invalid session and timeout - TODO PN XTHREAD enhance (needs DB change)
      invalidate();

      //Refresh user DOM to reflect logout
      refreshUserInfo(pRequestContext, SecurityScope.defaultInstance(), true);

      //Force a new FOX Session ID to reflect the change in authentication state
      pRequestContext.getFoxSession().forceNewFoxSessionID(pRequestContext, mSessionId);

      return new AuthenticationResult(AuthenticationResult.Code.INVALID, lMessage, null);
    }
  }

  @Override
  public AuthenticationResult logout(ActionRequestContext pRequestContext, String pClientInfo) {

    String lWuaId = pRequestContext.getAuthenticationContext().getAuthenticatedUser().getAccountID();

    //---------------------------------------------------------------------
    // Log the user out of the database session
    //---------------------------------------------------------------------
    if(mSessionId != null) {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":client_info", pClientInfo);
      lBindMap.defineBind(":app_display_name",  "FOX-SYSTEM");
      lBindMap.defineBind(":wua_id", lWuaId);
      lBindMap.defineBind(":session_id", mSessionId);
      lBindMap.defineBind(":status_code", UCon.bindOutString());
      lBindMap.defineBind(":status_message", UCon.bindOutString());

      UConStatementResult lAPIResult = null;
      UCon lUCon = pRequestContext.getContextUCon().getUCon("User Logout");
      try {
        lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(SESSION_END_FILENAME, getClass()), lBindMap);
      }
      catch (ExDB e) {
        throw new ExInternal("Error during logout API execution", e);
      }
      finally {
        pRequestContext.getContextUCon().returnUCon(lUCon, "User Logout");
      }

      String lCodeString = lAPIResult.getString(":status_code");

      AuthenticationResult.Code lCode = AuthenticationResult.Code.fromString(lCodeString);

      //Regardless of result of the API call, accept the logout
      invalidate();

      //Refresh user DOM to reflect logout
      refreshUserInfo(pRequestContext, pRequestContext.getCurrentSecurityScope(), true);

      //Create a new FoxSession because login state has changed. Note new session should NOT have a reference to this WUS ID.
      pRequestContext.getFoxSession().forceNewFoxSessionID(pRequestContext, "");

      return new AuthenticationResult(lCode, lAPIResult.getString(":status_message"), null);
    }
    else {
      Track.info("SkipLogOut", "Skipped logout command as not logged in");
      return new AuthenticationResult(AuthenticationResult.Code.GUEST, "Already logged out", null);
    }
  }

  private void invalidate(){
    setSessionId(null);
    mSessionTimeoutMins = 0;
  }

  private void setSessionId(String pSessionId) {
    mSessionId = pSessionId;
  }

  /**
   *
   * @param pRequestContext
   * @param pSecurityScope Use defaultInstance if not known
   */
  private void refreshUserInfo(RequestContext pRequestContext, SecurityScope pSecurityScope, boolean pForceRefresh) {

    //Check if the security scope has changed since last refresh. Note this relies on SecurityScope.equals() checking the isDefault() flag, so 2 defaults evaluate to the same scope.
    boolean lSecurityScopeChanged = mLastSecurityScope != null && !mLastSecurityScope.equals(pSecurityScope);

    if(pForceRefresh || lSecurityScopeChanged ) {

      //If the security scope has changed we must force the API to generate a new DOM with new SecurityScope data.
      //Otherwise, we only need a refresh if our copy of the DOM is stale, so tell the API our current change number.
      String lDOMChangeNumber;
      if (lSecurityScopeChanged) {
        lDOMChangeNumber = "*NEW_SECURITY_SCOPE*";
      }
      else {
        lDOMChangeNumber = mLatestUserDOMChangeNumber;
      }

      //TODO this behaviour could be composed on a member (like old FoxUser)
      Track.pushInfo("StandardAuthentication", "Refreshing user DOM");
      try {
        UConBindMap lBindMap = new UConBindMap()
          .defineBind(":user_dom", UCon.bindOutXML())
          .defineBind(":current_change_number", lDOMChangeNumber)
          .defineBind(":session_id", XFUtil.nvl(mSessionId, ""))
          .defineBind(":csv_sys_privs", pSecurityScope.getCsvSystemPrivileges())
          .defineBind(":csv_uref_list", pSecurityScope.getCsvURefList())
          .defineBind(":csv_uref_privs", pSecurityScope.getCsvObjectPrivileges())
          .defineBind(":csv_uref_types", pSecurityScope.getCsvURefTypes())
          .defineBind(":privs_csv", UCon.bindOutString())
          .defineBind(":latest_change_number", UCon.bindOutString());

        UConStatementResult lAPIResult;
        UCon lUCon = pRequestContext.getContextUCon().getUCon("User DOM");
        DOM lUserDOM;
        try {
          lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(GET_USER_DOM_FILENAME, getClass()), lBindMap);
          lUserDOM = lAPIResult.getDOMFromSQLXML(":user_dom");
        }
        catch (ExDB e) {
          throw new ExInternal("Failed to get user DOM", e);
        }
        finally {
          pRequestContext.getContextUCon().returnUCon(lUCon, "User DOM");
        }

        mLatestUserDOMChangeNumber = lAPIResult.getString(":latest_change_number");
        mLastSecurityScope = pSecurityScope;

        //If the returned DOM was null, a refresh is required
        if (lUserDOM != null) {
          Track.info("NewUserDOM", "Change number " + mLatestUserDOMChangeNumber);
          String lPrivsCSV = XFUtil.nvl(lAPIResult.getString(":privs_csv"), "");
          Set<String> lPrivSet = StringUtil.commaDelimitedListToSet(lPrivsCSV);

          mAuthenticatedUser = new StandardAuthenticatedUser(lUserDOM, lPrivSet);

          mUserDOMHandler.refreshDOM(lUserDOM);
        }
        else {
          Track.info("UseCachedUserDOM", "Cached User DOM is up to date");
        }

        mHasLatestUserDOM = true;
      }
      finally {
        Track.pop("StandardAuthentication");
      }
    }
  }

  @Override
  public DOMHandler getUserDOMHandler() {
    return mUserDOMHandler;
  }

  @Override
  public void handleStateChange(RequestContext pRequestContext, EventType pEventType, ModuleCallStack pCallStack) {

    if(pCallStack.getStackSize() > 0  && (pEventType == EventType.MODULE || pEventType == EventType.SECURITY_SCOPE)) {
      refreshUserInfo(pRequestContext, pCallStack.getTopModuleCall().getSecurityScope(), false);
    }
  }

  @Override
  public AuthenticatedUser getAuthenticatedUser() {
    return mAuthenticatedUser;
  }

  /**
   * Can be null
   * @return
   */
  public String getSessionId() {
    return mSessionId;
  }

  @Override
  public boolean isAuthenticated() {
    return mSessionId != null;
  }

  @Override
  public void refreshUserDOM(ActionRequestContext pRequestContext) {
    if(!mHasLatestUserDOM){
      refreshUserInfo(pRequestContext, pRequestContext.getCurrentSecurityScope(), true);
    }
    else {
      Track.info("SkipGetUserDOM", "User DOM has already been checked by session verify and is up to date");
    }

    Track.setProperty(TrackProperty.AUTHENTICATED_USER_ID, mAuthenticatedUser.getAccountID());
    Track.setProperty(TrackProperty.AUTHENTICATED_SESSION_ID, mSessionId);
  }

  @Override
  public int getSessionTimeoutMins() {
    return mSessionTimeoutMins;
  }
}
