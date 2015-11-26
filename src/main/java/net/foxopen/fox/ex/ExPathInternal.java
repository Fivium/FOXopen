package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

public class ExPathInternal extends ExRuntimeRoot {
  static String TYPE = "Fox Software Internal Path Error";
  public ExPathInternal(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExPathInternal(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExPathInternal(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExPathInternal(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExPathInternal(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
