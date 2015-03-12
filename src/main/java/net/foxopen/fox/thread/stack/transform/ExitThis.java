package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.track.Track;

class ExitThis
extends ExitModule {

  private final boolean mRunCallbacks;

  ExitThis(boolean pRunCallbacks, String pExitURI){
    super(pExitURI);
    mRunCallbacks = pRunCallbacks;
  }

  @Override
  public void transform(ActionRequestContext pRequestContext, ModuleCallStack pCallStack) {
    pCallStack.pop(pRequestContext, mRunCallbacks, true, true, isAllowCancellationInAutoStateFinal());
  }

  @Override
  public void writeTrackData() {
    Track.addAttribute("type", "exit-this");
  }
}
