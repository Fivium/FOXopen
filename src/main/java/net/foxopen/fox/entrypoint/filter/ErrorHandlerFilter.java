package net.foxopen.fox.entrypoint.filter;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxRequestHttp;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.servlets.ErrorServlet;
import net.foxopen.fox.logging.ErrorLogger;
import net.foxopen.fox.logging.FoxLogger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Catch all final errors and server something nicer
 */
public class ErrorHandlerFilter implements Filter {

  @Override
  public void init(FilterConfig pFilterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
  throws IOException, ServletException {
    try {
      chain.doFilter(request, response);
    }
    catch (Throwable th) {
      String lRequestLogId = (String) request.getAttribute(RequestLogFilter.REQUEST_ATTRIBUTE_LOG_ID);

      Throwable lThrowable = th;
      try{
        // Make sure the error is logged and get the error reference
        long lErrorRef = ErrorLogger.instance().logError(th, ErrorLogger.ErrorType.FATAL, lRequestLogId);
        FoxRequest lFoxRequest = new FoxRequestHttp((HttpServletRequest) request, (HttpServletResponse) response);

        ErrorServlet.respondWithErrorPage(lFoxRequest, String.valueOf(lErrorRef), (String)request.getAttribute(RequestLogFilter.REQUEST_ATTRIBUTE_TRACK_ID), XFUtil.getJavaStackTraceInfo(lThrowable));

        return;
      }
      catch (Throwable fatality) {
        FoxLogger.getLogger().error("Error caught while attempting to redirect error response", fatality);
        lThrowable = fatality;
      }

      //Final catch all error throw if something went disastrously wrong
      if (FoxGlobals.getInstance().canShowStackTracesOnError()) {
        throw new ServletException("Caught fatal error in ErrorHandler", lThrowable);
      }
    }
  }

  @Override
  public void destroy() {
  }
}
