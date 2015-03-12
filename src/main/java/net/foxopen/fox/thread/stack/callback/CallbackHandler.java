package net.foxopen.fox.thread.stack.callback;

import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCall;

public interface CallbackHandler {

  //TODO note in doco this is a MOUNTED module call
  //TODO should this not be just a contextuelem?
  public void handleCallback(ModuleCall pExitedModuleCall, XDoRunner pXDoRunner, ActionRequestContext pRequestContext);

}
