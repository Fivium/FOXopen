package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.command.XDoResult;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.XThreadInterface;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.track.Track;

public class ModelessCall
extends CallStackTransformation {

  private final ModuleCall.Builder mModuleCallBuilder;
  private final boolean mSameSession;

  ModelessCall(ModuleCall.Builder pModuleCall, boolean pSameSession){
    mModuleCallBuilder = pModuleCall;
    mSameSession = pSameSession;
  }

  public void transform(ActionRequestContext pRequestContext, ModuleCallStack pCallStack) {

    XThreadInterface lCreateNewXThread = pRequestContext.createNewXThread(mModuleCallBuilder, mSameSession);

    pRequestContext.addXDoResult(new ModelessPopup(lCreateNewXThread, mModuleCallBuilder.getModelessWindowOptions()));
  }

  public static class ModelessPopup
  implements XDoResult {

    private final XThreadInterface mXThread;
    private final ModelessWindowOptions mModelessWindowOptions;

    private ModelessPopup(XThreadInterface pXThread, ModelessWindowOptions pModelessWindowOptions){
      mXThread = pXThread;

      if(pModelessWindowOptions == null){
        throw new IllegalArgumentException("Modeless window options cannot be null for modeless calls");
      }
      mModelessWindowOptions = pModelessWindowOptions;
    }

    public String getEntryURI(RequestURIBuilder pURIBuilder) {
      return mXThread.getEntryURI(pURIBuilder);
    }

    public String getWindowName() {
      return mModelessWindowOptions.getWindowName();
    }

    public String getWindowProperties() {
      return mModelessWindowOptions.getWindowProperties();
    }
  }

  @Override
  public void writeTrackData() {
    Track.addAttribute("type", "modeless");
    Track.addAttribute("same-session", Boolean.toString(mSameSession));
  }
}
