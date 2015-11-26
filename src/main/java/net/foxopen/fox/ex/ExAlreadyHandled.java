package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

/**
 * This exception can be used to wrap another so that the FOX servlet knows it has already been dealt with
 */
public class ExAlreadyHandled extends ExRuntimeRoot {
  static String TYPE = "Handled Error";
  public ExAlreadyHandled(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExAlreadyHandled(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExAlreadyHandled(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}

