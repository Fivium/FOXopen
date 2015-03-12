package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

/**
 * Errors caused by parsing an XML DOM.
 */
public class ExDOMParser
extends ExInternal { //Would make more sense to extend ExDOM but originally the parser threw ExInternal so this avoids regression risk
  
  public ExDOMParser(String string, String string1, DOM dom, Throwable throwable) {
    super(string, string1, dom, throwable);
  }

  public ExDOMParser(String string, Throwable throwable) {
    super(string, throwable);
  }

  public ExDOMParser(String string) {
    super(string);
  }
}
