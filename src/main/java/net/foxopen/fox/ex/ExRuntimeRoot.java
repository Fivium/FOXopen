package net.foxopen.fox.ex;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.track.Track;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExRuntimeRoot
extends RuntimeExceptionWrapperException
implements TrackableException {

  private final String mErrorId = XFUtil.unique();

  public static final String TYPE = "Fox Software Runtime Error";

   /** The type of error. */
  private String t = TYPE;
  /** An XML DOM related to the error and giving some context to the error that has occurred. */
  private DOM x = null;


   /**
    * Constructs a <code>ExRuntimeRoot</code> with the specified
    * detail message.
    *
    * @param s the detail message
    */
   public ExRuntimeRoot(String s)
   {
      this(s, TYPE, null, null);
   }


   /**
    * Constructs a <code>ExRuntimeRoot</code> with the specified
    * detail message and nested exception.
    *
    * @param s the detail message
    * @param ex the nested exception
    */
   public ExRuntimeRoot(String s, Throwable ex)
   {
      this(s, TYPE, null, ex);
   }

   /**
    * Constructs a <code>ExRuntimeRoot</code> with the specified
    * detail message and nested exception.
    *
    * @param s the detail message
    * @param type a string containing a description of the type of error
    * @param xml an XML DOM related to the error
    * @param ex the nested exception
    */
   public ExRuntimeRoot(String msg, String type, DOM xml, Throwable ex) {
      super(msg, ex);
      x=xml;
      t=type;
      Track.recordException(this);
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
      return x.outputNodeToStringNoExInternal();
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

  @Override
  public String getErrorId() {
    return mErrorId;
  }

}

