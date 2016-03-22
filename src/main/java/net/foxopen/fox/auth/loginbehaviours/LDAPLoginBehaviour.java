package net.foxopen.fox.auth.loginbehaviours;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.io.LDAP;
import net.foxopen.fox.security.AuthenticatedInfo;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;

/**
 * Login using details obtained from an LDAP server. LDAP configuration comes from the Resource Master
 * (authentication-properties/ldap-connection-list)
 */
public class LDAPLoginBehaviour implements LoginBehaviour {
  private static final String AUTH_SCHEME_NAME = "LDAP";

  private final String mUsername;
  private final String mPassword;
  private final String mClientInfo;
  private final String mAuthDomain;

  public LDAPLoginBehaviour(String pUsername, String pPassword, String pClientInfo, String pAuthDomain) {
    mUsername = pUsername;
    mPassword = pPassword;
    mClientInfo = pClientInfo;
    mAuthDomain = pAuthDomain;
  }

  public static String getLoginBehaviourName() {
    return AUTH_SCHEME_NAME;
  }

  @Override
  public AuthenticationResult login(RequestContext pRequestContext) {
    // LDAP server and property lookup configuration is stored in the database as Authentication Properties under a FOX Environment
    AuthenticatedInfo.LDAPConfig lLDAPConfig = FoxGlobals.getInstance().getFoxEnvironment().getAuthenticationProperties().getLDAPConnectionMap().get(mAuthDomain);
    if (lLDAPConfig == null) {
      throw new ExInternal("No LDAP connection configuration with name \"" + mAuthDomain + "\" was found in the fox environment config");
    }

    AuthenticatedInfo lLDAPAuthenticatedInfo = new AuthenticatedInfo();
    lLDAPAuthenticatedInfo.setLDAPConfig(lLDAPConfig);

    // Construct user DN to attempt login with. The attribute specified for the connection is set
    // to the login id and the base DN is appended.
    String lUserDN = lLDAPConfig.mUserDNAttr + "=" + mUsername + "," + lLDAPConfig.mBaseDN;
    try {
      // Attempt login with supplied credentials
      String lHost = lLDAPConfig.mHost;
      lHost = !lHost.contains("ldap://") ? "ldap://" + lHost : lHost;
      // Default to port 389 if none specified
      lHost += ":"+ (lLDAPConfig.mPort != 0 ? lLDAPConfig.mPort : 389);
      LDAP lLDAPConnection = new LDAP(lHost, lUserDN, mPassword);

      // Connection obtained, loop through seek DN's trying to pull user information
      for (int i = 0; i < lLDAPConfig.mSeekConfigList.size(); i++) {
        AuthenticatedInfo.LDAPConfig.SeekConfig lSeek = (AuthenticatedInfo.LDAPConfig.SeekConfig)lLDAPConfig.mSeekConfigList.get(i);
        String lSourceDN = lSeek.mSeekDNAttr + "=" + mUsername + "," + lSeek.mBaseDN;
        try {
          DOM lLDAPResults = lLDAPConnection.getAttributesXMLFromDN(lSourceDN, null);
          lLDAPAuthenticatedInfo.processLDAPResult(lSeek, lLDAPResults);
        }
        catch (ExInternal ex) {
          // If we failed to find anything at this DN, proceed to the next one
          // or re-throw the parent exception if we have no others to try
          if (ex.getMessage().contains("No data found")) {
            if (i >= lLDAPConfig.mSeekConfigList.size()-1) {
              throw new ExInternal("Exhausted DN's in LDAP config without finding anything for: " + lSourceDN, ex);
            }
          }
          else {
            // Can't handle this error so re-throw, potentially a NamingException to catch outside
            throw ex;
          }
        }
      }

      // Call SSO login on database
      SSOLoginBehaviour lSSOLoginBehaviour = new SSOLoginBehaviour(mUsername, mClientInfo, mAuthDomain, AUTH_SCHEME_NAME, lLDAPAuthenticatedInfo.getDOM());
      return lSSOLoginBehaviour.login(pRequestContext);
    }
    catch (Exception e) {
      if (e.getMessage().contains("Invalid Credentials")) {
        return new AuthenticationResult(AuthenticationResult.Code.INVALID, "Invalid username or password", null);
      }
      else {
        // Add the exception to the error log despite suppressing it
        Track.recordSuppressedException("LDAPAuthError", e);
        // Add the serialised stack trace to Track to help developers identify the problem
        Track.logAlertText("LDAPAuthError", XFUtil.getJavaStackTraceInfo(e));
        // Return an invalid AuthenticationResult
        return new AuthenticationResult(AuthenticationResult.Code.INVALID, "An unexpected error has occurred", null);
      }
    }
  }
}
