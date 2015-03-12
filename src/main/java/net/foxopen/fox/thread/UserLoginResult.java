package net.foxopen.fox.thread;

import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.command.XDoResult;

/**
 * Evaluated data instructing an OutputGenerator to focus on a given element. See FocusCommand.
 */
public class UserLoginResult
implements XDoResult {

  private final AuthenticationResult mAuthenticationResult;

  public UserLoginResult(AuthenticationResult pAuthenticationResult) {
    mAuthenticationResult = pAuthenticationResult;
  }

  public boolean hasUserSuccessfullyLoggedIn() {
    return mAuthenticationResult.getCode() == AuthenticationResult.Code.VALID;
  }
}
