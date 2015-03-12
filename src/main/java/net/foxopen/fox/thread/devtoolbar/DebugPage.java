package net.foxopen.fox.thread.devtoolbar;

import net.foxopen.fox.ex.ExInternal;

public enum DebugPage {
  MODULE,
  PARSE_TREE,
  MOD_MERGER,
  EXPANDED,
  MODEL,
  FIELD_SET,
  THREAD;

  static DebugPage fromRequestParam(String pRequestParam) {
    try {
      return valueOf(pRequestParam);
    }
    catch (IllegalArgumentException e) {
      throw new ExInternal("Invalid request: parameter " + pRequestParam + " is not a valid debug page type");
    }
  }

}
