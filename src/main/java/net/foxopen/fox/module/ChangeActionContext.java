package net.foxopen.fox.module;


public class ChangeActionContext extends ActionContext {
  
  private final String mItemContextRef;

  public ChangeActionContext(String pActionName, String pActionContextRef, String pItemContextRef) {
    super(pActionName, pActionContextRef);
    mItemContextRef = pItemContextRef;
  }

  public String getItemContextRef() {
    return mItemContextRef;
  }
}
