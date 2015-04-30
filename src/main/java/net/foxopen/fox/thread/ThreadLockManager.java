package net.foxopen.fox.thread;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExInvalidThreadId;
import net.foxopen.fox.track.Track;

/**
 * Object which manages locking an XThread, running "actions" on it and closing the thread thereafter. Complexities regarding
 * transactional integrity - i.e. the need to commit a connection to lock and unlock a thread - are handled. Exceptions are
 * also handled and the manager always attempts to leave the thread in an unlocked state regardless of whether the action
 * was successful.<br><br>
 *
 * An "action" can either be a {@link RampedThreadRunnable} or a {@link LockedThreadRunnable}. The former allows access
 * to an {@link ActionRequestContext} but requires the thread to be fully ramped up before the action is invoked. The latter
 * does not ramp the thread but only allows operations against the thread in its unramped state - i.e. no access to DOMs.<br><br>
 *
 * The ThreadLockManager uses 1 or 2 connections depending on the pLockWithDedicatedConnection constructor parameter. Most
 * use cases only require a single "main" connection, which is more efficient. This can be used to lock the thread, run the action,
 * and unlock the thread, which involves committing the connection after each step. If you do not want the main connection to be
 * committed when the thread is locked/unlocked, you need to use the dedicated connection behaviour. The main connection is always
 * committed after running the action.<br><br>
 *
 * The "action" may return an arbitrary object if the consumer requires this. Generics should be used to ensure type safety.<br><br>
 *
 * Note: the thread ID is only validated when an action is run. If there is the possibility that the lock manager will be
 * given an invalid/stale thread ID, consumers should catch {@link ExInvalidThreadId} exceptions from the run methods.
 *
 * @param <T> Object type to be returned from an action, if required.
 */
public class ThreadLockManager<T> {

  private final String mThreadId;
  private final String mConnectionName;
  private final LockingBehaviour mLockingBehaviour;

  /**
   * Constructs a new ThreadLockManager for managing a single lock/action/unlock cycle.
   * @param pThreadId ID of XThread to lock.
   * @param pConnectionName Name of main connection, i.e. to top connection on the ContextUCon. This is committed after the
   *                        action is run.
   * @param pLockWithDedicatedConnection If true, a separate connection is used for locking and unlocking. Set this to true
   *                                     if you do not want the main connection to be committed immediately after the thread is locked.
   */
  public ThreadLockManager(String pThreadId, String pConnectionName, boolean pLockWithDedicatedConnection) {
    mThreadId = pThreadId;
    mConnectionName = pConnectionName;
    mLockingBehaviour = pLockWithDedicatedConnection ? new DedicatedConnectionLockingBehaviour() : new SharedConnectionLockingBehaviour();
  }

  public void lockRampAndRun(RequestContext pRequestContext, final String pTrackActionType, final RampedThreadRunnable pRampedThreadRunnable)
  throws ExInvalidThreadId {
    lockAndPerformAction(pRequestContext, new LockedThreadRunnable<T>() {
      @Override
      public T doWhenLocked(RequestContext pRequestContext, StatefulXThread pXThread) {
        pXThread.rampAndRun(pRequestContext, pRampedThreadRunnable, pTrackActionType);
        return null;
      }
    });
  }

  public T lockRampAndRun(RequestContext pRequestContext, final String pTrackActionType, final RampedThreadRunnable pRampedThreadRunnable, final ThreadActionResultGenerator<? extends T> pActionResultGenerator)
  throws ExInvalidThreadId {
    return lockAndPerformAction(pRequestContext, new LockedThreadRunnable<T>() {
      @Override
      public T doWhenLocked(RequestContext pRequestContext, StatefulXThread pXThread) {
        return pXThread.rampAndRun(pRequestContext, pTrackActionType, pRampedThreadRunnable, pActionResultGenerator);
      }
    });
  }

  /**
   * Locks the required thread, runs the given action against it, then releases the lock. The main connection is committed
   * immediately after the action is run. The locking connection is also committed before and after action processing. Note
   * that these may be the same connection depending on how the ThreadLockManager was set up.<br><br>
   *
   * In the event of an exception the main connection is rolled back, the thread is purged from cache and an attempt is made
   * to unlock the thread using a new connection.
   *
   * @param pRequestContext Current RequestContext.
   * @param pLockedThreadRunnable Action to run on the locked thread.
   * @return An arbitrary object of the desired return type, as returned from the LockedThreadRunnable. This may be null.
   * @throws ExInvalidThreadId If the thread ID given to this ThreadLockManager is not a valid ID.
   */
  public T lockAndPerformAction(RequestContext pRequestContext, LockedThreadRunnable<? extends T> pLockedThreadRunnable)
  throws ExInvalidThreadId {

    ContextUCon lContextUCon = pRequestContext.getContextUCon();

    StatefulXThread lXThread = null;
    T lResult;
    try {
      //Lock the thread - this will COMMIT for SharedConnection locking behaviour
      lXThread = mLockingBehaviour.lockXThread(pRequestContext);

      //Delegate to the consumer to perform whatever action is required with the locked thread
      lResult = pLockedThreadRunnable.doWhenLocked(pRequestContext, lXThread);

      //Validates all but MAIN transaction are committed
      lContextUCon.closeAllRetainedConnections();

      //Commit the MAIN connection - commits all work done by thread
      lContextUCon.commit(mConnectionName);

      //Now release the thread - this will COMMIT for SharedConnection locking behaviour
      mLockingBehaviour.unlockThread(pRequestContext, lXThread);
    }
    catch (ExInvalidThreadId e) {
      //Explicit re-throw of invalid thread ID exception (doesn't need to purge thread etc because no thread exists)
      lContextUCon.rollbackAndCloseAll(true);
      throw e;
    }
    catch (Throwable th) {
      //On any error, rollback contents of all connections, including main connection and release back to pool
      lContextUCon.rollbackAndCloseAll(true);

      try {
        //Unlock the thread if we managed to lock it
        if(lXThread != null) {
          //Thread should be unlocked on a new connection
          new DedicatedConnectionLockingBehaviour().unlockThread(pRequestContext, lXThread);
        }
      }
      catch (Throwable th2) {
        Track.recordSuppressedException("ErrorHandlerUnlockThread", th2);
        Track.alert("UnlockThread", "Error unlocking thread:" + th2.getMessage());
      }

      //Purge thread from cache to force restoration of state from the database on next churn
      StatefulXThread.purgeThreadFromCache(mThreadId);

      //Rethrow the handled error
      throw new ExInternal("Error processing request", th);
    }

    return lResult;
  }

  /**
   * Actions to run against a thread which has been locked by a ThreadLockManager.
   * @param <T> Object type to be returned.
   */
  public interface LockedThreadRunnable<T> {

    /**
     * Allows a consumer to implement behaviour to perform on a locked Thread. The thread will be unlocked by the ThreadLockManager
     * after this method is invoked. Note the thread's DOMs will not have been ramped up - implementations requiring DOM access
     * etc will need to invoke rampAndRun on the XThread from within this method. If a rampAndRun is all that is required, the
     * lockRampAndRun methods on ThreadLockManager are more appropriate.
     *
     * @param pRequestContext Current RequestContext.
     * @param pXThread Locked XThread to perform actions on.
     * @return Optional result object of the desired return type.
     */
    T doWhenLocked(RequestContext pRequestContext, StatefulXThread pXThread);
  }

  /** How to manage connections when locking/unlocking a thread */
  private interface LockingBehaviour {
    StatefulXThread lockXThread(RequestContext pRequestContext) throws ExInvalidThreadId;
    void unlockThread(RequestContext pRequestContext, StatefulXThread pXThread);
  }

  private class SharedConnectionLockingBehaviour implements LockingBehaviour {
    @Override
    public StatefulXThread lockXThread(RequestContext pRequestContext) throws ExInvalidThreadId {
      //Get the and lock the thread - THIS ISSUES A COMMIT
      return StatefulXThread.getAndLockXThread(pRequestContext, mThreadId);
    }

    @Override
    public void unlockThread(RequestContext pRequestContext, StatefulXThread pXThread) {
      //Now release the thread lock - THIS ISSUES A COMMIT
      StatefulXThread.unlockThread(pRequestContext, pXThread);
    }
  }

  private class DedicatedConnectionLockingBehaviour implements LockingBehaviour {
    public StatefulXThread lockXThread(RequestContext pRequestContext) throws ExInvalidThreadId {
      pRequestContext.getContextUCon().pushConnection("LOCK_THREAD");
      try {
        //Get the and lock the thread - THIS ISSUES A COMMIT
        return StatefulXThread.getAndLockXThread(pRequestContext, mThreadId);
      }
      finally {
        pRequestContext.getContextUCon().popConnection("LOCK_THREAD");
      }
    }

    public void unlockThread(RequestContext pRequestContext, StatefulXThread pXThread) {

      pRequestContext.getContextUCon().pushConnection("UNLOCK_THREAD");
      try {
        //Now release the thread lock - THIS ISSUES A COMMIT
        StatefulXThread.unlockThread(pRequestContext, pXThread);
      }
      finally {
        pRequestContext.getContextUCon().popConnection("UNLOCK_THREAD");
      }
    }
  }
}
