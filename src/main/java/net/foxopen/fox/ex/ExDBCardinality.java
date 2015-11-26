package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExDBCardinality extends ExDB {
  static String TYPE = "Database Cardinality Error";
  public ExDBCardinality(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExDBCardinality(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExDBCardinality(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExDBCardinality(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExDBCardinality(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
