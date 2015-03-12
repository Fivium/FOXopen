package net.foxopen.fox.module.datanode;


import java.util.HashMap;
import java.util.Map;

/**
 * TODO - NP
 */
public enum NamespaceFunctionAttribute {
  EDIT("edit"),
  RO("ro"),
  RUN("run");

  private static Map<String, NamespaceFunctionAttribute> NAMESPACE_FUNCTION_ATTRIBUTE_LOOKUP_MAP = new HashMap<>(NamespaceFunctionAttribute.values().length);
  static {
    for (NamespaceFunctionAttribute lNamespaceFunctionAttribute : NamespaceFunctionAttribute.values()) {
      NAMESPACE_FUNCTION_ATTRIBUTE_LOOKUP_MAP.put(lNamespaceFunctionAttribute.mExternalString, lNamespaceFunctionAttribute);
    }
  }

  private final String mExternalString;

  private NamespaceFunctionAttribute(String pExternalString) {
    mExternalString = pExternalString;
  }

  /**
   * Get a NamespaceFunctionAttribute for a given name, typically from an attribute key
   *
   * @param pNamespaceFunctionAttributeName
   * @return NamespaceFunctionAttribute associated with pNamespaceFunctionAttributeName
   */
  public static NamespaceFunctionAttribute fromString(String pNamespaceFunctionAttributeName) {
    return NAMESPACE_FUNCTION_ATTRIBUTE_LOOKUP_MAP.get(pNamespaceFunctionAttributeName);
  }

  public String getExternalString() {
    return mExternalString;
  }
}
