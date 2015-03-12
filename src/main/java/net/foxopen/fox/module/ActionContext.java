package net.foxopen.fox.module;


/**
 * Store the contextual information for an action for storage on a fieldset
 */
public class ActionContext {
  private final String mActionName;
  private final String mActionContextRef;

  public ActionContext(String pActionName, String pActionContext) {
    mActionName = pActionName;
    this.mActionContextRef = pActionContext;
  }

  public String getActionName() {
    return mActionName;
  }

  public String getActionContextRef() {
    return this.mActionContextRef;
  }

}
