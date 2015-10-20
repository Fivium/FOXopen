package net.foxopen.fox.module;

import net.foxopen.fox.ex.ExInternal;

/**
 * Common style types used by notifcations displayed to a user (info boxes, alerts, etc). Consumers should decide how
 * to externally serialise CSS class names etc for these enum values.
 */
public enum NotificationDisplayType {

  INFO,
  SUCCESS,
  WARNING,
  DANGER;

  /**
   * Resolves a NotificationDisplayType enum value from a case insensitive string comparison. If pExternalString is null
   * or invalid, an exception is thrown.
   * @param pExternalString String to resolve.
   * @return Resolved enum value.
   */
  public static NotificationDisplayType fromExternalString(String pExternalString) {
    try {
      return valueOf(pExternalString.toUpperCase());
    }
    catch (IllegalArgumentException e) {
      throw new ExInternal("Not a valid notification display type: '" + pExternalString + "'", e);
    }
  }
}
