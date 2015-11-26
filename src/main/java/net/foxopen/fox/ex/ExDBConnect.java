package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExDBConnect extends ExDB
{
  static String TYPE = "Database Connection Error";
  public ExDBConnect(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExDBConnect(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExDBConnect(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExDBConnect(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExDBConnect(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
