package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

public class ExInternalConfiguration extends ExInternal {
  static String TYPE = "Fox Internal Configuration Error";
  
  public ExInternalConfiguration(String msg)  {
    super(msg, TYPE, null, null);
  }
  
  public ExInternalConfiguration(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  
  public ExInternalConfiguration(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  
  public ExInternalConfiguration(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  
  public ExInternalConfiguration(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
