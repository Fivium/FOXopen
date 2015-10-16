package net.foxopen.fox.module;

import java.util.Arrays;

public enum LayoutDirection {
  NORTH("north"),
  EAST("east"),
  SOUTH("south"),
  WEST("west");

  private final String mStringValue;

  LayoutDirection(String lStringValue) {
    mStringValue = lStringValue;
  }

  public String getStringValue() {
    return mStringValue;
  }

  public static LayoutDirection fromString(String pStringValue) {
    return Arrays.asList(LayoutDirection.values())
                 .stream()
                 .filter(pPromptLayoutValue -> pPromptLayoutValue.getStringValue().equals(pStringValue))
                 .findFirst()
                 .orElse(null);
  }
}
