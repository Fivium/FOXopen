package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

/** Exceptions for instantiation and iteration through the Fox Cursor Object (Database Cursor)
 */
public class ExCursorRoot extends ExRoot {
  static String TYPE = "Fox cursor Error";
  /** Base level Error
   * @param msg Text of error message
   */
  public ExCursorRoot(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExCursorRoot(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExCursorRoot(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExCursorRoot(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExCursorRoot(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
  
}
