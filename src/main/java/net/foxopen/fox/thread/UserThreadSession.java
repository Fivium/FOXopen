package net.foxopen.fox.thread;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.thread.persistence.Persistable;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;
import net.foxopen.fox.thread.persistence.SharedDOMManager;
import net.foxopen.fox.thread.persistence.SharedDOMManager.SharedDOMType;


/**
 * A UserThreadSession (formally an "xfsession") is a session belonging to at least one XThread. Currently it is used to
 * hold the :{session} DOM. FOX developers have control over the session in use when performing a modeless module call,
 * which offers a choice between same and new session.
 */
public class UserThreadSession
implements Persistable {
  
  private static final Iterator<String> gChangeNumberIterator = XFUtil.getUniqueIterator();
  
  /** Unique ID for this session (a table PK) */
  private final String mSessionId;
  /** Manager for the session DOM. */
  private final SharedDOMManager mSharedDOMManager;
  
  private UserThreadSession(String pSessionId, PersistenceContext pPersistenceContext) {
    mSessionId = pSessionId;
    mSharedDOMManager = pPersistenceContext.getSharedDOMManager(SharedDOMType.SESSION, mSessionId);
  }

  /**
   * Bootstraps a new UserThreadSession.
   * @param pPersistenceContext For initial serialisation - an XThread depends on its UserThreadSession being serialised first
   * so they must be serialised in the same persistence cycle.
   * @return New UserThreadSession.
   */
  public static UserThreadSession createNewSession(PersistenceContext pPersistenceContext) {
    UserThreadSession lNewSession = new UserThreadSession(gChangeNumberIterator.next(), pPersistenceContext);
    pPersistenceContext.requiresPersisting(lNewSession, PersistenceMethod.CREATE);
    return lNewSession;
  }
  
  /**
   * Gets an existing UserThreadSession. This does not read anything from the PersistenceContext - it is the responsibility of
   * the consumer to ensure the given session ID corresponds to a valid session. This method constructs a new object, meaning
   * it is possible that multiple objects representing the same session will exist within the same JVM.
   * @param pSessionId ID of the existing session.
   * @param pPersistenceContext For determining which SharedDOMManager to use for the session DOM.
   * @return Object representing an existing session.
   */
  public static UserThreadSession getExistingSession(String pSessionId, PersistenceContext pPersistenceContext) {
    return new UserThreadSession(pSessionId, pPersistenceContext);
  }
  
  public String getSessionId() {
    return mSessionId;
  }

  public SharedDOMManager getSessionDOMManager() {
    return mSharedDOMManager;
  }

  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().createUserThreadSession(mSessionId);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.CREATE));
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {
    //No need to implement updating yet - nothing to update
    return Collections.emptySet();
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    //No need to implement deleting yet - nothing to delete
    return Collections.emptySet();
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.USER_THREAD_SESSION;
  }
}
