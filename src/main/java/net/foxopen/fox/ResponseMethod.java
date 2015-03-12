package net.foxopen.fox;

import net.foxopen.fox.ex.ExInternal;

import java.util.HashMap;
import java.util.Map;

/**
 * Methods of how FOX should respond to a module response. Streaming starts sending data out to the client as soon as
 * it's serialsed whereas buffered serialses the whole module and closes the thread before sending a response to the
 * client.
 */
public enum ResponseMethod {
  STREAMING("streaming"),
  BUFFERED("buffered");

  private static final Map<String, ResponseMethod> gExternalStringToResponseMethod = new HashMap<>(2);

  static {
    for(ResponseMethod lMode : values()) {
      gExternalStringToResponseMethod.put(lMode.mExternalString, lMode);
    }
  }

  public static ResponseMethod fromExternalString(String pExternalString) {
    ResponseMethod lResponseMethod = gExternalStringToResponseMethod.get(pExternalString);

    if(lResponseMethod == null) {
      throw new ExInternal("Unrecognised value for response-method: " + pExternalString);
    }

    return lResponseMethod;
  }

  private final String mExternalString;

  private ResponseMethod(String pExternalString) {
    mExternalString = pExternalString;
  }
}
