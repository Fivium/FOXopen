package net.foxopen.fox.auth.loginbehaviours;

import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.thread.RequestContext;

/**
 * Login behaviour objects can be constructed with arbitrary information to facilitate a login method, e.g. user/pass or SSO credentials
 */
public interface LoginBehaviour {

  public AuthenticationResult login(RequestContext pRequestContext);
}
