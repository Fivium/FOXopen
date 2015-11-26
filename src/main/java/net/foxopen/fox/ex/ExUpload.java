package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

public class ExUpload extends ExInternal {
  static String TYPE = "Fox Upload Error";
  
  private final String mReadableMessage;
  
  public ExUpload(String msg)  {
    super(msg, TYPE, null, null);
    mReadableMessage = msg;
  }
  
  public ExUpload(String msg, String pReadableMessage)  {
    super(msg, TYPE, null, null);
    mReadableMessage = pReadableMessage;
  }
  
  public ExUpload(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
    mReadableMessage = msg;
  }
  
  public ExUpload(String msg, Throwable e) {
    super(msg, TYPE, null, e);
    mReadableMessage = msg;
  }
  
  public ExUpload(String msg, String pReadableMessage, Throwable e) {
    super(msg, TYPE, null, e);
    mReadableMessage = pReadableMessage;
  }
  
  public ExUpload(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
    mReadableMessage = msg;
  }
  public ExUpload(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
    mReadableMessage = msg;
  }

  public String getReadableMessage() {
    return mReadableMessage;
  }
}
