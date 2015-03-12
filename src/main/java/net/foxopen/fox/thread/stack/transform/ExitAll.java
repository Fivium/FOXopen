package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.track.Track;

class ExitAll
extends ExitModule {

  ExitAll(String pExitURI){
    super(pExitURI);
  }

  @Override
  public void transform(ActionRequestContext pRequestContext, ModuleCallStack pCallStack) {

    do {
      pCallStack.pop(pRequestContext, false, false, false, isAllowCancellationInAutoStateFinal());
    }
    while(pCallStack.getStackSize() > 0);

  }

  @Override
  public void writeTrackData() {
    Track.addAttribute("type", "exit-all");
  }
}
