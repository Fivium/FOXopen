package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.track.Track;

class ModalCall
extends CallStackTransformation {

  protected final ModuleCall.Builder mModuleCallBuilder;

  ModalCall(ModuleCall.Builder pModuleCall){
    mModuleCallBuilder = pModuleCall;
  }

  public void transform(ActionRequestContext pRequestContext, ModuleCallStack pCallStack) {

    mModuleCallBuilder.setEnvironmentDOM(pRequestContext.getContextUElem().getUElem(ContextLabel.ENV));

    pCallStack.push(pRequestContext, mModuleCallBuilder);
  }

  @Override
  public void writeTrackData() {
    Track.addAttribute("type", Type.MODAL.getExternalName());
  }
}
