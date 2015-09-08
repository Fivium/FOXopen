package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;

/**
 * An action name with optional state prefix, used to identify an action which may be in a different state. The state name
 * is only included in the identifier if it is required.
 */
public class ActionIdentifier {

  /** Optional state name, to be included if the action is in a different state */
  private final String mStateName;
  /** Name of action to run */
  private final String mActionName;

  /**
   * Creates a new ActionIdentifier.
   * @param pStateName Optional state name, to be included if the action is in a different state
   * @param pActionName Name of action to run
   */
  ActionIdentifier(String pStateName, String pActionName) {
    mStateName = pStateName;
    mActionName = pActionName;
  }

  /**
   * @return The identifying string for this action (i.e. action name with optional state prefix).
   */
  public String getIdentifier() {
    return (!XFUtil.isNull(mStateName) ? mStateName + "/" : "") + mActionName;
  }

  /**
   * @return The name of the action corresponding to this identifier, without any prefix.
   */
  public String getActionName() {
    return mActionName;
  }

}
