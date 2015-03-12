package net.foxopen.fox.entrypoint.auth;

import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.StandardAuthenticationContext;
import net.foxopen.fox.thread.RequestContext;


public class AuthTypeHttpHeader implements AuthType {
  private static AuthType INSTANCE = new AuthTypeHttpHeader();
  public static AuthType getInstance() {
    return INSTANCE;
  }
  private AuthTypeHttpHeader() {
  }

  @Override
  public AuthenticationContext processBeforeEntry(RequestContext pRequestContext) {
    // TODO NP - Implement basic 401 auth:
    // http://en.wikipedia.org/wiki/Basic_access_authentication

    /**
     * Is header given, read it and attempt to parse details from it
     * Otherwise issue "Not allowed here" response
     */
    return new StandardAuthenticationContext(pRequestContext);
  }
}
