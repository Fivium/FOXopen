package net.foxopen.fox.command.flow;

/**
 * Result object which is used to co-ordinate control flow when executing commands. Implementations may cause subsequent
 * command execution to be prevented or altered - see XDoCommandRunner.
 */
public interface XDoControlFlow {
  
  /**
   * Tests if command execution is allowed to continue.
   * @return True if command execution can continue.
   */
  public boolean canContinue();
  
  /**
   * Tests if this XDoControlFlow represents a call stack transformation.
   * @return
   */
  public boolean isCallStackTransformation();
  
}
