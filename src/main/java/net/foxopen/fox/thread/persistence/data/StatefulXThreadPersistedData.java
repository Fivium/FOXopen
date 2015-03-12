package net.foxopen.fox.thread.persistence.data;

import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.thread.ThreadPropertyMap;


public class StatefulXThreadPersistedData
implements PersistedData {

  private final String mThreadId;
  private final String mAppMnem;
  private final String mUserThreadSessionId;

  private final ThreadPropertyMap mThreadPropertyMap;

  private final FieldSet mFieldSet;

  private final AuthenticationContext mAuthenticationContext;

  private final String mChangeNumber;

  private final String mFoxSessionID;

  public StatefulXThreadPersistedData(String pThreadId, String pAppMnem, String pThreadUserSessionId, ThreadPropertyMap pThreadPropertyMap, FieldSet pFieldSet,
                                      AuthenticationContext pAuthenticationContext, String pChangeNumber, String pFoxSessionID) {
    mThreadId = pThreadId;
    mAppMnem = pAppMnem;
    mUserThreadSessionId = pThreadUserSessionId;
    mThreadPropertyMap = pThreadPropertyMap;
    mFieldSet = pFieldSet;
    mAuthenticationContext = pAuthenticationContext;
    mChangeNumber = pChangeNumber;
    mFoxSessionID = pFoxSessionID;
  }

  public String getThreadId() {
    return mThreadId;
  }

  public String getAppMnem() {
    return mAppMnem;
  }

  public String getUserThreadSessionId() {
    return mUserThreadSessionId;
  }

  public ThreadPropertyMap getThreadPropertyMap() {
    return mThreadPropertyMap;
  }

  public FieldSet getFieldSet() {
    return mFieldSet;
  }

  public AuthenticationContext getAuthenticationContext() {
    return mAuthenticationContext;
  }

  public String getChangeNumber() {
    return mChangeNumber;
  }

  public String getFoxSessionID() {
    return mFoxSessionID;
  }
}
