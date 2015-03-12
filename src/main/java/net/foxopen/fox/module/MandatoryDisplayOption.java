package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;

import java.util.HashMap;
import java.util.Map;

public enum MandatoryDisplayOption {
  BOTH("both"),
  MANDATORY("mandatory"),
  OPTIONAL("optional"),
  NONE("none");

  private static Map<String, MandatoryDisplayOption> DISPLAY_OPTION_LOOKUP_MAP = new HashMap<>(MandatoryDisplayOption.values().length);
  static {
    for (MandatoryDisplayOption lMandatoryDisplayOption : MandatoryDisplayOption.values()) {
      DISPLAY_OPTION_LOOKUP_MAP.put(lMandatoryDisplayOption.mAttributeValue, lMandatoryDisplayOption);
    }
  }

  private final String mAttributeValue;
  private MandatoryDisplayOption (String pAttributeValue) {
    mAttributeValue = pAttributeValue;
  }

  /**
   * Get a MandatoryDisplayOption for a given name, typically from an attribute value
   *
   * @param pMandatoryDisplayOptionName
   * @return MandatoryDisplayOption associated with pMandatoryDisplayOptionName, or MANDATORY by default
   */
  public static MandatoryDisplayOption fromString(String pMandatoryDisplayOptionName) {
    return XFUtil.nvl(DISPLAY_OPTION_LOOKUP_MAP.get(pMandatoryDisplayOptionName.toLowerCase()), MANDATORY);
  }
}
