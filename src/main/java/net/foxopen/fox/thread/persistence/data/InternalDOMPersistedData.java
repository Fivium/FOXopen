package net.foxopen.fox.thread.persistence.data;

import net.foxopen.fox.dom.DOM;


public class InternalDOMPersistedData 
implements PersistedData {
  
  private final String mDocumentName;
  private final String mModuleCallId;  
  private final DOM mDOM;

  public InternalDOMPersistedData(String pModuleCallId, String pDocumentName, DOM pDOM) {
    mDocumentName = pDocumentName;
    mModuleCallId = pModuleCallId;
    mDOM = pDOM;
  }

  public String getDocumentName() {
    return mDocumentName;
  }

  public String getModuleCallId() {
    return mModuleCallId;
  }

  public DOM getDOM() {
    return mDOM;
  }
}
