package net.foxopen.fox.dom.handler;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.database.storage.dom.XMLWorkDoc;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.persistence.Persistable;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Collections;


/**
 * Handler for a thread DOM (theme, params, etc) which can be serialised as part of a module call.
 */
public class InternalDOMHandler
implements DOMHandler, Persistable {

  /** The name/context label for this DOM. */
  private final String mDocumentName;

  /** Module call which this DOM belongs to. */
  private final String mModuleCallId;

  /** The DOM for this handler. This reference should NOT be changed after it is initialised. */
  private DOM mDOM;

  /** DOM modified count when open was last called - used to track if the DOM has changed. */
  private int mModifiedCountAtOpen;

  /** Tracks if this DOM is 'new', i.e. will not have been initially persisted yet.
   * If true, the document will be persisted on close even if it wasn't modified (but was accessed) - used for new DOMs. */
  private boolean mIsNew = false;

  /** Tracks if the DOM has been accessed since it was opened. */
  private boolean mWasAccessed = false;

  /** Optional object for initialising the DOM from an arbitrary source on the first call to open() */
  private DOMInitialiser mDOMInitialiser;

  public static DOMHandler createHandlerForNewDOM(ContextLabel pContextLabel, String pModuleCallId) {
    return createHandlerForNewDOM(pContextLabel.asString(), pContextLabel.asString(), pModuleCallId);
  }

  public static DOMHandler createHandlerForNewDOM(String pContextLabel, String pDocumentRootName, String pModuleCallId) {
    return new InternalDOMHandler(pContextLabel, pModuleCallId, DOM.createDocument(pDocumentRootName), true, null);
  }

  public static DOMHandler createHandlerForWorkDoc(String pContextLabel, String pModuleCallId, XMLWorkDoc pWorkDoc) {
    return new InternalDOMHandler(pContextLabel, pModuleCallId, null, true, new WorkDocDOMInitialiser(pWorkDoc));
  }

  public static DOMHandler createHandlerForExistingDOM(ContextLabel pContextLabel, String pModuleCallId, DOM pDOM) {
    return createHandlerForExistingDOM(pContextLabel.asString(), pModuleCallId, pDOM);
  }

  public static DOMHandler createHandlerForExistingDOM(String pContextLabel, String pModuleCallId, DOM pDOM) {
    return new InternalDOMHandler(pContextLabel, pModuleCallId, pDOM, false, null);
  }

  protected InternalDOMHandler(String pDocumentName, String pModuleCallId, DOM pDOM, boolean pIsNew, DOMInitialiser pDOMInitialiser) {
    mDocumentName = pDocumentName;
    mModuleCallId = pModuleCallId;
    mDOM = pDOM;
    mIsNew = pIsNew;
    mDOMInitialiser = pDOMInitialiser;
  }

  private void initialise(ActionRequestContext pRequestContext) {
    if(mDOMInitialiser != null) {
      Track.debug("InitialiseInternalDOM", "Initialising internal DOM from " + mDOMInitialiser.getClass().getSimpleName() + " for " + mDocumentName);
      //Create a copy of the DOM from the initialiser - IMPORTANT - this must be a copy to avoid concurrency issues
      mDOM = mDOMInitialiser.getInitialDOM(pRequestContext).createDocument();
      //Null out the initialiser to mark that initialisation has been performed.
      mDOMInitialiser = null;
      //Force an initial serialise
      mWasAccessed = true;
    }
  }

  @Override
  public void open(ActionRequestContext pRequestContext) {
    initialise(pRequestContext);
    mDOM.getDocControl().setDocumentReadWriteAutoIds();
    mModifiedCountAtOpen = mDOM.getDocControl().getDocumentModifiedCount();
  }

  @Override
  public DOM getDOM() {
    mWasAccessed = true;
    return mDOM;
  }

  @Override
  public void close(ActionRequestContext pRequestContext) {
    //Serialise the DOM if it changed since open
    if(mDOM.getDocumentModifiedCount() != mModifiedCountAtOpen || (mIsNew && mWasAccessed)) {
      pRequestContext.getPersistenceContext().requiresPersisting(this, PersistenceMethod.UPDATE);
      //Reset the new flag so it's only forced once
      if(mIsNew) {
        mIsNew = false;
      }
    }
    //Prevent the document being modified after close
    mDOM.getDocControl().setDocumentReadOnly();
  }

  @Override
  public boolean isTransient(){
    return false;
  }

  @Override
  public String getContextLabel() {
    return mDocumentName;
  }

  public void setDOMContents(DOM pNewContents){
    mDOM.removeAllChildren();
    pNewContents.copyContentsTo(mDOM);
    //Mark as accessed in case this new contents needs to be serialised on close
    mWasAccessed = true;
  }


  @Override
  public int getLoadPrecedence() {
    return LOAD_PRECEDENCE_HIGH;
  }

  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().createInternalDOM(mModuleCallId, mDocumentName, mDOM);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.CREATE));
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().updateInternalDOM(mModuleCallId, mDocumentName, mDOM);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.UPDATE));
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.INTERNAL_DOM;
  }

  private interface DOMInitialiser {
    DOM getInitialDOM(ActionRequestContext pRequestContext);
  }

  private static class WorkDocDOMInitialiser
  implements DOMInitialiser {

    private final XMLWorkDoc mWorkDoc;

    WorkDocDOMInitialiser(XMLWorkDoc pWorkDoc) {
      mWorkDoc = pWorkDoc;
    }

    @Override
    public DOM getInitialDOM(ActionRequestContext pRequestContext) {
      mWorkDoc.open(pRequestContext.getContextUCon());
      try {
        return mWorkDoc.getDOM();
      }
      finally {
        mWorkDoc.close(pRequestContext.getContextUCon());
      }
    }
  }
}
