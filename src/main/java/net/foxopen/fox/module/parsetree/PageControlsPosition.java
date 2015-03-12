package net.foxopen.fox.module.parsetree;

import net.foxopen.fox.ex.ExInternal;

public enum PageControlsPosition {
  ABOVE,
  BELOW,
  BOTH,
  NONE;

  public static PageControlsPosition fromString(String pAttributeValue) {
    try {
      return valueOf(pAttributeValue.toUpperCase());
    }
    catch (IllegalArgumentException e) {
      throw new ExInternal(pAttributeValue + " is not a valid value for page-controls-position attribute");
    }
  }

  public boolean isAbove() {
    return this == ABOVE || this == BOTH;
  }

  public boolean isBelow() {
    return this == BELOW || this == BOTH;
  }
}