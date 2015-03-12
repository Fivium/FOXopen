package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.thread.stack.callback.CallbackHandler;
import net.foxopen.fox.track.Track;

class ModalReplaceThisCall
extends ModalCall {

  static enum CallbackOption {
    CALLBACKS_NOW, NO_CALLBACKS, PRESERVE_CALLBACKS;
  }

  private final CallbackOption mCallbackOption;

  ModalReplaceThisCall(ModuleCall.Builder pBuilder, CallbackOption pCallbackOption) {
    super(pBuilder);
    mCallbackOption = pCallbackOption;
  }

  public void transform(ActionRequestContext pRequestContext, ModuleCallStack pCallStack) {

    ModuleCall lLastCall = pCallStack.getTopModuleCall();
    boolean lRunCallbacks = mCallbackOption == CallbackOption.CALLBACKS_NOW;

    //Get the current env DOM now before popping anything
    mModuleCallBuilder.setEnvironmentDOM(pRequestContext.getContextUElem().getUElem(ContextLabel.ENV));

    //Pop the first module off the top of the stack
    pCallStack.pop(pRequestContext, lRunCallbacks, true, false, false);

    if(mCallbackOption == CallbackOption.PRESERVE_CALLBACKS){

      //Wipe out any requested callbacks and copy callbacks from the popped module call to the new module call
      mModuleCallBuilder.clearCallbackHandlers();
      for(CallbackHandler lCallbackHandler : lLastCall.getCallbackHandlerList()){
        mModuleCallBuilder.addCallbackHandler(lCallbackHandler);
      }
    }
    else {
      //Do not allow callbacks to be defined on this call
      mModuleCallBuilder.clearCallbackHandlers();
    }

    pCallStack.push(pRequestContext, mModuleCallBuilder);
  }

  @Override
  public void writeTrackData() {
    Track.addAttribute("type", "replace-this");
    Track.addAttribute("callback-option", mCallbackOption.toString());
  }
}
