package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExDB extends ExGeneral
{
  static String TYPE = "Database Error";
  public ExDB(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExDB(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExDB(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExDB(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExDB(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
