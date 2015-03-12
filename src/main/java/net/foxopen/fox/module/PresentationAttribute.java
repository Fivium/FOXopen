package net.foxopen.fox.module;

import net.foxopen.fox.dom.DOM;

import java.util.HashMap;
import java.util.Map;

/**
 * Store attribute values with a contextual DOM so that later evaluation is relative to the context DOM and an evaluatable
 * flag, typically set to false when attribute has not come from the node itself and instead a fixed value from the RM
 */
public class PresentationAttribute {
  private final String mValue;
  private final DOM mEvalContextRuleDOM;
  private final boolean mEvaluatableAttribute;

  public PresentationAttribute(String pAttributeValue, DOM pEvalContextRuleDOM, boolean pEvaluatableAttribute) {
    mValue = pAttributeValue;
    mEvalContextRuleDOM = pEvalContextRuleDOM;
    mEvaluatableAttribute = pEvaluatableAttribute;
  }

  /**
   * Constructor for known non-evaluatable attributes
   * @param pAttributeValue
   */
  public PresentationAttribute(String pAttributeValue) {
    mValue = pAttributeValue;
    mEvalContextRuleDOM = null;
    mEvaluatableAttribute = false;
  }

  public String getValue() {
    return mValue;
  }

  public DOM getEvalContextRuleDOM() {
    return mEvalContextRuleDOM;
  }

  public boolean isEvaluatableAttribute() {
    return mEvaluatableAttribute;
  }

  /**
   * Take a map of String attribute names to String attribute values and return a map of String attribute names
   * to Attribute objects, which store a reference to the context used to evaluate them.
   *
   * @param pAttributeMap Map&lt;String, String&gt; of attributes
   * @param pEvalContextRuleDOM DOM to evaluate attributes against
   * @param pEvaluatableAttribute If set to false these attributes will never be executed as XPaths
   * @return Map of String, Attribute objects
   */
  public static Map<String, PresentationAttribute> convertAttributeMap(Map<String, String> pAttributeMap, DOM pEvalContextRuleDOM, boolean pEvaluatableAttribute) {
    Map<String, PresentationAttribute> lConvertedMap = new HashMap<>(pAttributeMap.size());

    for (Map.Entry<String, String> lEntry : pAttributeMap.entrySet()) {
      lConvertedMap.put(lEntry.getKey(), new PresentationAttribute(lEntry.getValue(), pEvalContextRuleDOM, pEvaluatableAttribute));
    }

    return lConvertedMap;
  }
}
