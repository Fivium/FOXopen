package net.foxopen.fox.entrypoint.auth;

import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.StandardAuthenticationContext;
import net.foxopen.fox.thread.RequestContext;


public class AuthTypeSAML implements AuthType {
  private static AuthType INSTANCE = new AuthTypeSAML();
  public static AuthType getInstance() {
    return INSTANCE;
  }
  private AuthTypeSAML() {
  }

  @Override
  public AuthenticationContext processBeforeEntry(RequestContext pRequestContext) {
    // TODO - Implement SAML

    /**
     * If not logged in and no SAML posted, respond with a self posting form to the designated Identity Provider
     * (externally, not part of FOX) Form self-posts to the IdP from the client browser, the IdP confirms the user details and responds to them with another self-posting form back to FOX
     * We can then see a SAML response, confirm the signature, check the timeouts and then extract any user details from it
     * Use those user details to login via some kind of SSOAuthenticationContext that calls the SSO methods on the Authentication package
     * Redirect user to the module/theme they tried to go to in the first place, now logged in
     *
     * See: http://en.wikipedia.org/wiki/SAML_2.0#SP_POST_Request.3B_IdP_POST_Response
     */
    return new StandardAuthenticationContext(pRequestContext);
  }
}
