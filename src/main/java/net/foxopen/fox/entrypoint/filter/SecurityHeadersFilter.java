package net.foxopen.fox.entrypoint.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SecurityHeadersFilter implements Filter {
  /**
   * This filter is applied globally, but some servlets shouldn't have these header applied. This exclusion happens in
   * this class rather than the web.xml as the code can be more flexible.<br/>
   * Currently excluded servlets:
   * <ul>
   *   <li>
   *     /upload<br/>
   *     <i>The upload servlet might use an iframe in old versions of IE and the X-Frame-Options might break this</i>
   *   </li>
   * </ul>
   */
  private final Set<String> EXCLUDED_SERVLETS = new HashSet<>(Arrays.asList("/upload"));

  /**
   * X-Frame-Options response header to tell IE8 (and any other browsers who
   * decide to implement) not to display this content in a frame. For details, please
   * refer to http://blogs.msdn.com/sdl/archive/2009/02/05/clickjacking-defense-in-ie8.aspx.
   */
  private String mFrameOptionsMode = "DENY";

  /**
   * X-XSS-Protection response header enables the Cross-site scripting (XSS) filter built into most recent web browsers.
   * It's usually enabled by default anyway, so the role of this header is to re-enable the filter for this particular
   * website if it was disabled by the user. For details, please refer to
   * http://blogs.msdn.com/b/ie/archive/2008/07/02/ie8-security-part-iv-the-xss-filter.aspx
   */
  private String mXSSProtectionsMode = "1; mode=block";

  /**
   * X-Content-Type-Options response header prevents Internet Explorer and Google Chrome from MIME-sniffing a response
   * away from the declared content-type. This reduces exposure to drive-by download attacks and sites serving user
   * uploaded content that, by clever naming, could be treated by MSIE as executable or dynamic HTML files. For details,
   * please refer to http://blogs.msdn.com/b/ie/archive/2008/09/02/ie8-security-part-vi-beta-2-update.aspx
   */
  private String mContentTypeOptions = "nosniff";

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    String lRequestURI = ((HttpServletRequest)request).getServletPath();

    // Don't add security headers for excluded servlets
    if (!EXCLUDED_SERVLETS.contains(lRequestURI)) {
      HttpServletResponse res = (HttpServletResponse) response;

      res.addHeader("X-Frame-Options", mFrameOptionsMode);
      res.addHeader("X-XSS-Protection", mXSSProtectionsMode);
      res.addHeader("X-Content-Type-Options", mContentTypeOptions);
    }

    chain.doFilter(request, response);
  }

  public void destroy() {
  }

  public void init(FilterConfig filterConfig) {
    String lFrameOptionsOverride = filterConfig.getInitParameter("X-Frame-Options");
    if (lFrameOptionsOverride != null) {
      mFrameOptionsMode = lFrameOptionsOverride;
    }
    String lXSSProtectionsOverride = filterConfig.getInitParameter("X-XSS-Protection");
    if (lXSSProtectionsOverride != null) {
      mXSSProtectionsMode = lXSSProtectionsOverride;
    }
    String lContentTypeOptionsOverride = filterConfig.getInitParameter("X-Content-Type-Options");
    if (lContentTypeOptionsOverride != null) {
      mContentTypeOptions = lContentTypeOptionsOverride;
    }
  }
}
