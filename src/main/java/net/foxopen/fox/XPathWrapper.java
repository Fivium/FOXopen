package net.foxopen.fox;

import java.util.EnumSet;
import java.util.Set;

/**
 * XPathWrappers are used by module developers to indicate that an argument should be interpreted as an XPath and executed
 * accordingly. I.e. for the fox:prompt attribute, the argument "Enter name" should return as is, but "string(/X/Y/text())"
 * should invoke an XPath evaluation. The different values of this enum allow for the XPath's behaviour to be further refined
 * (in legacy FOX, only "string()" was allowed).
 */
public enum XPathWrapper {
  STRING("string"),
  NUMBER("number"),
  UNESCAPED_STRING("unescaped-string"),
  NO_WRAPPER(null);

  /** Wrapper names which are allowed to appear in XPaths (i.e. don't allow internal ones like NO_WRAPPER) */
  private static final Set<XPathWrapper> ALLOWED_EXTERNAL_WRAPPERS = EnumSet.of(STRING, NUMBER, UNESCAPED_STRING);

  /** How this wrapper is referenced in an XPath (i.e. the function name) */
  private final String mExternalName;

  private XPathWrapper(String pExternalName) {
    mExternalName = pExternalName;
  }

  static XPathWrapper getWrapperForXPathString(String pStringOrExtendedXPath) {
    for (XPathWrapper lWrapper : ALLOWED_EXTERNAL_WRAPPERS) {
      if (pStringOrExtendedXPath.startsWith(lWrapper.mExternalName + "(")) {
        return lWrapper;
      }
    }

    return NO_WRAPPER;
  }

  public String getExternalName() {
    return mExternalName;
  }
}
