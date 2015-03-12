package net.foxopen.fox.module.fieldset.action;

import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Map;

/**
 * Actions which are created by the engine rather than defined by a module developer (i.e. created internally). These
 * are top-level actions, usually invoked from a screen link/button.
 */
public interface InternalAction {

  public void run(ActionRequestContext pRequestContext,  Map<String, String> pParams) throws ExUserRequest;

}
