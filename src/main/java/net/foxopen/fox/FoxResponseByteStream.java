package net.foxopen.fox;

import net.foxopen.fox.ex.ExInternal;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class FoxResponseByteStream
extends FoxResponse {
  private final HttpServletResponse mHttpServletResponse;

  public FoxResponseByteStream(
  String pContentType
  , FoxRequest pFoxRequest
  , long pBrowserCacheMilliSec
  )
  {
    this(pContentType, pFoxRequest, pBrowserCacheMilliSec, HttpServletResponse.SC_OK);
  }

  public FoxResponseByteStream(
  String pContentType
  , FoxRequest pFoxRequest
  , long pBrowserCacheMilliSec
  , int pStatusCode
  )
  {
    mHttpServletResponse = pFoxRequest.getHttpResponse();
    mHttpServletResponse.setStatus(pStatusCode);
    mHttpServletResponse.setContentType(pContentType);
    mHttpServletResponse.setDateHeader("Expires", pBrowserCacheMilliSec > 0 ? pBrowserCacheMilliSec + System.currentTimeMillis() : 0); // No cache
  }

  public OutputStream getHttpServletOutputStream() {

    //Any BeforeResponseActions may require additional headers to be set
    runBeforeResponseActions();

    setResponseHttpHeaders(mHttpServletResponse);
    try {
      return mHttpServletResponse.getOutputStream();
    }
    catch (IOException e) {
      throw new ExInternal("Cannot get OutputStream for FoxResponseByteStream", e);
    }
  }

  /**
   * @see javax.servlet.ServletResponse#flushBuffer()
   * @throws IOException
   */
  public void flushBuffer() throws IOException {
    mHttpServletResponse.flushBuffer();
  }

  /**
   * Respond methods have no effect on Stream type response classes
   *
   * @param pRequest
   * @throws ExInternal
   */
  @Deprecated
  public void respond(FoxRequest pRequest)
    throws ExInternal  {
    // Do nothing
  }

  @Deprecated
  public void setStatus(int pStatusCode) {
    throw new ExInternal("Called setStatus on a FoxResponseByteStream which will have no effect, status needs to be set at response construction time");
  }
}
