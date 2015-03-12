package net.foxopen.fox.database.storage;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.ex.ExDBTimeout;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;

/**
 * Encapsulation of the attempt lock... wait... repeat behaviour shared by writeable WorkDocs.
 */
public class WaitingRowSelector {

  private final int mLockAttempts;

  /**
   * Action which should be performed in order to acquire the lock and do any additional reading/setup based on the results
   * of the select statement.
   */
  public interface SelectAction {
    /**
     * Attempts to select the row. Implementors must throw ExDBTimeout if the select times out.
     * @param pUCon
     * @return True if 1 row was selected, or false if 0 rows were selected.
     * @throws ExDBTimeout
     */
    public boolean attemptSelect(UCon pUCon) throws ExDBTimeout;
  }

  public WaitingRowSelector(int pLockAttempts) {
    mLockAttempts = pLockAttempts;
  }

  /**
   * Attempts to lock a row from a select statement, retrying a defined number of times if the row is already locked.
   * The retry behaviour depends on the select statement containing the FOR UPDATE NOWAIT clause (if NOWAIT is not specified,
   * the read will hang until the lock is granted by Oracle). If the row remains locked after retrying, an exception is raised.
   * @param pUCon
   * @return True if a row was selected, false if not.
   */
  public boolean selectRow(UCon pUCon, SelectAction pSelectAction, String pDebugInfo) {
    //Open a locator to the target XMLType by running the select statement
    boolean lRowExists = false;

    ExDBTimeout lExDBTimeout = null;
    TRY_LOOP: for(int lTry=0; lTry < mLockAttempts; lTry++) {
      try {
        lRowExists = pSelectAction.attemptSelect(pUCon);
        return lRowExists;
      }
      catch (ExDBTimeout e) {
        lExDBTimeout = e;
        // proceed with next loop try
      }

      // Sleep - approx 1 second
      try {
        Track.info("ThreadSleep", "Sleep for 1 second in wait loop");
        Thread.sleep(1000);
      }
      catch(InterruptedException x) {
        throw new ExInternal("WaitingRowSelector: sleep was interrupted", x);
      }
    }

    //Loop ended without selecting anything
    throw new ExInternal("WaitingRowSelector: Row remains locked after " + mLockAttempts + " tries/seconds: " + pDebugInfo, lExDBTimeout);
  }
}
