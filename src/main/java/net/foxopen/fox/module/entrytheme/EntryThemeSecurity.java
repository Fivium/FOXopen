package net.foxopen.fox.module.entrytheme;

import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * A pre-entry security definition for an entry theme. These are run before the entry theme DO block to assert
 * that the user attempting access has the required privileges.
 */
public interface EntryThemeSecurity {

  /**
   * Evaluates the entry security rules based on the current user's privileges. If the user needs to be redirected to a
   * "permission denied" screen, a call stack transformation will be returned. This should be invoked immediately to prevent
   * further unauthorised access.
   * @param pRequestContext Current RequestContext.
   * @return A CST to redirect an invalid user, or a continue code to indicate success.
   */
  public XDoControlFlow evaluate(ActionRequestContext pRequestContext);

}
