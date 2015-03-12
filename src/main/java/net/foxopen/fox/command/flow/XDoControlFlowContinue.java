package net.foxopen.fox.command.flow;

/**
 * Implementation of an XDoControlFlow which allows commands to continue. This is the default result of a command.
 * This object has no state and should be used as a singleton.
 */
public class XDoControlFlowContinue 
implements XDoControlFlow {
  
  private static XDoControlFlowContinue INSTANCE = new XDoControlFlowContinue();
  
  public static XDoControlFlowContinue instance(){
    return INSTANCE;
  }
  
  protected XDoControlFlowContinue() {}

  public boolean canContinue() {
    return true;
  }
  
  @Override
  public boolean isCallStackTransformation() {
    return false;
  }
}
