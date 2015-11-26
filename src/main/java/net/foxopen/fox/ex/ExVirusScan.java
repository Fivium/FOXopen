package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

public class ExVirusScan extends ExGeneral {

  static String TYPE = "Fox Virus Scan Error";
  
  public ExVirusScan(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExVirusScan(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExVirusScan(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExVirusScan(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExVirusScan(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
   
}
