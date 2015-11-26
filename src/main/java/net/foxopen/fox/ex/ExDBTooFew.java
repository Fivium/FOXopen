package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExDBTooFew extends ExDBCardinality
{
  static String TYPE = "Database Too Few Rows";
  public ExDBTooFew(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExDBTooFew(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExDBTooFew(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExDBTooFew(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExDBTooFew(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
