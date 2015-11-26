package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExDBTimeout extends ExDB
{
  static String TYPE = "Database Timeout";
  public ExDBTimeout(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExDBTimeout(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExDBTimeout(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExDBTimeout(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExDBTimeout(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
