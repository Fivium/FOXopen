package net.foxopen.fox.command.flow;

import net.foxopen.fox.ex.ExInternal;

/**
 * Implementation of an XDoControlFlow for a thrown error, i.e. a result of the fm:throw command which was not an ignore
 * or break.
 */
public class XDoControlFlowError
extends XDoControlFlowThrownCode {
  
  private final Throwable mThrowable;
  
  public XDoControlFlowError(String pCode, String pMessage, Throwable pException){
    super(pCode, pMessage);    
    mThrowable = pException;
  }
  
  public void reThrow(){
    throw new ExInternal("Uncaught application error", mThrowable);
  }

}
