package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.track.Track;

class ModalReplaceAllCall
extends ModalCall {

  ModalReplaceAllCall(ModuleCall.Builder pBuilder) {
    super(pBuilder);
  }

  @Override
  public void transform(ActionRequestContext pRequestContext, ModuleCallStack pCallStack) {

    //Get the current env DOM now before popping anything
    mModuleCallBuilder.setEnvironmentDOM(pRequestContext.getContextUElem().getUElem(ContextLabel.ENV));

    do {
      pCallStack.pop(pRequestContext, false, false, false, false);
    }
    while(pCallStack.getStackSize() > 0);

    //Do not allow callbacks to be defined on this call
    mModuleCallBuilder.clearCallbackHandlers();

    pCallStack.push(pRequestContext, mModuleCallBuilder);
  }

  @Override
  public void writeTrackData() {
    Track.addAttribute("type", Type.MODAL_REPLACE_ALL_CANCEL_CALLBACKS.getExternalName());
  }
}
