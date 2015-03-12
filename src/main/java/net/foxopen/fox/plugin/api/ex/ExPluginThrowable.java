package net.foxopen.fox.plugin.api.ex;

import net.foxopen.fox.ex.ExGeneral;
import net.foxopen.fox.plugin.api.dom.FxpDOM;

public class ExPluginThrowable extends ExGeneral {

  static String TYPE = "General Plugin Error";

  public ExPluginThrowable(String msg)  {
    super(msg, TYPE, null, null);
  }

  public ExPluginThrowable(String msg, FxpDOM xml) {
    super(msg, TYPE, xml, null);
  }

  public ExPluginThrowable(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }

  public ExPluginThrowable(String msg, FxpDOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }

  public ExPluginThrowable(String msg, String type, FxpDOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
