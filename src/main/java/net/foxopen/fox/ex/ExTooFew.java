package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExTooFew extends ExCardinality {
  static String TYPE = "Too Few Error";
  public ExTooFew(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExTooFew(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExTooFew(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExTooFew(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExTooFew(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
