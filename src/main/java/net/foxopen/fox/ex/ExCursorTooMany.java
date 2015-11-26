package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

/** Throwables for multiple rows returned when single expected
 */
public class ExCursorTooMany extends ExCursorCardinality
{
  static String TYPE = "Database Too Many Rows";
  public ExCursorTooMany(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExCursorTooMany(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExCursorTooMany(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExCursorTooMany(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExCursorTooMany(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
