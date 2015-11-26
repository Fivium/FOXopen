package net.foxopen.fox;

import net.foxopen.fox.ex.ExInternal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;


// TODO - Could do with less StringBuffers and more StringBuilders
public class FoxResponseCHAR
extends FoxResponse {
  private final StringBuffer mStringBuffer;
  private final String mContentType;
  private final long mBrowserCacheMilliSec;

  public FoxResponseCHAR(
    String pContentType
  , StringBuffer pStringBuffer
  , long pBrowserCacheMilliSec
  )
  {
    mStringBuffer = pStringBuffer;
    mContentType = pContentType;
    mBrowserCacheMilliSec = pBrowserCacheMilliSec;
  }

  public void respond(FoxRequest pRequest)
  throws ExInternal
  {
    // Set the response header
    pRequest.getHttpResponse().setStatus(mStatusCode);
    pRequest.getHttpResponse().setContentType(mContentType);
    pRequest.getHttpResponse().setDateHeader("Expires", mBrowserCacheMilliSec > 0 ? mBrowserCacheMilliSec + System.currentTimeMillis() : 0); // No cache

    //Other BeforeResponseActions may require additional headers to be set
    runBeforeResponseActions();

    setResponseHttpHeaders(pRequest.getHttpResponse());

    // TODO - Is this really the best way to convert the stringbuffer to UTF8? What about other charsets
    // Process UTF-8 Character set encoding translation
    if(mContentType.indexOf("UTF-8") != -1) {
      byte[] lByteArray;
      try {
        lByteArray = mStringBuffer.toString().getBytes("UTF-8");
      }
      catch (UnsupportedEncodingException e) {
        throw new ExInternal("Character encoding error", e);
      }

      pRequest.getHttpResponse().setContentLength(lByteArray.length);

      try {
        OutputStream lOutputStream = pRequest.getHttpResponse().getOutputStream();
        lOutputStream.write(lByteArray);
        lOutputStream.close();
      }
      catch (IOException e) {
        throw new ExInternal("Stream write CHAR response content", e);
      }
    }
    // Old code that did not cope with character sets
    // NB: This could still be used for static HTML from database, CSS, JavaScript etc
    else {
      // Get the response content writer
      pRequest.getHttpResponse().setContentLength(mStringBuffer.length());
      try {
        PrintWriter lPrintWriter = pRequest.getHttpResponse().getWriter();
        lPrintWriter.print(mStringBuffer.toString());
        lPrintWriter.close();
      }
      catch(IOException lIOException) {
        throw new ExInternal("Print CHAR response content", lIOException);
      }
    }
  }

}
