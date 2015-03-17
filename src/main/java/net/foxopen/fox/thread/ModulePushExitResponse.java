package net.foxopen.fox.thread;

import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.thread.stack.ModuleCall;

/**
 * An exit response which prevents the current thread from being exited and instead pushes a module call into the empty
 * call stack.
 */
public class ModulePushExitResponse
implements ExitResponse {

  /** Module name to push, may also contain an entry theme name following a slash, i.e. BPM001X/new */
  private final String mModuleName;

  public ModulePushExitResponse(String pModuleName) {
    mModuleName = pModuleName;
  }

  public ModuleCall.Builder createBuilder(ActionRequestContext pRequestContext) {

    EntryTheme lEntryTheme;
    try {
      if(mModuleName.contains("/")) {
        //Slash in name indicates entry theme has been specified
        String lModName = mModuleName.split("/")[0];
        String lEntryThemeName = mModuleName.split("/")[1];
        lEntryTheme = pRequestContext.getRequestApp().getMod(lModName).getEntryTheme(lEntryThemeName);
      }
      else {
        lEntryTheme = pRequestContext.getRequestApp().getMod(mModuleName).getDefaultEntryTheme();
      }
    }
    catch (ExUserRequest | ExServiceUnavailable | ExModule | ExApp e) {
      throw new ExInternal("Failed to resolve module for ModulePushExitResponse", e);
    }

    return new ModuleCall.Builder(lEntryTheme);
  }
}
