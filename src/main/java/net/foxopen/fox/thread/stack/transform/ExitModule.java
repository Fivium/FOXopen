package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.URIResourceReference;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.thread.ResponseOverride;

abstract class ExitModule 
extends CallStackTransformation {  
  
  private final String mExitURI;  
  
  /** 
   * If true, an ACTIONBREAK/ACTIONIGNORE (interruption) or CallStackTransformation encountered in an auto-state-final 
   * will cause this exit to be cancelled.
   */
  private boolean mAllowCancellationInAutoStateFinal = false;  
  
  ExitModule(String pExitURI){
    mExitURI = pExitURI;
  }

  public ResponseOverride getExitResponseOverride() {
    if(!XFUtil.isNull(mExitURI)){
      return new ResponseOverride(new URIResourceReference(mExitURI));
    }
    else {
      return null;
    }
  }

  public void allowCancellationInAutoStateFinal() {
    mAllowCancellationInAutoStateFinal = true;
  }

  public boolean isAllowCancellationInAutoStateFinal() {
    return mAllowCancellationInAutoStateFinal;
  }
}
