package net.foxopen.fox.module.fieldset.transformer;


/**
 * Determines if posted strings should be trimmed or not.
 */
public enum CleanOption {
  TRIM,
  NO_TRIM;

  public static CleanOption fromExternalString(String pExternalString) {
    if("no-trim".equals(pExternalString)) {
      return NO_TRIM;
    }
    else {
      return TRIM;
    }
  }
}
