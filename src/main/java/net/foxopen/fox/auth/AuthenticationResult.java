package net.foxopen.fox.auth;

import net.foxopen.fox.ex.ExInternal;

public class AuthenticationResult {

  public enum Code {
    VALID(true),
    INVALID(false),
    GUEST(true),
    PASSWORD_EXPIRED(true), //we still have a session if even password is expired, so auth "succeeded"
    TFA_CHALLENGE(false),
    SUSPENDED(false),
    TFA_TOKEN_TIMEOUT(false);

    private final boolean mAuthenticationSucceeded;

    Code(boolean mAuthenticationSucceeded) {
      this.mAuthenticationSucceeded = mAuthenticationSucceeded;
    }

    /**
     * @return True if this Code represents a successful authentication attempt (note: "GUEST" and "PASSWORD_EXPIRED" are
     * considered successful). False implies the authentication attempt was invalid for some reason.
     */
    public boolean isAuthenticationSucceeded() {
      return mAuthenticationSucceeded;
    }

    public static Code fromString(String pCode) {
      if(pCode == null) {
        throw new ExInternal("Code cannot be null");
      }

      pCode = pCode.toUpperCase();

      switch (pCode) {
        case "VALID":
          return VALID;
        case "INVALID":
          return INVALID;
        case "CNGPASSWORD":
          return PASSWORD_EXPIRED;
        case "2FA_CHALLENGE":
          return TFA_CHALLENGE;
        case "SUSPENDED":
          return SUSPENDED;
        case "2FA_TOKEN_TIMEOUT":
          return TFA_TOKEN_TIMEOUT;
        default:
          throw new ExInternal("Unrecognised Auth code " + pCode);
      }
    }
  }

  private final Code mCode;
  private final String mMessage;
  private final String mSessionId;

  public AuthenticationResult(Code pCode, String pMessage, String pSessionId){
    mCode = pCode;
    mMessage = pMessage;
    mSessionId = pSessionId;
  }

  public Code getCode() {
    return mCode;
  }

  public String getMessage() {
    return mMessage;
  }

  public String getSessionId() {
    return mSessionId;
  }
}
