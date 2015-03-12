package net.foxopen.fox.thread;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTimeout;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.sql.SQLManager;

import java.util.Date;


class StatefulXThreadLocker {

  //Forbid construction
  private StatefulXThreadLocker() {}

  private static final int SELECT_ATTEMPTS = 7;
  private static final String SELECT_THREAD_FOR_UPDATE_FILENAME = "SelectThreadForUpdate.sql";
  private static final String SWITCH_LOCK_THREAD_FILENAME = "ThreadLockSwitcher.sql";

  static String acquireLock(RequestContext pRequestContext, String pThreadId){

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Lock Thread " + pThreadId);
    try {
      Date lLockedSince = null;
      String lLockedBy = "unknown";

      for(int i=0; i < SELECT_ATTEMPTS; i++) {
        boolean lRowLocked = false;
        UConStatementResult lSelectResult = null;
        try {
          lSelectResult = lUCon.querySingleRow(SQLManager.instance().getStatement(SELECT_THREAD_FOR_UPDATE_FILENAME, StatefulXThreadLocker.class), pThreadId);

          lLockedBy = lSelectResult.getString("LOCKED_BY");
          lLockedSince = lSelectResult.getDate("LOCKED_SINCE_DATETIME");
        }
        catch (ExDBTimeout e) {
          lRowLocked = true;
        }
        catch (ExDB e) {
          throw new ExInternal("Unexpected error acquiring thread lock", e);
        }

        if(!lRowLocked && XFUtil.isNull(lLockedBy)) {
          //Row not physically locked and not marked as locked - grab a symbolic lock and return
          lock(lUCon, pThreadId);
          return lSelectResult.getString("CHANGE_NUMBER");
        }
        else {
          //Wait 1 sec and try again (rollback to release any lock)
          try {
            lUCon.rollback();
          }
          catch (ExDB e) {
            throw new ExInternal("Failed to lock thread " + pThreadId + " - error when rolling back", e);
          }

          Thread.sleep(1000);
        }
      }

      throw new ExInternal("Failed to lock thread " + pThreadId + " - lock remains after " + SELECT_ATTEMPTS + " lock attempts [locked since " + lLockedSince + " by " + lLockedBy + "]");
    }
    catch (InterruptedException e) {
      throw new ExInternal("Failed to lock thread " + pThreadId + " - interrupted waiting for lock", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Lock Thread " + pThreadId);
    }
  }

  private static void lock(UCon pUCon, String pThreadId) {
    try {
      pUCon.executeAPI(SQLManager.instance().getStatement(SWITCH_LOCK_THREAD_FILENAME, StatefulXThreadLocker.class), FoxGlobals.getInstance().getEngineLocator(), new Date(), pThreadId);
      pUCon.commit();
    }
    catch (ExDB | ExServiceUnavailable e) {
      throw new ExInternal("Failed to lock XThread", e);
    }
  }

  private static void unlock(UCon pUCon, String pThreadId) {
    try {
      pUCon.executeAPI(SQLManager.instance().getStatement(SWITCH_LOCK_THREAD_FILENAME, StatefulXThreadLocker.class), null, null, pThreadId);
      pUCon.commit();
    }
    catch (ExDB | ExServiceUnavailable e) {
      throw new ExInternal("Failed to unlock XThread", e);
    }
  }

  /**
   * NOTE: THIS COMMITS THE TOP UCON
   * @param pRequestContext
   * @param pThreadId
   */
  static void releaseLock(RequestContext pRequestContext, String pThreadId) {
    UCon lUCon = pRequestContext.getContextUCon().getUCon("Unlock Thread " + pThreadId);
    try {
      unlock(lUCon, pThreadId);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Unlock Thread " + pThreadId);
    }
  }
}
