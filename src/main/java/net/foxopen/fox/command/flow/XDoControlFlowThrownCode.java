package net.foxopen.fox.command.flow;

/**
 * Shared XDoControlFlow behaviour for all codes thrown by the fm:throw command.
 */
public abstract class XDoControlFlowThrownCode 
implements XDoControlFlow {  
  
  protected final String mCode;
  protected final String mMessage;
  
  public XDoControlFlowThrownCode(String pCode, String pMessage) {
    mCode = pCode;
    mMessage = pMessage;
  }

  public String getMessage() {
    return mMessage;
  } 

  public String getCode() {
    return mCode;
  }

  @Override
  public boolean canContinue() {
    return false;
  }
  
  @Override
  public boolean isCallStackTransformation() {
    return false;
  }
}
