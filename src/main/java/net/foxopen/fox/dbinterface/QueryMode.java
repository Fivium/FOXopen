package net.foxopen.fox.dbinterface;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.ex.ExModule;


/**
 * Valid modes for a query execution. A subset (add-to and purge-all) also applies to APIs for legacy reasons.
 */
public enum QueryMode {
  ADD_TO("add-to"),
  PURGE_ALL("purge-all"),
  PURGE_SELECTED("purge-selected"),
  AUGMENT("augment");

  private final String mExternalString;

  private static Map<String, QueryMode> gExternalStringToType = new HashMap<>(4);
  static {
    for(QueryMode lType : values()) {
      gExternalStringToType.put(lType.mExternalString, lType);
    }
  }

  /**
   * Case-insensitive string lookup.
   * @param pExternalString String from module markup.
   * @return The corresponding query mode.
   * @throws ExModule If the string is not recognised.
   */
  public static QueryMode fromExternalString(String pExternalString)
  throws ExModule {
    QueryMode lMode = gExternalStringToType.get(pExternalString.toLowerCase());
    if(lMode != null) {
      return lMode;
    }
    else {
      throw new ExModule("Unrecognised query mode " + pExternalString);
    }
  }

  private QueryMode(String pExternalString) {
    mExternalString = pExternalString;
  }
}
