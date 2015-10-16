package net.foxopen.fox.module;

import java.util.Arrays;
import java.util.Optional;

public enum RenderTypeOption {
  FORM("form"),
  PRINT("print");

  private final String mStringValue;

  private RenderTypeOption(String lStringValue) {
    mStringValue = lStringValue;
  }

  public String getStringValue() {
    return mStringValue;
  }

  public static Optional<RenderTypeOption> fromString(String pStringValue) {
    return Arrays.asList(RenderTypeOption.values())
                 .stream()
                 .filter(pRenderTypeValue -> pRenderTypeValue.getStringValue().equals(pStringValue))
                 .findFirst();
  }
}
