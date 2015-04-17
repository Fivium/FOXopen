package net.foxopen.fox.thread;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.ex.ExInternal;
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
 * The "action" may return an arbitrary object if the consumer requires this. Generics should be used to ensure type safety.
 *
 * @param <T> Object type to be returned from an action, if required.
 */
public class ThreadLockManager<T> {

  private final String mThreadId;
  private final String mConnectionName;

  public ThreadLockManager(String pThreadId, String pConnectionName) {
    mThreadId = pThreadId;
    mConnectionName = pConnectionName;
  }

  public void lockRampAndRun(RequestContext pRequestContext, final String pTrackActionType, final RampedThreadRunnable pRampedThreadRunnable) {
    lockAndPerformAction(pRequestContext, new LockedThreadRunnable<T>() {
      @Override
      public T doWhenLocked(RequestContext pRequestContext, StatefulXThread pXThread) {
        pXThread.rampAndRun(pRequestContext, pRampedThreadRunnable, pTrackActionType);
        return null;
      }
    });
  }

  public T lockRampAndRun(RequestContext pRequestContext, final String pTrackActionType, final RampedThreadRunnable pRampedThreadRunnable, final ThreadActionResultGenerator<? extends T> pActionResultGenerator) {
    return lockAndPerformAction(pRequestContext, new LockedThreadRunnable<T>() {
      @Override
      public T doWhenLocked(RequestContext pRequestContext, StatefulXThread pXThread) {
        return pXThread.rampAndRun(pRequestContext, pTrackActionType, pRampedThreadRunnable, pActionResultGenerator);
      }
    });
  }

  /**
   * Locks the required thread, runs the given action against it, then release the lock. Note the current UCon
   * will be committed by the thread lock/unlock action. A commit is also issued immediately after the action is run.
   * In the event of an exception the connection is rolled back, the thread is purged from cache and an attempt is made
   * to unlock the thread.
   *
   * @param pRequestContext Current RequestContext.
   * @param pLockedThreadRunnable Action to run on the locked thread.
   * @return An arbitrary object of the desired return type, as returned from the LockedThreadRunnable. This may be null.
   */
  public T lockAndPerformAction(RequestContext pRequestContext, LockedThreadRunnable<? extends T> pLockedThreadRunnable) {

    ContextUCon lContextUCon = pRequestContext.getContextUCon();

    StatefulXThread lXThread = null;
    T lResult;
    try {
      //Get the and lock the thread - THIS ISSUES A COMMIT
      lXThread = StatefulXThread.getAndLockXThread(pRequestContext, mThreadId);

      //Delegate to the consumer to perform whatever action is required with the locked thread
      lResult = pLockedThreadRunnable.doWhenLocked(pRequestContext, lXThread);

      //Validates all but MAIN transaction are committed
      lContextUCon.closeAllRetainedConnections();

      //Commit the MAIN connection - commits all work done by thread
      lContextUCon.commit(mConnectionName);

      //Now release the thread lock - THIS ISSUES ANOTHER COMMIT
      StatefulXThread.unlockThread(pRequestContext, lXThread);
    }
    catch (Throwable th) {
      //On any error, rollback contents of all connections, including main connection and release back to pool
      lContextUCon.rollbackAndCloseAll(true);

      try {
        //Unlock the thread if we managed to lock it
        if(lXThread != null) {
          //Ensure we always have a decent connection to unlock the thread
          lContextUCon.pushConnection(mConnectionName);
          StatefulXThread.unlockThread(pRequestContext, lXThread);
          lContextUCon.popConnection(mConnectionName);
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
     * etc will need to invoke rampAndRun on the XThread from within this method.
     *
     * @param pRequestContext Current RequestContext.
     * @param pXThread Locked XThread to perform actions on.
     * @return Optional result object of the desired return type.
     */
    T doWhenLocked(RequestContext pRequestContext, StatefulXThread pXThread);
  }
}
