package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExEvent extends ExGeneral
{
  static String TYPE = "Database Error";
  public ExEvent(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExEvent(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExEvent(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExEvent(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExEvent(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
