package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExDBTooMany extends ExDBCardinality
{
  static String TYPE = "Database Too Many Rows";
  public ExDBTooMany(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExDBTooMany(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExDBTooMany(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExDBTooMany(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExDBTooMany(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
