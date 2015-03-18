package net.foxopen.fox.entrypoint.filter;

import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.logging.RequestLogger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestLogFilter
implements Filter {

  public static final String REQUEST_ATTRIBUTE_LOG_ID = "net.foxopen.fox.entrypoint.filter.RequestLogFilter.RequestLogId";
  public static final String REQUEST_ATTRIBUTE_TRACK_ID = "net.foxopen.fox.entrypoint.filter.RequestLogFilter.TrackID";

  private static final AtomicInteger gActiveFoxRequestCounter = new AtomicInteger(0);

  @Override
  public void init(FilterConfig pFilterConfig)
  throws ServletException {
  }

  public static int getActiveRequestCount() {
    return gActiveFoxRequestCounter.intValue();
  }

  @Override
  public void doFilter(ServletRequest pServletRequest, ServletResponse pServletResponse, FilterChain pFilterChain)
  throws IOException, ServletException {

    long lRequestStartTime = System.currentTimeMillis();

    HttpServletRequest lHttpServletRequest = (HttpServletRequest) pServletRequest;

    //Start the log on the database
    String lRequestLogId = "";
    try {
      lRequestLogId = RequestLogger.instance().startRequestLog(lHttpServletRequest);
      pServletRequest.setAttribute(REQUEST_ATTRIBUTE_LOG_ID, lRequestLogId);
    }
    catch (Throwable th) {
      //TODO PN - set a big red flag in the status API
      FoxLogger.getLogger().error("Serious error caught at start of RequestLogFilter", th);
    }
    //Still allow the request through if the log failed - it should be logged on Apache anyway

    //Log start to disk
    gActiveFoxRequestCounter.incrementAndGet();
    if (FoxLogger.getLogger().isTraceEnabled()) {
      FoxLogger.getLogger().trace("Request Start {}, {} active requests, {}, {}", lRequestLogId, gActiveFoxRequestCounter.get(), lHttpServletRequest.getRequestURI(), lHttpServletRequest.getHeader("User-Agent"));
    }
    else if (FoxLogger.getLogger().isInfoEnabled()) {
      FoxLogger.getLogger().info("Request Start {}, {} active requests", lRequestLogId, gActiveFoxRequestCounter.get());
    }
    else {
      FoxLogger.getLogger().debug("Request Start {}", lRequestLogId);
    }

    //Delegate down the filter chain
    try {
      pFilterChain.doFilter(pServletRequest, pServletResponse);
    }
    finally {
      gActiveFoxRequestCounter.decrementAndGet();
      if (FoxLogger.getLogger().isTraceEnabled()) {
        FoxLogger.getLogger().trace("Request End {}, {} active requests, {}ms", lRequestLogId, gActiveFoxRequestCounter.get(), (System.currentTimeMillis() - lRequestStartTime));
      }
      else if (FoxLogger.getLogger().isInfoEnabled()) {
        FoxLogger.getLogger().info("Request End {}, {} active requests", lRequestLogId, gActiveFoxRequestCounter.get());
      }
      else {
        FoxLogger.getLogger().debug("Request End {}", lRequestLogId);
      }

      try {
        RequestLogger.instance().endRequestLog(lRequestLogId, lHttpServletRequest, (HttpServletResponse) pServletResponse);
      }
      catch (Throwable th) {
        //TODO PN - set a big red flag in the status API
        //Suppress errors from log finalisation
        FoxLogger.getLogger().error("Serious error caught at end of RequestLogFilter", th);
      }
    }
  }

  @Override
  public void destroy() {
  }
}
