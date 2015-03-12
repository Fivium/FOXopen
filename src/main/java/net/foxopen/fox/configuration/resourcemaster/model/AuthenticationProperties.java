package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.security.AuthenticatedInfo;
import net.foxopen.fox.security.AuthenticatedInfo.LDAPConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationProperties {

  /** A mapping of HTTP headers which trigger authentication bootstrapping and the objects that handle it */
  private final Map<String, AuthenticatedInfo> mAuthenticationBootstrapMap;
  private final String mAuthenticationBootstrapUserChange;

  /** Maps LDAP connection names to a map of attributes associated with that connection */
  private final Map<String, LDAPConfig> mLDAPConnectionMap;

  public static AuthenticationProperties createAuthenticationProperties(DOM pResourceMasterDOM) throws ExApp {
    AuthenticationProperties lAuthenticationProperties = new AuthenticationProperties(pResourceMasterDOM);
    return lAuthenticationProperties;
  }

  public AuthenticationProperties (DOM pAuthenticationDOM) throws ExApp {
    if (pAuthenticationDOM != null) {
      DOMList lAuthBootstraps = pAuthenticationDOM.getUL("authentication-bootstrap-list/authentication-bootstrap");
      DOM lBootstrapItem;
      String lKeyHeaderName;

      Map<String, AuthenticatedInfo> lAuthenticationInfo = new HashMap<>();
      for(int i = 0; i<lAuthBootstraps.getLength(); i++) {
        lBootstrapItem = lAuthBootstraps.item(i);
        AuthenticatedInfo lInfo = new AuthenticatedInfo();

        try {
          lKeyHeaderName = lBootstrapItem.get1S("key-header");
          lInfo.mLoginIdHeader = lBootstrapItem.get1S("login-id-header");
        }
        catch (ExCardinality eC) {
          throw new ExApp("Incorrect cardinality on authentication-bootstrap header configuration. You must specify one and only one key-header and login-id-header");
        }

        lInfo.mForenameHeader = lBootstrapItem.get1SNoEx("forename-header");
        lInfo.mSurnameHeader = lBootstrapItem.get1SNoEx("surname-header");
        lInfo.mPrimaryEmailHeader = lBootstrapItem.get1SNoEx("primary-email-header");
        lAuthenticationInfo.put(lKeyHeaderName, lInfo);
      }
      mAuthenticationBootstrapMap = Collections.unmodifiableMap(lAuthenticationInfo);
      mAuthenticationBootstrapUserChange = pAuthenticationDOM.get1SNoEx("authentication-bootstrap-user-change");


      // Load LDAP connections configurations
      DOMList lLDAPConnectionList = pAuthenticationDOM.getUL("ldap-connection-list/ldap-connection");
      Map<String, LDAPConfig> lLDAPConnectionMap = new HashMap<>();
      for(DOM lLDAPConnectionConfigDOM : lLDAPConnectionList) {
        String lConName = lLDAPConnectionConfigDOM.get1SNoEx("name");

        LDAPConfig lLDAPConfig = new LDAPConfig();
        lLDAPConfig.mHost = lLDAPConnectionConfigDOM.get1SNoEx("host");

        try {
          lLDAPConfig.mPort = Integer.parseInt(lLDAPConnectionConfigDOM.get1SNoEx("port"));
        }
        catch (NumberFormatException eNFE) {
          throw new ExApp("Bad port specified on LDAP configuration", eNFE);
        }

        lLDAPConfig.mBaseDN = lLDAPConnectionConfigDOM.get1SNoEx("base-dn");
        lLDAPConfig.mUserDNAttr = lLDAPConnectionConfigDOM.get1SNoEx("user-dn/attribute");
        lLDAPConfig.mPassword = lLDAPConnectionConfigDOM.get1SNoEx("password");

        DOMList lSeekConfigList = lLDAPConnectionConfigDOM.getUL("auth-info-list/auth-info");
        for(DOM lSeekConfigDOM : lSeekConfigList) {
          LDAPConfig.SeekConfig lSeekConfig = lLDAPConfig.getNewSeekConfig();
          lSeekConfig.mBaseDN = lSeekConfigDOM.get1SNoEx("source-dn/base-dn");
          lSeekConfig.mSeekDNAttr = lSeekConfigDOM.get1SNoEx("source-dn/attribute");
          lSeekConfig.mUIDAttr = lSeekConfigDOM.get1SNoEx("uid");
          lSeekConfig.mLoginIdAttr = lSeekConfigDOM.get1SNoEx("login-id");
          lSeekConfig.mForenameAttr = lSeekConfigDOM.get1SNoEx("forename");
          lSeekConfig.mSurnameAttr = lSeekConfigDOM.get1SNoEx("surname");
          lSeekConfig.mEmailAttr = lSeekConfigDOM.get1SNoEx("email");
          lLDAPConfig.mSeekConfigList.add(lSeekConfig);
        }

        lLDAPConnectionMap.put(lConName, lLDAPConfig);
      }
      mLDAPConnectionMap = Collections.unmodifiableMap(lLDAPConnectionMap);
    }
    else {
      mAuthenticationBootstrapUserChange = "";
      mAuthenticationBootstrapMap = Collections.emptyMap();

      mLDAPConnectionMap = Collections.emptyMap();
    }
  }

  public String getAuthenticationBootstrapUserChange() {
    return mAuthenticationBootstrapUserChange;
  }

  public Map<String, LDAPConfig> getLDAPConnectionMap() {
    return mLDAPConnectionMap;
  }

  public Map getAuthenticationBootstrapMap() {
    return mAuthenticationBootstrapMap;
  }
}
