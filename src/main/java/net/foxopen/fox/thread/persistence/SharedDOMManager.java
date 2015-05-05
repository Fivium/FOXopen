package net.foxopen.fox.thread.persistence;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Collections;

/**
 * A manager for DOMs which can be shared between threads. Each thread requests a local copy of the DOM from the manager.
 * The consumer is expected to track if the DOM has been modified and ask the manager to update the master copy when it is.<br/><br/>
 *
 * This class guarantees that the latest committed version of the DOM will always be provided to a consumer. In the case
 * of concurrent modification, a "last write wins" approach is taken. This eliminates the overheads of synchronization
 * and maintains thread safety, with the penalty of potential data loss. This is considered to be a minimal risk for the
 * current use cases of this class.<br/><br/>
 *
 * A SharedDOM has an identity composed of its DOM ID and DOM Type. It is important that only one SharedDOMManager exists
 * in a JVM for an individual SharedDOM - otherwise, overhead will be incurred as the seperate managers have to constantly
 * read the latest DOM from the database as they will be unaware of the updates being made by the other managers.
 */
public abstract class SharedDOMManager {

  public static enum SharedDOMType {
    SESSION,
    PREFS; //TODO PN implement or remove
  }

  //These two members provide a unique identity for the DOM
  private final String mDOMId;
  private final SharedDOMType mDOMType;

  /** The latest, "master" copy of the DOM. */
  private DOM mMasterDOM;
  /** The change number this object believes to be current. This is checked against the database on every read. */
  private String mLastReadChangeNumber;

  protected SharedDOMManager(String pDOMId, SharedDOMType pDOMType) {
    mDOMId = pDOMId;
    mDOMType = pDOMType;
  }

  /**
   * Creates a copy of the master DOM which the consuming thread is free to read from and write to without the risk of
   * concurrent modification. If the consumer requires updates to be persisted it should invoke {@link #updateDOM} when it
   * has finished modifying the DOM.
   * @param pRequestContext
   * @return A copy of the current DOM, or null if no persisted DOM exists.
   */
  public DOM getDOMCopy(RequestContext pRequestContext) {

    Track.pushDebug("GetSharedDOM", mDOMType.toString());
    try {
      synchronized(this) {
        DOMReadResult lReadResult = readDOMOrNull(pRequestContext, XFUtil.nvl(mLastReadChangeNumber, ""));
        if(lReadResult != null) {
          mMasterDOM = lReadResult.getDOM();
          mLastReadChangeNumber = lReadResult.getChangeNumber();
        }

        //Return a clone the current master DOM if one exists
        if(mMasterDOM != null) {
          return mMasterDOM.createDocument();
        }
        else {
          return null;
        }
      }
    }
    finally {
      Track.pop("GetSharedDOM");
    }
  }

  /**
   * Updates the master DOM for this Manager in the persistence layer (i.e. with an UPDATE statement). This should only
   * be invoked if the local DOM has changed since it was retrieved.
   * @param pRequestContext
   * @param pModifiedDOM Local DOM copy which was modified and needs to be persisted.
   */
  public void updateModifiedDOM(final ActionRequestContext pRequestContext, final DOM pModifiedDOM) {

    //Don't do the update immediately, as the target row may not exist yet. Instead add a Persistable to the persistence context
    //and do the update at the end of the persistence cycle.
    pRequestContext.getPersistenceContext().requiresPersisting(new Persistable() {
      @Override
      public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
        return null; //Doesn't need to do creates
      }

      @Override
      public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {
        Track.pushDebug("UpdateSharedDOM", mDOMType.toString());
        try {
          //Sync on the SharedDOMManager object - it is possible that two updates could happen concurrently
          synchronized (SharedDOMManager.this) {
            mLastReadChangeNumber = updateDOM(pPersistenceContext, pModifiedDOM);
            mMasterDOM = pModifiedDOM.createDocument();
          }
        }
        finally {
          Track.pop("UpdateSharedDOM");
        }

        return Collections.singleton(new PersistenceResult(this, PersistenceMethod.UPDATE));
      }

      @Override
      public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
        return null; //Doesn't need to do deletes
      }

      @Override
      public PersistableType getPersistableType() {
        return gePersistableType();
      }
    }, PersistenceMethod.UPDATE);
  }

  protected String getDOMId() {
    return mDOMId;
  }

  protected SharedDOMType getDOMType() {
    return mDOMType;
  }

  protected interface DOMReadResult {
    String getChangeNumber();
    DOM getDOM();
  }

  /**
   * Gets the DOMResult (DOM/change number) tuple from the persistence layer for the current DOM. This should return null
   * if the given change number matches the latest persisted change number.
   * @param pRequestContext
   * @param pCurrentChangeNumber Used for determining if the DOM has changed in the persistence layer.
   * @return
   */
  protected abstract DOMReadResult readDOMOrNull(RequestContext pRequestContext, String pCurrentChangeNumber);

  /**
   * Update the persisted DOM with the contents of the given DOM. This should cause the change number to be modified.
   * @param pPersistenceContext PersistenceContext being used to do serialisation
   * @param pDOM DOM to be written.
   * @return New change number.
   */
  protected abstract String updateDOM(PersistenceContext pPersistenceContext, DOM pDOM);

  /**
   * Gets the PersistableType which "update" type persistables are registered on the PersistenceContext as.
   * @return PersistableType.
   */
  protected abstract PersistableType gePersistableType();

}
