package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExImageCurrentlyProcessing extends ExInternal
{
  static String TYPE = "Image Currently Processing";
  public ExImageCurrentlyProcessing(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExImageCurrentlyProcessing(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExImageCurrentlyProcessing(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExImageCurrentlyProcessing(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExImageCurrentlyProcessing(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
