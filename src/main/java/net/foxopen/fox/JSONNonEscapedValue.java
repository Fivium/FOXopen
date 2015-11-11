package net.foxopen.fox;

import org.json.simple.JSONAware;

/**
 * Class to use with the simple JSON library FOX uses when you need to pass in a string but don't want the JSON library
 * to escape it when calling toJSONString()
 */
public class JSONNonEscapedValue implements JSONAware {
  private final String mValue;

  public JSONNonEscapedValue (String pValue) {
    mValue = pValue;
  }

  @Override
  public String toJSONString() {
    return mValue;
  }
}
