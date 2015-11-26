package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.plugin.api.dom.FxpDOM;

public class ExInternal extends ExRuntimeRoot {
  static String TYPE = "Fox Software Internal Error";
  public ExInternal(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExInternal(String msg, FxpDOM xml) {
    super(msg, TYPE, (DOM) xml, null);
  }
  public ExInternal(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExInternal(String msg, FxpDOM xml, Throwable e) {
    super(msg, TYPE, (DOM) xml, e);
  }
  public ExInternal(String msg, String type, FxpDOM xml, Throwable exception) {
    super(msg,type,(DOM) xml,exception);
  }
}

