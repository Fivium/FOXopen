package net.foxopen.fox.entrypoint.filter;

import net.foxopen.fox.entrypoint.servlets.StaticServlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Filter for the default servlet (which serves static resources from the webapp's root directory) which modifies the headers
 * used for cache control.
 */
public class DefaultServletCacheFilter
implements Filter {

  @Override
  public void init(FilterConfig pFilterConfig)
  throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest pServletRequest, ServletResponse pServletResponse, FilterChain pFilterChain)
  throws IOException, ServletException {

    final HttpServletResponse lResponse = (HttpServletResponse) pServletResponse;

    //Set expiry time to a year - these resources are unlikely to change, and we can modify the directory name if they do
    lResponse.setDateHeader("Expires", System.currentTimeMillis() + StaticServlet.staticResourceExpiryTimeMS());

    //ResponseWrapper to prevent etag headers being sent on static resources - the expires header should suffice
    pFilterChain.doFilter(pServletRequest, new HttpServletResponseWrapper(lResponse) {
      public void setHeader(String name, String value) {
        //Disallow etag, allow all others
        if (!"etag".equalsIgnoreCase(name)) {
          super.setHeader(name, value);
        }
      }
    });
  }

  @Override
  public void destroy() {
  }
}
