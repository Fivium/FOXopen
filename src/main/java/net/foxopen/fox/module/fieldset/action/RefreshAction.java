package net.foxopen.fox.module.fieldset.action;

import java.util.Map;

import net.foxopen.fox.thread.ActionRequestContext;


public class RefreshAction implements InternalAction {

  private static RefreshAction INSTANCE = new RefreshAction();

  public static RefreshAction instance() {
    return INSTANCE;
  }

  private RefreshAction() {
  }

  @Override
  public void run(ActionRequestContext pRequestContext, Map<String, String> pParams) {
    //Refresh action does nothing
  }
}
