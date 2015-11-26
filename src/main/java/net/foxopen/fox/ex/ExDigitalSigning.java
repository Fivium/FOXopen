package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

public class ExDigitalSigning extends ExRuntimeRoot {
  static String TYPE = "Fox Software Digital Signing Error";
  public ExDigitalSigning(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExDigitalSigning(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExDigitalSigning(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExDigitalSigning(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExDigitalSigning(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
