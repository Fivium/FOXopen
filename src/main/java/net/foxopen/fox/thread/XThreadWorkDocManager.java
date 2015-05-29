package net.foxopen.fox.thread;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.database.storage.dom.XMLWorkDoc;
import net.foxopen.fox.ex.ExInternal;

/**
 * Class for managing opening/closing of WorkDocs within a churn. There are some edge cases where a WorkDoc may be opened
 * multiple times within a churn (e.g. when doing a call-module to a module which uses the same storage location). If this
 * happens it is important that FOX does not attempt to reopen a WorkDoc which is already open. This object tracks whether
 * the WorkDoc has been opened by the thread, and only calls open/close on the WorkDoc when it is appropriate to do so.<br><br>
 *
 * This is managed externally to the WorkDoc code itself, because most WorkDoc use cases do not need to account for this
 * behaviour. We cannot call #isOpen on the WorkDoc to determine its status, because that operation requires a lock which
 * we do not have at the point where we need it.<br><br>
 *
 * This object should only ever be associated with a single XThread.
 */
public class XThreadWorkDocManager {

  private final Multiset<String> mWorkDocOpenCounter = HashMultiset.create(4);

  XThreadWorkDocManager() { }

  /**
   * Opens the given WorkDoc if it is not already open by this object's XThread.
   * @param pContextUCon For opening WorkDoc.
   * @param pWorkDoc WorkDoc to open.
   */
  public void openIfRequired(ContextUCon pContextUCon, XMLWorkDoc pWorkDoc) {

    String lCacheKey = pWorkDoc.getCacheKey();

    if(mWorkDocOpenCounter.count(lCacheKey) == 0) {
      //No usages in the counter - this is first time this thread wants to open the WorkDoc, so actually open it
      pWorkDoc.open(pContextUCon);
    }

    //Record the open attempt so we can validate the stack against close attempts
    mWorkDocOpenCounter.add(lCacheKey);
  }

  /**
   * Closes the given WorkDoc if no usages are outstanding in this object's XThread.
   * @param pContextUCon For closing WorkDoc.
   * @param pWorkDoc WorkDoc to close.
   */
  public void closeIfRequired(ContextUCon pContextUCon, XMLWorkDoc pWorkDoc) {

    String lCacheKey = pWorkDoc.getCacheKey();

    //Remove one usage from the counter
    if(!mWorkDocOpenCounter.remove(lCacheKey)) {
      throw new ExInternal("Failed to validate close of WorkDoc - expected to remove a usage but register was empty for WorkDoc " + lCacheKey);
    }

    if(mWorkDocOpenCounter.count(lCacheKey) == 0) {
      //All usages have been accounted for, we can close the WorkDoc
      pWorkDoc.close(pContextUCon);
    }
  }

  /**
   * Aborts the given WorkDoc and cleans up this manager.
   * @param pWorkDoc WorkDoc to abort.
   */
  public void abort(XMLWorkDoc pWorkDoc) {
    String lCacheKey = pWorkDoc.getCacheKey();
    try {
      pWorkDoc.abort();
    }
    finally {
      //Clear the usage counter for this aborted WorkDoc
      mWorkDocOpenCounter.setCount(lCacheKey, 0);
    }
  }

}
