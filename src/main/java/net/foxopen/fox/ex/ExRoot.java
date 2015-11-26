package net.foxopen.fox.ex;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.plugin.api.dom.FxpDOM;
import net.foxopen.fox.track.Track;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExRoot
extends Exception
implements TrackableException {

  static String TYPE = "Fox Software Error";

  private final String mErrorId = XFUtil.unique();

  String t = TYPE;
  FxpDOM x = null;

  private int mHttpStatusCode = 500;

  public ExRoot(String msg, String type, FxpDOM xml, Throwable exgeneral) {
     super(msg, exgeneral);
     x=xml;
     t=type;
     Track.recordException(this);
//     trackException();
  }

    /**
     * Returns the detail message, including the message from the nested
     * exception if there is one.
     */
   public String getMessage()
   {
      return super.getMessage()+(getCause() != null ? (" See nested exception: \n\t"+ XFUtil.nvl(getCause().getMessage(), "[no message available]")) : "");
   }

  public String toString() {
    return t+": "+getMessage();
  }

   public String getMessageStack()
   {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      printStackTrace(pw);

      return sw.toString();
   }

  public String getXmlString() {
    // Output associated xml
    if(x!=null) {
      // TODO AT - this was not throwing exinternals before
      return x.outputNodeContentsToString(true);
    }
    else {
      return "";
    }
  }

  /** Convert standard exceptions to common ExInternal("Unexpected") form */
  public ExInternal toUnexpected() {
    return new ExInternal("Unexpected Error", this);
  }
  public ExInternal toUnexpected(String pMsg) {
    return new ExInternal("Unexpected Error: "+pMsg, this);
  }

  public ExServiceUnavailable toServiceUnavailable() {
    return new ExServiceUnavailable("Service Unavailable", this);
  }
  public ExServiceUnavailable toServiceUnavailable(String pMsg) {
    return new ExServiceUnavailable("Service Unavailable: "+pMsg, this);
  }


  public void setHttpStatusCode(int pStatusCode) {
    mHttpStatusCode = pStatusCode;
  }
  public int getHttpStatusCode() {
    return mHttpStatusCode;
  }

  @Override
  public String getErrorId() {
    return mErrorId;
  }
}

