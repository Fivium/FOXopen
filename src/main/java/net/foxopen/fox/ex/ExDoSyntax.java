package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExDoSyntax extends ExGeneral { 
  static String TYPE = "DO Command Syntax";
  public ExDoSyntax(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExDoSyntax(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExDoSyntax(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExDoSyntax(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExDoSyntax(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
