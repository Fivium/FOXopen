package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;

/**
 * A setout action's ActionType determines how it is invoked by the browser (i.e. form post, direct link...)
 */
public enum ActionType {
  /** Actions which are run in a standard churn, i.e. a form POST */
  CHURN,
  /** Actions which are run by clicking a download link. */
  DOWNLOAD;

  /**
   * Gets the action type as defined in an attribute by the developer. For null values the default is "CHURN". Unknown
   * values throw an exception.
   * @param pExternalString
   * @return
   */
  public static ActionType fromExternalString(String pExternalString) {

    if(XFUtil.isNull(pExternalString)) {
      pExternalString = "churn";
    }

    switch (pExternalString.toLowerCase()) {
      case "download":
        return DOWNLOAD;
      case "churn":
        return CHURN;
      default:
        throw new ExInternal("Unknown actionType " + pExternalString);
    }
  }
}
