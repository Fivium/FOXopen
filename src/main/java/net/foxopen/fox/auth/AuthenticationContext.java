package net.foxopen.fox.auth;

import net.foxopen.fox.auth.loginbehaviours.LoginBehaviour;
import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.stack.ModuleStateChangeListener;


public interface AuthenticationContext
extends ModuleStateChangeListener {

  AuthenticationResult login (RequestContext pRequestContext, LoginBehaviour pLoginBehaviour);

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
