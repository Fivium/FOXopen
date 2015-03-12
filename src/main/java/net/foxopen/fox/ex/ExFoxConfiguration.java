package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

public class ExFoxConfiguration extends ExGeneral {
  static String TYPE = "Fox Configuration Error";
  
  public ExFoxConfiguration(String msg)  {
    super(msg, TYPE, null, null);
  }
  
  public ExFoxConfiguration(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  
  public ExFoxConfiguration(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  
  public ExFoxConfiguration(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  
  public ExFoxConfiguration(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
