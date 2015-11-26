package net.foxopen.fox;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class FoxRequest {

  public abstract String getRequestLogId();

  /**
   * Gets the path info for the current request, i.e. the part after the context name and servlet name. The URI will have
   * a preceding slash.
   * @return Current request path.
   */
  public abstract String getRequestURI();

  /**
   * Gets the path info for the current request, i.e. the part after the context name and servlet name. The URI will have
   * a preceding slash.
   * @return Current request path.
   */
  public abstract StringBuffer getRequestURIStringBuffer();

  /**
   * Gets the path info for the current request, i.e. the part after the context name and servlet name. The URI will have
   * a preceding slash.
   * @return Current request path.
   */
  public abstract StringBuilder getRequestURIStringBuilder();

  public abstract void addCookie (String pName, String pValue);

  public abstract void addCookie (String pName, String pValue, int pMaxAge);

  public abstract void addCookie (String pName, String pValue, boolean pSetPath);

  public abstract void addCookie (String pName, String pValue, boolean pSetPath, boolean pHttpOnly);

  public abstract void addCookie (String pName, String pValue, int pMaxAge, boolean pSetPath);

  public abstract void addCookie (String pName, String pValue, int pMaxAge, boolean pSetPath, boolean pHttpOnly);

  public abstract void removeCookie (String pName, boolean pSetPath);

  public abstract void removeCookie (String pName);

  public abstract String getCookieValue (String pCookieName);

  public abstract HttpServletRequest getHttpRequest ();

  public abstract HttpServletResponse getHttpResponse ();

  /**
   * Gets the named parameter from the request, or null if it is not defined. For parameters with multiple values the
   * first value is returned.
   * @param pParameterName
   * @return
   */
  public abstract String getParameter(String pParameterName);

}
