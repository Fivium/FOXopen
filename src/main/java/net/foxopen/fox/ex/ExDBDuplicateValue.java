package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExDBDuplicateValue extends ExDBSyntax
{
  static String TYPE = "Duplicate Value";
  public ExDBDuplicateValue(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExDBDuplicateValue(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExDBDuplicateValue(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExDBDuplicateValue(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExDBDuplicateValue(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
