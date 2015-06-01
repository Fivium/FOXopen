package net.foxopen.fox.dom.handler;

import net.foxopen.fox.database.storage.dom.XMLWorkDoc;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.XThreadWorkDocManager;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;

public class WorkDocDOMHandler
implements PostableDOMHandler, AbortableDOMHandler {

  private final WorkingDataDOMStorageLocation mWorkingStoreLocation;
  private final String mContextLabel;
  private final XThreadWorkDocManager mWorkDocManager;

  private XMLWorkDoc mWorkDoc = null;

  public WorkDocDOMHandler(WorkingDataDOMStorageLocation pWorkingStoreLocation, String pContextLabel, XThreadWorkDocManager pWorkDocManager) {
    mWorkingStoreLocation = pWorkingStoreLocation;
    mContextLabel = pContextLabel;
    mWorkDocManager = pWorkDocManager;
  }

  @Override
  public void open(ActionRequestContext pRequestContext) {

    if(mWorkDoc == null) {
      //Bootstrap a WorkDoc JIT if we don't have a reference to one
      mWorkDoc = XMLWorkDoc.getOrCreateXMLWorkDoc(mWorkingStoreLocation, true);
    }

    //Open the WorkDoc if it's not already open (accounts for multiple access attempt swithin a call stack transformation)
    mWorkDocManager.openIfRequired(pRequestContext.getContextUCon(), mWorkDoc);
  }

  @Override
  public DOM getDOM() {
    return mWorkDoc.getDOM();
  }

  @Override
  public void close(ActionRequestContext pRequestContext) {
    mWorkDocManager.closeIfRequired(pRequestContext.getContextUCon(), mWorkDoc);
  }

  @Override
  public boolean isTransient(){
    return true;
  }

  @Override
  public String getContextLabel() {
    return mContextLabel;
  }

  private boolean isOpen(){
    return mWorkDoc.isOpen();
  }

  @Override
  public int getLoadPrecedence() {
    return LOAD_PRECEDENCE_MEDIUM;
  }

  @Override
  public void postDOM(ActionRequestContext pRequestContext) {
    //Only allow posting if open (was being called by a transaction join in a mapset do block after the handler had been closed)
    if(isOpen()) {
      mWorkDoc.post(pRequestContext.getContextUCon());
    }
  }

  @Override
  public void abort() {
    //The manager needs to know about the abort
    mWorkDocManager.abort(mWorkDoc);
  }
}
