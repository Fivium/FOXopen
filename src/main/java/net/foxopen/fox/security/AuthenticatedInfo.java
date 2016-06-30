package net.foxopen.fox.security;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.SSOAuthenticatedInfo;
import net.foxopen.fox.dom.DOM;

import java.util.ArrayList;
import java.util.List;

/**
 * AuthenticatedInfo acts as a wrapper for information about an externally-authenticated
 * user coming in on a request.
 * Values for result members can be determined by the various methods supported such
 * as retrieval from HTTP headers or from an LDAP lookup result
 *
 * TODO - This code should be split up and shouldn't be so stateful, see SSOAuthenticatedInfo for a start
 */
public class AuthenticatedInfo {

  /**
   * LDAPConfig is a static nested class that wraps data related to an LDAP connection
   * and related configuration.
   */
  public static class LDAPConfig {

    /**
     * SeekConfig is an inner class wrapping data for a possible DN location containing
     * user information to seek. An LDAPConfig can contain more than 1 of these.
     */
    public class SeekConfig {
      public String mBaseDN;
      public String mSeekDNAttr;
      public String mUIDAttr;
      public String mLoginIdAttr;
      public String mForenameAttr;
      public String mSurnameAttr;
      public String mEmailAttr;
    }

    public String mHost;
    public int mPort;
    public String mBaseDN;
    public String mUserDNAttr;
    public String mPassword;
    public List mSeekConfigList = new ArrayList();

    /**
     * Creates a new SeekConfig scoped to this LDAP configuration
     * @return a new instance of SeekConfig
     */
    public SeekConfig getNewSeekConfig() {
      return new SeekConfig();
    }
  }

  public String mLoginIdHeader;
  public String mForenameHeader;
  public String mSurnameHeader;
  public String mPrimaryEmailHeader;

  private LDAPConfig mLDAPConfig;

  // Result members
  private String mUID;
  private String mLoginId;
  private String mForename;
  private String mSurname;
  private String mPrimaryEmail;

  public String getLoginId() {
    return mLoginId;
  }

  public LDAPConfig getLDAPConfig() {
    if(mLDAPConfig == null) {
      mLDAPConfig = new LDAPConfig();
    }
    return mLDAPConfig;
  }

  public void setLDAPConfig(LDAPConfig pLDAPConfig) {
    mLDAPConfig = pLDAPConfig;
  }

  /**
   * Constructs authenticated info in a standard format
   * @return DOM representation of info
   */
  public DOM getDOM() {
    SSOAuthenticatedInfo lSSOAuthenticatedInfo = new SSOAuthenticatedInfo(mUID, mLoginId, mForename, mSurname, mPrimaryEmail, null, null);
    return lSSOAuthenticatedInfo.getDOM();
  }

  /**
   * Reads a request for defined headers and stores their value on the object
   * @param pFoxRequest request to check for headers
   */
  public void processRequestHeaders(FoxRequest pFoxRequest) {
    mLoginId = XFUtil.exists(mLoginIdHeader) ? pFoxRequest.getHttpRequest().getHeader(mLoginIdHeader) : null;
    mForename = XFUtil.exists(mForenameHeader) ? pFoxRequest.getHttpRequest().getHeader(mForenameHeader) : null;
    mSurname = XFUtil.exists(mSurnameHeader) ? pFoxRequest.getHttpRequest().getHeader(mSurnameHeader) : null;
    mPrimaryEmail = XFUtil.exists(mPrimaryEmailHeader) ? pFoxRequest.getHttpRequest().getHeader(mPrimaryEmailHeader) : null;
  }

  /**
   * Takes a DOM result from an LDAP DN lookup and extracts info based on pre-defined attrs
   * @param pConfig the seek configuration used to find the result
   * @param pLDAPResult LDAP DN XML result
   */
  public void processLDAPResult(LDAPConfig.SeekConfig pConfig, DOM pLDAPResult) {
    mUID = pLDAPResult.get1SNoEx(pConfig.mUIDAttr);
    mLoginId = pLDAPResult.get1SNoEx(pConfig.mLoginIdAttr);
    mForename = pLDAPResult.get1SNoEx(pConfig.mForenameAttr);
    mSurname = pLDAPResult.get1SNoEx(pConfig.mSurnameAttr);
    mPrimaryEmail = pLDAPResult.get1SNoEx(pConfig.mEmailAttr);
  }
}
