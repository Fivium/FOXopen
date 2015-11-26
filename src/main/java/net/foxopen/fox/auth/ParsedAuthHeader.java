package net.foxopen.fox.auth;

import net.foxopen.fox.XFUtil;

public class ParsedAuthHeader {

  // Authentication type, e.g BASIC, DIGEST, see auth constants on HttpServletRequest
  public final String mAuthType;

  // Credentials supplied in headers
  /** Not null */
  public final String mUsername;
  /** Not null */
  public final String mPassword;

  public ParsedAuthHeader (String pAuthType, String pUsername, String pPassword) {
    mAuthType = pAuthType;
    mUsername = XFUtil.nvl(pUsername);
    mPassword = XFUtil.nvl(pPassword);
  }
}
