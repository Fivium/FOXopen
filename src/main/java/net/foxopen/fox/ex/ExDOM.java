package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

/**
 * Encapsulates Runtime Exceptions relating to Fox DOMs.
 */
public class ExDOM
extends ExRuntimeRoot {
  public ExDOM(String string, String string1, DOM dom, Throwable throwable) {
    super(string, string1, dom, throwable);
  }

  public ExDOM(String string, Throwable throwable) {
    super(string, throwable);
  }

  public ExDOM(String string) {
    super(string);
  }
}
