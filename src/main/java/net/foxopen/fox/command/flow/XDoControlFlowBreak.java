package net.foxopen.fox.command.flow;

/**
 * Implementation of an XDoControlFlow which causes all subsequent commands, in any action block, to be skipped.
 */
public class XDoControlFlowBreak 
extends XDoControlFlowThrownCode {
  
  public static final String BREAK_CODE = "ACTIONBREAK";
  
  public XDoControlFlowBreak(String pMessage){
    super(BREAK_CODE, pMessage);
  }

}
