package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

public class ExInvalidThreadId
extends ExRuntimeRoot {
  static String TYPE = "Invalid FOX Thread Reference";

  public ExInvalidThreadId(String msg)  {
    super(msg, TYPE, null, null);
  }

  public ExInvalidThreadId(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }

  public ExInvalidThreadId(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }

  public ExInvalidThreadId(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }

  public ExInvalidThreadId(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
