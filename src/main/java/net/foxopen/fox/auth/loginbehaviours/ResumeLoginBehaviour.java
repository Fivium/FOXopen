package net.foxopen.fox.auth.loginbehaviours;

import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.thread.RequestContext;

public interface ResumeLoginBehaviour {

  /**
   * A method which can be used to resume a partially completed login attempt (i.e. for 2FA)
   * @param pRequestContext Current RequestContext.
   * @return The status code and associated message of the login attempt.
   * Invalid login attempts include
   * INVALID (invalid hash code), SUSPENDED (account suspended after too many failed login attempts),
   * 2FA_TOKEN_TIMEOUT (login attempt ran out of time)
   * Valid login attempts include
   * VALID (correct code used to login), CNGPASSWORD (user's password has expired and needs to be changed before login)
   */
  AuthenticationResult resumeLogin(RequestContext pRequestContext);
}
