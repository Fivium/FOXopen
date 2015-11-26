package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExSecurity extends ExGeneral {
  static String TYPE = "Security violation";
  public ExSecurity(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExSecurity(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExSecurity(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExSecurity(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExSecurity(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
