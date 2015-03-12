package net.foxopen.fox.auth;

import net.foxopen.fox.auth.loginbehaviours.LoginBehaviour;
import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.stack.ModuleStateChangeListener;


public interface AuthenticationContext
extends ModuleStateChangeListener {

  public AuthenticationResult login (RequestContext pRequestContext, LoginBehaviour pLoginBehaviour);

  public AuthenticationResult verifySession(RequestContext pRequestContext, String pClientInfo, String pAppInfo);

  public AuthenticationResult logout(ActionRequestContext pRequestContext, String pClientInfo);

  public AuthenticatedUser getAuthenticatedUser();

  public DOMHandler getUserDOMHandler();

  public void refreshUserDOM(ActionRequestContext pRequestContext);

  public String getSessionId();

  /**
   * Tests if this AuthenticationContext currently represents an authenticated session, i.e. the user's identity has been
   * confirmed and the user is not a "guest".
   * @return
   */
  public boolean isAuthenticated();
}
