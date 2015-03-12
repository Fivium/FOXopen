/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
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

