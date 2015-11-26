package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExGeneral;

// This exception is thrown to indicate to user that request (usually URL request)
// is not valid in some way - e.g. subject (module) not known.
public class ExUserRequest extends ExGeneral {
  static String TYPE = "Request Error";
  public ExUserRequest(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExUserRequest(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExUserRequest(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExUserRequest(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExUserRequest(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
