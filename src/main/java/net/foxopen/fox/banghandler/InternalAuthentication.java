package net.foxopen.fox.banghandler;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.ParsedAuthHeader;
import net.foxopen.fox.configuration.FoxBootConfig;
import net.foxopen.fox.configuration.FoxConfigHelper;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.auth.http.BasicHttpAuthenticator;
import net.foxopen.fox.entrypoint.auth.http.HttpAuthenticationValidator;
import net.foxopen.fox.logging.FoxLogger;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton which provides internal authentication, i.e. for !LOGIN and FoxBoot access. Actual authentication state
 * is stored on the browser session.
 */
public class InternalAuthentication {

  private static final InternalAuthentication INSTANCE = new InternalAuthentication();

  private static final int AUTH_TIMEOUT_SECONDS = 300;
  private static final int AUTH_MAX_ATTEMPTS = 5;

  private static final String SESSION_AUTH_LEVEL_ATTRIBUTE = "net.foxopen.banghandler.InternalAuthentication.SessionAuthLevel";

  private final AtomicInteger mFoxInternalLoginAttempts = new AtomicInteger(0);
  private Calendar mFoxInternalLoginBlockedUntil;

  public static InternalAuthentication instance() {
    return INSTANCE;
  }

  private InternalAuthentication() { }

  /**
   * Determines the internal authentication level of this request, without issuing a challenge.
   * @param pFoxRequest
   * @return
   */
  public InternalAuthLevel getSessionAuthLevel(FoxRequest pFoxRequest) {
    return XFUtil.nvl((InternalAuthLevel) pFoxRequest.getHttpRequest().getSession().getAttribute(SESSION_AUTH_LEVEL_ATTRIBUTE), InternalAuthLevel.NONE);
  }

  /**
   * Forces this request to be internally authenticated to the requested level. If the request is already authenticated,
   * no action is taken. Otherwise a 401 challenge/response is immediately issued. Consumers should inspect the return value
   * of this method and only provide their own response if this method returns true.
   * @param pFoxRequest Request to authenticate.
   * @param pRequiredAuthLevel The minimum auth level required. This will result in a challenge being issued if the user
   *                           is logged in as "support" but "admin" is required.
   * @return True if the session is authenticated, or false if it is not and a 401 response was issued.
   */
  public boolean authenticate(FoxRequest pFoxRequest, final InternalAuthLevel pRequiredAuthLevel) {

    final HttpServletRequest lHttpServletRequest = pFoxRequest.getHttpRequest();

    //Check if the user is already authenticated on this browser session
    InternalAuthLevel lSessionAuthLevel = getSessionAuthLevel(pFoxRequest);
    if(pRequiredAuthLevel.intValue() > lSessionAuthLevel.intValue()) {

      //Check the HTTP headers/issue a challenge if not sent
      boolean lAuthSuccess = BasicHttpAuthenticator.authenticateOrChallenge(pFoxRequest, new HttpAuthenticationValidator() {

        @Override
        public boolean isChallengeAllowed() {
          //Only send a challenge if the engine is not cooling down from too many invalid attempts
          synchronized (InternalAuthentication.this) {
            return mFoxInternalLoginBlockedUntil == null || Calendar.getInstance().after(mFoxInternalLoginBlockedUntil);
          }
        }

        @Override
        public boolean authenticate(ParsedAuthHeader pParsedAuthHeader) {

          FoxBootConfig lFoxBootConfig = FoxGlobals.getInstance().getFoxBootConfig();
          if (pParsedAuthHeader.mUsername.equals(lFoxBootConfig.getAdminUsername())) {
            //Check admin level access
            if (FoxConfigHelper.verifyInternalPassword(lFoxBootConfig.getAdminPassword(), pParsedAuthHeader.mPassword)) {
              //Cache the result on the session
              lHttpServletRequest.getSession().setAttribute(SESSION_AUTH_LEVEL_ATTRIBUTE, InternalAuthLevel.INTERNAL_ADMIN);
              //Reset failure counter
              successfulAuthentication();
              return true;
            }
          }
          else if (pParsedAuthHeader.mUsername.equals(lFoxBootConfig.getSupportUsername())) {
            //Check support level access
            if (FoxConfigHelper.verifyInternalPassword(lFoxBootConfig.getSupportPassword(), pParsedAuthHeader.mPassword)) {
              //Cache the result on the session
              lHttpServletRequest.getSession().setAttribute(SESSION_AUTH_LEVEL_ATTRIBUTE, InternalAuthLevel.INTERNAL_SUPPORT);
              //Reset failure counter
              successfulAuthentication();
              //Only allow the authentication if support access is allowed. Otherwise we should issue a challenge to get full admin credentials
              return pRequiredAuthLevel.intValue() <= InternalAuthLevel.INTERNAL_SUPPORT.intValue();
            }
          }

          //Passwords did not match - record this invalid login attempt
          invalidLoginAttempt();
          return false;
        }
      });

      //Only allow user in if the authentication attempt succeeded AND they are now logged in at the required level
      return lAuthSuccess && getSessionAuthLevel(pFoxRequest).intValue() >= pRequiredAuthLevel.intValue();
    }
    else {
      //User is already authenticated to the required level for this browser session
      return true;
    }
  }

  private void invalidLoginAttempt() {

    if(mFoxInternalLoginAttempts.incrementAndGet() > AUTH_MAX_ATTEMPTS) {
      Calendar lCalendar = Calendar.getInstance();
      lCalendar.add(Calendar.SECOND, AUTH_TIMEOUT_SECONDS);
      setFoxInternalLoginBlockedUntil(lCalendar);
      FoxLogger.getLogger().warn("Bang login over HTTP Auth suspended for {} seconds due to failed login attempts", AUTH_TIMEOUT_SECONDS);
    }

    //TODO PN proper failure audit
  }

  private void successfulAuthentication() {
    setFoxInternalLoginBlockedUntil(null);
    mFoxInternalLoginAttempts.set(0);
  }

  private synchronized void setFoxInternalLoginBlockedUntil(Calendar pCalendar) {
    mFoxInternalLoginBlockedUntil = pCalendar;
  }
}
