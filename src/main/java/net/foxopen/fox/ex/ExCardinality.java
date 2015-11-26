package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExGeneral;

public class ExCardinality extends ExGeneral {
  static String TYPE = "Cardinality Error";
  public ExCardinality(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExCardinality(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExCardinality(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExCardinality(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExCardinality(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
