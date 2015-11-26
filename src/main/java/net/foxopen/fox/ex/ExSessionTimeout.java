package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExSessionTimeout extends ExGeneral
{
  static String TYPE = "Session Timeout";
  public ExSessionTimeout(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExSessionTimeout(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExSessionTimeout(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExSessionTimeout(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExSessionTimeout(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
