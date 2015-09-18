package net.foxopen.fox.auth;

import net.foxopen.fox.FoxRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Static methods for shared authentication code.
 */
public class AuthUtil {

  public static final String X_FORWARDED_FOR_HEADER_NAME = "X-Forwarded-For";

  private AuthUtil() {}

  /**
   * Gets a set of name value pairs containing information about various request IP addresses.
   * E.g. "IP=127.0.0.1, REMOTE-ADDR=101.202.303.404, FORWARDED-FOR=1.2.3.4"
   * @param pFoxRequest Request to read IP information from.
   * @return Client IP info string, for logging purposes
   */
  public static String getClientInfoNVP(FoxRequest pFoxRequest) {

    String lClientInfo;
    try {
      lClientInfo = "IP="+ InetAddress.getLocalHost().getHostAddress();
    }
    catch (UnknownHostException e) {
      lClientInfo = "IP=unknown";
    }

    lClientInfo += ", REMOTE-ADDR=" + pFoxRequest.getHttpRequest().getRemoteAddr();

    String lForwardedForHeader = pFoxRequest.getHttpRequest().getHeader(X_FORWARDED_FOR_HEADER_NAME);
    if(lForwardedForHeader != null){
      lClientInfo += ", FORWARDED-FOR=" + lForwardedForHeader;
    }

    return lClientInfo;
  }
}
