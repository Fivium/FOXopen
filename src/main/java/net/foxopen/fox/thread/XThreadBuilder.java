package net.foxopen.fox.thread;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.persistence.DatabasePersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.util.RandomString;

public class XThreadBuilder {

  private static final int THREAD_ID_LENGTH = 100;

  private UserThreadSession mUserThreadSession;
  private final String mAppMnem;
  private final AuthenticationContext mAuthenticationContext;
  private ThreadPropertyMap mThreadPropertyMap = ThreadPropertyMap.createDefaultPropertyMap();
  private String mFoxSessionID;
  private boolean mDatabasePersistence = true;

  private static String generateNewThreadId() {
    return RandomString.getString(THREAD_ID_LENGTH);
  }

  public XThreadBuilder(String pAppMnem, AuthenticationContext pAuthenticationContext) {
    mAppMnem = pAppMnem;
    mAuthenticationContext = pAuthenticationContext;
  }

  public XThreadBuilder setUserThreadSession(UserThreadSession pUserThreadSession) {
    mUserThreadSession = pUserThreadSession;
    return this;
  }

  public XThreadBuilder setBooleanThreadProperty(ThreadProperty.Type pPropertyType, boolean pValue) {
    mThreadPropertyMap.setBooleanProperty(pPropertyType, pValue);
    return this;
  }

  public XThreadBuilder setStringThreadProperty(ThreadProperty.Type pPropertyType, String pValue) {
    mThreadPropertyMap.setStringProperty(pPropertyType, pValue);
    return this;
  }

  public XThreadBuilder setFoxSessionID(String pFoxSessionID) {
    mFoxSessionID = pFoxSessionID;
    return this;
  }

  public XThreadBuilder setDatabasePersistence(boolean pDatabasePersistence) {
    mDatabasePersistence = pDatabasePersistence;
    return this;
  }

  public StatefulXThread createXThread(RequestContext pRequestContext) {

    String lNewThreadId = generateNewThreadId();

    //Establish the persistence context to be used
    PersistenceContext lPersistenceContext;
    if(mDatabasePersistence) {
      lPersistenceContext = new DatabasePersistenceContext(lNewThreadId);
      //Setup the deserialiser now so it can grab a ContextUCon if it needs one during thread construction
      lPersistenceContext.setupDeserialiser(pRequestContext);
    }
    else {
      //TODO PN - alternative persistence methods
      throw new ExInternal("Database persistence must be enabled at this time");
    }

    //Module call stack - currently there are no configuration options for this
    ModuleCallStack lModuleCallStack = new ModuleCallStack();

    //By default, assume a new user thread session should be created for this thread
    UserThreadSession lUserThreadSession = mUserThreadSession;
    if(lUserThreadSession == null) {
      lUserThreadSession = UserThreadSession.createNewSession(lPersistenceContext);
    }

    //Default the session ID to whatever is on the request context if we've not been given one
    String lFoxSessionId = mFoxSessionID;
    if(XFUtil.isNull(lFoxSessionId)) {
      lFoxSessionId = pRequestContext.getFoxSession().getSessionId();
    }

    StatefulXThread lStatefulXThread = new StatefulXThread(pRequestContext, lUserThreadSession, mAppMnem, lNewThreadId, mAuthenticationContext, lPersistenceContext, mThreadPropertyMap, lModuleCallStack, lFoxSessionId);

    CacheManager.getCache(BuiltInCacheDefinition.STATEFUL_XTHREADS).put(lNewThreadId, lStatefulXThread);

    return lStatefulXThread;
  }
}
