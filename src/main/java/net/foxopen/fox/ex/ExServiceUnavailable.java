package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExServiceUnavailable extends ExRoot{
  static String TYPE = "Service Unavailable";
  public ExServiceUnavailable(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExServiceUnavailable(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExServiceUnavailable(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExServiceUnavailable(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExServiceUnavailable(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
