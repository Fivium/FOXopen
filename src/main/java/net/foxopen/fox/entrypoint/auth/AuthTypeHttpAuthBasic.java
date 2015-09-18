package net.foxopen.fox.entrypoint.auth;

import net.foxopen.fox.auth.AuthUtil;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.auth.ParsedAuthHeader;
import net.foxopen.fox.auth.StandardAuthenticationContext;
import net.foxopen.fox.auth.loginbehaviours.LoginBehaviour;
import net.foxopen.fox.auth.loginbehaviours.StandardLoginBehaviour;
import net.foxopen.fox.entrypoint.auth.http.BasicHttpAuthenticator;
import net.foxopen.fox.entrypoint.auth.http.HttpAuthenticationValidator;
import net.foxopen.fox.thread.RequestContext;


public class AuthTypeHttpAuthBasic implements AuthType {

  private static AuthType INSTANCE = new AuthTypeHttpAuthBasic();
  public static AuthType getInstance() {
    return INSTANCE;
  }

  private AuthTypeHttpAuthBasic() { }

  private static String HTTP_AUTH_COOKIE = "http_auth_session";

  @Override
  public AuthenticationContext processBeforeEntry(final RequestContext pRequestContext) {

    /**
     * Is digest given, read it and attempt to parse details from it
     * Otherwise issue out a 401 request
     */
    final StandardAuthenticationContext lSAC = new StandardAuthenticationContext(pRequestContext);

    BasicHttpAuthenticator.authenticateOrChallenge(pRequestContext.getFoxRequest(), new HttpAuthenticationValidator() {
      @Override
      public boolean isChallengeAllowed() {
        return true;
      }

      @Override
      public boolean authenticate(ParsedAuthHeader pParsedAuthHeader) {

        //TODO PN - FOXRD-519 - this auth/cookie code makes no sense, needs reworking
        if (pRequestContext.getFoxSession().getSessionId().equals(pRequestContext.getFoxRequest().getCookieValue(HTTP_AUTH_COOKIE))) {
          pRequestContext.getFoxRequest().removeCookie(HTTP_AUTH_COOKIE);
        }

        if (lSAC.getSessionId() == null) {
          LoginBehaviour lLoginBehaviour = new StandardLoginBehaviour(pParsedAuthHeader.mUsername, pParsedAuthHeader.mPassword, AuthUtil.getClientInfoNVP(pRequestContext.getFoxRequest()));
          AuthenticationResult lAuthResult = lSAC.login(pRequestContext, lLoginBehaviour);

          boolean lValid = lAuthResult.getCode() == AuthenticationResult.Code.VALID;
          if (!lValid) {
            //Duplicating what previous code did, may not make sense - FOXRD-519
            pRequestContext.getFoxRequest().addCookie(HTTP_AUTH_COOKIE, pRequestContext.getFoxSession().getSessionId(), false, true);
          }

          return lValid;
        }
        else {
          return true;
        }
      }
    });

    return lSAC;
  }
}
