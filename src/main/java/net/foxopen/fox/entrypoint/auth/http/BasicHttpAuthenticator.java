package net.foxopen.fox.entrypoint.auth.http;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.ParsedAuthHeader;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Implementation of Basic HTTP Authentication behaviour.
 */
public class BasicHttpAuthenticator {

  private static String HTTP_AUTH_REALM = "HTTP AUTHENTICATION";

  /**
   * Attempts to authenticate a request using Basic HTTP authentication, with the provided validation behaviour. If the
   * validation is successful this method returns true and performs no further action. If the validation fails, this method
   * immediately issues a 401 response (usually with a challenge unless specified by the HttpAuthenticationValidator).
   * Therefore, consumers MUST check the return value of this method and only attempt to send a response if the authentication
   * succeeded.
   * @param pFoxRequest Request to authenticate.
   * @param pHttpAuthenticationValidator Functions to perform the actual authentication.
   * @return True if authentication succeeded, false if it failed and therefore a 401 challenge was sent.
   */
  public static boolean authenticateOrChallenge(FoxRequest pFoxRequest, HttpAuthenticationValidator pHttpAuthenticationValidator) {

    ParsedAuthHeader lParsedAuthHeader = extractAuthorizationHeader(pFoxRequest.getHttpRequest());

    boolean lAuthenticated = false;
    if(lParsedAuthHeader != null) {
      lAuthenticated = pHttpAuthenticationValidator.authenticate(lParsedAuthHeader);
    }

    if(!lAuthenticated) {
      if(pHttpAuthenticationValidator.isChallengeAllowed()) {
        getAuthenticateChallengeResponse(pFoxRequest).respond(pFoxRequest);
      }
      else {
        getUnauthorizedResponse().respond(pFoxRequest);
      }
      return false;
    }
    else {
      return true;
    }
  }

  /**
   * Get authorisation details from the "Authorization" header where the header value is like "Basic V2hhdERvZXNUaGU6Rm94U2F5".
   * Only "Basic" HTTP auth is supported - for other attempts this method returns null.
   * @param pRequest The http request containing the headers
   * @return Header details (username and password) or null.
   */
  private static ParsedAuthHeader extractAuthorizationHeader(HttpServletRequest pRequest) {
    String lAuthHeader = pRequest.getHeader("Authorization");
    String lAuthType = null;
    String lUsername = null;
    String lPassword = null;

    if (lAuthHeader != null) {
      // If the header exists we can try and extract the data from it.
      // Provided it exists we can assume it will contain both authentication
      // type and credentials. A request with blank username and password
      // will still send ":" in Base64 for credentials
      String[] authHeaderTokens = lAuthHeader.split(" ");
      lAuthType = authHeaderTokens[0].toUpperCase();
      String[] lAuthCredentials = new String(Base64.decodeBase64(authHeaderTokens[1].getBytes())).split(":", 2);
      if (lAuthCredentials.length == 2) {
        lUsername = lAuthCredentials[0];
        lPassword = lAuthCredentials[1];
      }
      else if(lAuthCredentials.length == 1) {
        lUsername = lAuthCredentials[0];
      }

      //Only support BASIC authentication at the moment
      if(HttpServletRequest.BASIC_AUTH.equals(lAuthType)) {
        return new ParsedAuthHeader(lAuthType, lUsername, lPassword);
      }
    }

    return null;
  }

  /**
   * Generate a 401 Unauthorised FoxResponse
   * TODO - NP - There should be a config option to perhaps set this to a module, e.g. timeout module
   *
   * @return 401 Unauthorized response
   */
  private static FoxResponse getUnauthorizedResponse() {
    FoxResponse lResponse = new FoxResponseCHAR("text/html", new StringBuffer("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" +
                                                                              "<html><head>\n" +
                                                                              "<title>401 Authorization Required</title>\n" +
                                                                              "</head><body>\n" +
                                                                              "<h1>Authorization Required</h1>\n" +
                                                                              "<p>This server could not verify that you\n" +
                                                                              "are authorized to access the document\n" +
                                                                              "requested.  Either you supplied the wrong\n" +
                                                                              "credentials (e.g. bad password), or your\n" +
                                                                              "browser doesn't understand how to supply\n" +
                                                                              "the credentials required.</p>\n" +
                                                                              "</body></html>\n"), 0);
    lResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return lResponse;
  }

  private static FoxResponse getAuthenticateChallengeResponse(FoxRequest pFoxRequest) {
    /**
     * User Agent Check -- IMPORTANT
     *
     * We need to detect the User Agent when generating the realm for this challenge.
     * This is because the credential storage behaviour for different realms differs drastically
     * between IE and other browsers.
     *
     * IE seemingly stores HTTP Authentication credentials for at most one realm per domain. When a
     * challenge is issued on a new realm, IE discards the credential information for the previous
     * realm, even if the user then goes on to click the 'Cancel' button on the login prompt. This
     * means that changing the authentication realm forces IE to clear its cached credentials and
     * the user is effectively 'logged out' of their HTTP Authenticated session.
     *
     * Firefox, however, is more capable and will store credentials for multiple realms.
     * If a new realm challenge is issued to these browsers, the previous valid credentials remain
     * cached if the user clicks 'Cancel' on the login prompt. This becomes a problem because there
     * is no way to detect which realm the credentials are destined for when the browser sends them.
     * Resultantly, it is possible to be timed out, re-enter the system and cancel the password prompt,
     * then re-enter the system again. The browser will resend the credentials from the first realm and
     * the user will be logged in without having to re-enter their password. The solution to this is
     * to have a fixed realm, so that the 'Cancel' action forces the browser to wipe the credentials,
     * but this is directly contradictory to what needs to be done to support IE, which does NOT clear
     * credentials for the SAME realm on a 'Cancel'.
     *
     * So, we detect the user agent and branch the logic accordingly. Clearly this is a security
     * weakness - but it will only be an issue if a user is somehow spoofing their user-agent string
     * and re-entering in the described pattern after a session timeout. They will definitely still
     * have entered their correct username and password at some point.
     *
     * NB Chrome will resend credentials for a static realm without prompting for a username/password
     * when it receives a challenge. Annoyingly it also exhibits Firefox's multuple realm storing
     * behaviour. This makes it difficult to properly log someone out of an HTTP Auth'd session on
     * Chrome - a weakness that may need addressing in the future.
     */

    String lUserAgent = pFoxRequest.getHttpRequest().getHeader("User-Agent");
    String lRealm;

    if(lUserAgent != null && (lUserAgent.indexOf(" MSIE ") > 0 || lUserAgent.indexOf(" Chrome") > 0)){
      //IE or Chrome; variable realm
      lRealm = XFUtil.unique();
    }
    else {
      //All other browsers; static realm
      lRealm = HTTP_AUTH_REALM;
    }

    FoxResponse lResponse = getUnauthorizedResponse();
    String lAuthValue = HttpServletRequest.BASIC_AUTH + " realm=\"" +  lRealm  + "\"";
    lResponse.setHttpHeader("WWW-Authenticate", lAuthValue);
    return lResponse;
  }
}
