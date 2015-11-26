package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExNavigation extends ExGeneral {
  static String TYPE = "Navigation Error";
  public ExNavigation(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExNavigation(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExNavigation(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExNavigation(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExNavigation(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
