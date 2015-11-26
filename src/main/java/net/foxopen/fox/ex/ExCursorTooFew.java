package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

/** Throwables for now rows returned
 */
public class ExCursorTooFew extends ExCursorCardinality
{
  static String TYPE = "Database Too Few Rows";
  public ExCursorTooFew(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExCursorTooFew(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExCursorTooFew(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExCursorTooFew(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExCursorTooFew(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
