package net.foxopen.fox.command.flow;

/**
 * Implementation of an XDoControlFlow which causes all subsequent commands in the current action block to be skipped.
 * Commands in subsequent action blocks are allowed to run.
 */
public class XDoControlFlowIgnore 
extends XDoControlFlowThrownCode {
  
  public static final String IGNORE_CODE = "ACTIONIGNORE";
  
  public XDoControlFlowIgnore(String pMessage){
    super(IGNORE_CODE, pMessage);
  }
  
}
