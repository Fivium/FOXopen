package net.foxopen.fox.dom.handler;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.ThreadEventListener;
import net.foxopen.fox.thread.ThreadEventType;
import net.foxopen.fox.thread.persistence.SharedDOMManager;

public class SessionDOMHandler
implements DOMHandler, ThreadEventListener {

  //TODO PN - either refactor so this can be a "SharedDOMHandler" for session/prefs or properly javadoc

  private final SharedDOMManager mSharedDOMManager;

  private RequestContext mRequestContext;
  private int mInitialModifiedCount;
  private DOM mDOMCopy;

  public static SessionDOMHandler createHandler(SharedDOMManager pSharedDOMManager, StatefulXThread pXThread) {
    SessionDOMHandler lSessionDOMHandler = new SessionDOMHandler(pSharedDOMManager);
    pXThread.registerThreadEventListener(lSessionDOMHandler);
    return lSessionDOMHandler;
  }

  private SessionDOMHandler(SharedDOMManager pSharedDOMManager) {
    mSharedDOMManager = pSharedDOMManager;
  }

  @Override
  public void open(ActionRequestContext pRequestContext) {
  }

  @Override
  public void close(ActionRequestContext pRequestContext) {
    //If the DOM has been modified, tell the manager
    if(mDOMCopy != null && mDOMCopy.getDocumentModifiedCount() != mInitialModifiedCount) {
      mSharedDOMManager.updateModifiedDOM(pRequestContext, mDOMCopy);
    }
  }

  /**
   * Cleans up the temporary resources held by this Handler. This should be invoked at the end of a page churn.
   */
  private void cleanup() {
    //Note: we can't do this in close() because the DOM copy may be required after a close (i.e. in HTML gen)
    mDOMCopy = null;
    mRequestContext = null;
    mInitialModifiedCount = -1;
  }

  @Override
  public String getContextLabel() {
    return ContextLabel.SESSION.asString();
  }

  @Override
  public DOM getDOM() {
    if(mRequestContext == null) {
      throw new ExInternal("SessionDOMHandler cannot retrieve a DOM without a request context");
    }

    mDOMCopy = mSharedDOMManager.getDOMCopy(mRequestContext);
    if(mDOMCopy == null) {
      mDOMCopy = DOM.createDocument(ContextLabel.SESSION.asString());
    }

    mInitialModifiedCount = mDOMCopy.getDocumentModifiedCount();

    return mDOMCopy;
  }

  @Override
  public boolean isTransient() {
    return true;
  }

  @Override
  public int getLoadPrecedence() {
    return LOAD_PRECEDENCE_LOW;
  }

  @Override
  public void handleThreadEvent(ActionRequestContext pRequestContext, ThreadEventType pEventType) {

    if(pEventType == ThreadEventType.START_REQUEST_PROCESSING) {
      //Keep a reference to the RequestContext for the duration of the churn, so we can do JIT retrieval if we need
      //Do this here instead of in open() so we can always retrieve a DOM even if the handler is not "open" (i.e. for HTML generation during a thread resume)
      mRequestContext = pRequestContext;
    }
    if(pEventType == ThreadEventType.FINISH_REQUEST_PROCESSING) {
      //Tell session DOM handler to release its churn-specific resources (i.e. its DOM copy)
      cleanup();
    }
  }
}
