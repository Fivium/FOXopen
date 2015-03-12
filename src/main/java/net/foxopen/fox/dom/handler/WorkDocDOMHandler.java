package net.foxopen.fox.dom.handler;

import net.foxopen.fox.database.storage.dom.XMLWorkDoc;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;

public class WorkDocDOMHandler
implements PostableDOMHandler, AbortableDOMHandler {

  private final WorkingDataDOMStorageLocation mWorkingStoreLocation;
  private final String mContextLabel;

  private XMLWorkDoc mWorkDoc = null;

  public WorkDocDOMHandler(WorkingDataDOMStorageLocation pWorkingStoreLocation, String pContextLabel) {
    mWorkingStoreLocation = pWorkingStoreLocation;
    mContextLabel = pContextLabel;
  }

  @Override
  public void open(ActionRequestContext pRequestContext) {

    if(mWorkDoc == null) {
      //Bootstrap a WorkDoc JIT if we don't have a reference to one
      mWorkDoc = XMLWorkDoc.getOrCreateXMLWorkDoc(mWorkingStoreLocation, true);
    }

    mWorkDoc.open(pRequestContext.getContextUCon());
  }

  @Override
  public DOM getDOM() {
    return mWorkDoc.getDOM();
  }

  @Override
  public void close(ActionRequestContext pRequestContext) {
    mWorkDoc.close(pRequestContext.getContextUCon());
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
    //Only allow close/open if already open (was being called by a transaction join in a mapset do block after the handler had been closed)
    if(isOpen()) {
      close(pRequestContext);
      open(pRequestContext);
    }
  }

  @Override
  public void abort() {
    mWorkDoc.abort();
  }
}
