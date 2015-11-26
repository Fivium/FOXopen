package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExLockViolation extends ExEvent
{
  static String TYPE = "Node lock violation";
  public ExLockViolation(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExLockViolation(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExLockViolation(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExLockViolation(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExLockViolation(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
