package net.foxopen.fox.auth;

import net.foxopen.fox.ex.ExInternal;

public class AuthenticationResult {

  public static enum Code {
    VALID,
    INVALID,
    GUEST,
    PASSWORD_EXPIRED;

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
