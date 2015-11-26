package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExTooMany extends ExCardinality {
  static String TYPE = "Too Many Error";
  public ExTooMany(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExTooMany(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExTooMany(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExTooMany(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExTooMany(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
