package net.foxopen.fox.command.flow;

import net.foxopen.fox.thread.stack.transform.CallStackTransformation;

/**
 * Implementation of an XDoControlFlow which represents a call stack transformation (CST), typically invoked by an fm:call-module
 * or fm:exit-module command. Typically commands are not permitted to run after a CST has been hit.
 */
public class XDoControlFlowCST 
implements XDoControlFlow {  
  
  private final CallStackTransformation mCallStackTransformation;
  
  public XDoControlFlowCST(CallStackTransformation pCallStackTransformation) {
    mCallStackTransformation = pCallStackTransformation;
  }

  public boolean canContinue() {
    return false;
  }

  public CallStackTransformation getCallStackTransformation() {
    return mCallStackTransformation;
  }

  @Override
  public boolean isCallStackTransformation() {
    return true;
  }
}
