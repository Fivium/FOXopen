package net.foxopen.fox.entrypoint.auth.http;

import net.foxopen.fox.auth.ParsedAuthHeader;

/**
 * Actions for validating an attempt at Basic HTTP authentication.
 */
public interface HttpAuthenticationValidator {

  /**
   * Invoked by the BasicHttpAuthenticator to determine if a challenge should be issued in the event that authentication
   * fails (i.e. {@link #authenticate} returns false). If this method returns false, the user is sent to an "unauthorized" page
   * without a 401 challenge.
   * @return True if a 401 challenge should be issued following a failed authentication attempt.
   */
  public boolean isChallengeAllowed();

  /**
   * Run any actions required to validate a Basic HTTP Authentication request. Implementors can have side effects,
   * for instance creating/caching a session object. If this method returns true HTTP authentication is assumed to have
   * been a success. A 401 challenge will be issued if this method returns false and {@link #isChallengeAllowed} returns
   * true.
   * @param pParsedAuthHeader Username and password sent by the browser.
   * @return True if authentication succeeded.
   */
  public boolean authenticate(ParsedAuthHeader pParsedAuthHeader);

}
