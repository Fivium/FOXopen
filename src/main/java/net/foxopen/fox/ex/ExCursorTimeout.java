package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExCursorRoot;

/** Throwables for timeout on database read
 */
public class ExCursorTimeout extends ExCursorRoot
{
  static String TYPE = "Database Timeout on row access ";
  public ExCursorTimeout(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExCursorTimeout(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExCursorTimeout(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExCursorTimeout(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExCursorTimeout(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
