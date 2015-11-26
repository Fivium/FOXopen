package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

/**
 * Errors caused by the SQL parser component.
 */
public class ExParser extends ExRoot {
  static String TYPE = "Fox Parser Error";
  public ExParser(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExParser(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExParser(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExParser(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExParser(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
