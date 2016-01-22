package net.foxopen.fox.auth;

import net.foxopen.fox.auth.loginbehaviours.LoginBehaviour;
import net.foxopen.fox.auth.loginbehaviours.ResumeLoginBehaviour;
import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.stack.ModuleStateChangeListener;


public interface AuthenticationContext
extends ModuleStateChangeListener {

  AuthenticationResult login (RequestContext pRequestContext, LoginBehaviour pLoginBehaviour);

  /**
   * Resumes a partially completed login attempt (i.e. for two-factor authentication).
   * @param pRequestContext Current RequestContext.
   * @param pLoginBehaviour Method for resuming a login (e.g. using a validation hash code)
   * @return The result of the resume attempt.
   */
  AuthenticationResult resumeLogin (RequestContext pRequestContext, ResumeLoginBehaviour pLoginBehaviour);

  AuthenticationResult verifySession(RequestContext pRequestContext, String pClientInfo, String pAppInfo);

  AuthenticationResult logout(ActionRequestContext pRequestContext, String pClientInfo);

  AuthenticatedUser getAuthenticatedUser();

  DOMHandler getUserDOMHandler();

  void refreshUserDOM(ActionRequestContext pRequestContext);

  String getSessionId();

  /**
   * Tests if this AuthenticationContext currently represents an authenticated session, i.e. the user's identity has been
   * confirmed and the user is not a "guest".
   * @return True if user is authenticated.
   */
  boolean isAuthenticated();

  /**
   * Gets the number of minutes the user's session is valid for. This will only return a usable value if {@link #isAuthenticated()}
   * is true. Otherwise the result is undefined.
   * @return Session time left in minutes.
   */
  int getSessionTimeoutMins();

}
