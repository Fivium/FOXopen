package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExCursorRoot;

public class ExCursorCardinality extends ExCursorRoot {
  static String TYPE = "Fox cursor Cardinality Error";
  public ExCursorCardinality(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExCursorCardinality(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExCursorCardinality(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExCursorCardinality(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExCursorCardinality(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
