package net.foxopen.fox;

import net.foxopen.fox.ex.ExInternal;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;


public class FoxResponseBytes
extends FoxResponse {
  private final byte[] mByteArray;
  private final String mContentType;
  private final long mBrowserCacheMilliSec;

  public FoxResponseBytes(
    String pContentType
  , byte[] pByteArray
  , long pBrowserCacheMilliSec
  )
  {
    mByteArray = pByteArray;
    mContentType = pContentType;
    mBrowserCacheMilliSec = pBrowserCacheMilliSec;
  }

  public void respond(FoxRequest pRequest)
  throws ExInternal
  {
     HttpServletResponse lHttpServletResponse = pRequest.getHttpResponse();

    //Any BeforeResponseActions may require additional headers to be set
    runBeforeResponseActions();

    // TODO - Not sure about this, why no content-type/length set when other headers set? Could check for collisions?
    // Fox header processing
    if(this.getHttpHeaderList() == null || this.getHttpHeaderList().size() == 0) {
      lHttpServletResponse.setContentType(mContentType);
      lHttpServletResponse.setContentLength(mByteArray.length);
      if(mBrowserCacheMilliSec > 0) {
        lHttpServletResponse.setDateHeader("Expires", mBrowserCacheMilliSec + System.currentTimeMillis());
      }
    }
    // HttpMessage header processing
    else {
      setResponseHttpHeaders(lHttpServletResponse);
    }

    // Get the response content writer
    try {
      OutputStream lOutputStream = lHttpServletResponse.getOutputStream();
      lOutputStream.write(mByteArray);
      lOutputStream.close();
    }
    catch(IOException lIOException) {
      throw new ExInternal("Write ByteArray response content", lIOException);
    }
  }

  public final String getBase64Data() {
    return new String(Base64.encodeBase64(mByteArray));
  }

  public final String getBase64DataURLText() {
    return "data:"+mContentType+";base64,"+getBase64Data();
  }

  public final byte[] getBytes() {
    return mByteArray;
  }

}
