package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.track.Track;

class ModalReplaceReturnToFirstCall extends ModalCall {

  public ModalReplaceReturnToFirstCall(ModuleCall.Builder pBuilder) {
    super(pBuilder);
  }

  @Override
  public void transform(ActionRequestContext pRequestContext, ModuleCallStack pCallStack) {

    String lSeekModuleName = mModuleCallBuilder.getEntryTheme().getModule().getName();
    String lSeekAppName = mModuleCallBuilder.getEntryTheme().getModule().getApp().getMnemonicName();

    //Get the current env DOM now before popping anything
    DOM lCurrentEnvDOM = pRequestContext.getContextUElem().getUElem(ContextLabel.ENV);

    int lCallIndex = -1;
    int i = 0;

    //Look for the lowest matching call in the stack
    for(ModuleCall lModuleCall : pCallStack){
      if(lSeekModuleName.equals(lModuleCall.getModule().getName()) && lSeekAppName.equals(lModuleCall.getModule().getApp().getMnemonicName())){
        //We found a matching module call, record its index
        lCallIndex = i;
      }
      i++;
    }

    //Pop calls off the stack until we reach the matching call index (or empty the stack if there's no matching call)
    for(i = 0; i < (lCallIndex != -1 ? lCallIndex : pCallStack.getStackSize()); i++) {
      pCallStack.pop(pRequestContext, false, false, false, false);
    }

    //If the call was not found on the stack, push the new module on
    if(lCallIndex == -1){
      //Do not allow callbacks to be defined on this call
      mModuleCallBuilder.clearCallbackHandlers();

      mModuleCallBuilder.setEnvironmentDOM(lCurrentEnvDOM);

      pCallStack.push(pRequestContext, mModuleCallBuilder);
    }
  }
  @Override
  public void writeTrackData() {
    Track.addAttribute("type", Type.MODAL_RETURN_TO_FIRST_OR_REPLACE_ALL_CANCEL_CALLBACKS.getExternalName());
  }
}
