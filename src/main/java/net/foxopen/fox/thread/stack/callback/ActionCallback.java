package net.foxopen.fox.thread.stack.callback;

import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCall;

public class ActionCallback
implements CallbackHandler {

  private final String mActionName;

  public ActionCallback(String pActionName) {
    mActionName = pActionName;
  }

  public void handleCallback(ModuleCall pExitedModuleCall, XDoRunner pXDoRunner, ActionRequestContext pRequestContext) {

    XDoCommandList lCallbackAction = pRequestContext.resolveActionName(mActionName);
    pXDoRunner.runCommands(pRequestContext, lCallbackAction);

  }
}
