package net.foxopen.fox.entrypoint.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;


/**
 * Transform unknown charset requests to the default request encoding specified in web.xml
 */
public class RequestEncodingFilter implements Filter {
  private String mDefaultEncoding = null;

  public void init(FilterConfig filterConfig) throws
     ServletException {
     mDefaultEncoding = filterConfig.getInitParameter("defaultRequestEncoding");
  }

  public void destroy() {
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
  throws IOException, ServletException {
    // Change encoding to default if there is a default set and the request didn't specify an encoding
    if (mDefaultEncoding != null && (request.getCharacterEncoding() == null || "".equals(request.getCharacterEncoding()))) {
      request.setCharacterEncoding(mDefaultEncoding);
    }

    chain.doFilter(request, response);
  }
}
