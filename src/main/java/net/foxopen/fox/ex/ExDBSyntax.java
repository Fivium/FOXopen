package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExDBSyntax extends ExDB
{
  static String TYPE = "Database Syntax";
  public ExDBSyntax(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExDBSyntax(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExDBSyntax(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExDBSyntax(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExDBSyntax(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
