package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

// This exception is thrown to indicate to module designers, that the module
// definition has a flaw. For example, the state-list section may be missing
public class ExModule extends ExGeneral {
  static String TYPE = "Module Structure Error";
  public ExModule(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExModule(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExModule(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExModule(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExModule(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
  
