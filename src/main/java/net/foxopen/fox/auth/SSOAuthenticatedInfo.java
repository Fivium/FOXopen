package net.foxopen.fox.auth;

import net.foxopen.fox.dom.DOM;

/**
 * AuthenticatedInfo Is a mess with header and LDAP specific code and should be split up into several objects. The
 * important part in that code is for building an AuthenticatedInfo DOM object which has been moved here for the SSO
 * login process.
 */
public class SSOAuthenticatedInfo {
  private final String mUID;
  private final String mLoginId;
  private final String mForename;
  private final String mSurname;
  private final String mPrimaryEmail;

  public SSOAuthenticatedInfo(String pUID, String pLoginId, String pForename, String pSurname, String pPrimaryEmail) {
    mUID = pUID;
    mLoginId = pLoginId;
    mForename = pForename;
    mSurname = pSurname;
    mPrimaryEmail = pPrimaryEmail;
  }

  /**
   * Constructs authenticated info in a standard format for SSO
   * @return DOM representation of info
   */
  public DOM getDOM() {
    DOM lDOM = DOM.createDocument("AUTHENTICATED_INFO");
    lDOM.addElem("UID", mUID);
    lDOM.addElem("LOGIN_ID", mLoginId);
    lDOM.addElem("FORENAME", mForename);
    lDOM.addElem("SURNAME", mSurname);
    lDOM.addElem("PRIMARY_EMAIL_ADDRESS", mPrimaryEmail);
    return lDOM;
  }

  /**
   * Get the Login ID
   * @return ID used for Login
   */
  public String getLoginId() {
    return mLoginId;
  }
}
