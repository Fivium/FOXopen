package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

public class ExCache extends ExRuntimeRoot {
  static String TYPE = "Fox Internal Cache Error";
  public ExCache(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExCache(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExCache(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExCache(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExCache(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}

