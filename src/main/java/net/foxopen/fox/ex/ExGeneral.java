package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExRoot;
import net.foxopen.fox.plugin.api.dom.FxpDOM;

public class ExGeneral extends ExRoot{
  static String TYPE = "General Error";
  public ExGeneral(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExGeneral(String msg, FxpDOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExGeneral(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExGeneral(String msg, FxpDOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExGeneral(String msg, String type, FxpDOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}

