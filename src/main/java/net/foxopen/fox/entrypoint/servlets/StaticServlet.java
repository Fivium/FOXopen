package net.foxopen.fox.entrypoint.servlets;

import net.foxopen.fox.FoxComponent;
import net.foxopen.fox.FoxRequestHttp;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExInternal;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class StaticServlet
extends HttpServlet {

  @Override
  public final void init(ServletConfig pServletConfig) throws ServletException {
    super.init(pServletConfig);
  }

  public static final String SERVLET_PATH = "static";

  /**
   * @return Gets the expiry time in milliseconds for any static resource which is unlikely to be modified.
   */
  public static long staticResourceExpiryTimeMS() {
    return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365);
  }

  /**
   * Gets the entry URI for the static servlet including the app mnem. This does not modify the builder.
   * @param pURIBuilder
   * @param pAppMnem
   * @return
   */
  public static String getURIWithAppMnem(RequestURIBuilder pURIBuilder, String pAppMnem) {
    return pURIBuilder.buildServletURI(SERVLET_PATH) + "/" + pAppMnem;
  }

  @Override
  protected void doGet(HttpServletRequest pRequest, HttpServletResponse pResponse)
  throws ServletException, IOException {
    processHttpRequest(pRequest, pResponse);
  }

  @Override
  protected void doPost(HttpServletRequest pRequest, HttpServletResponse pResponse)
  throws ServletException, IOException {
    processHttpRequest(pRequest, pResponse);
  }

  public final void processHttpRequest(HttpServletRequest pRequest, HttpServletResponse pResponse) {
    // Grab from caches and return...
    FoxRequestHttp lFoxRequest = new FoxRequestHttp(pRequest, pResponse);
    StringBuilder lFilePath = lFoxRequest.getRequestURIStringBuilder();
    FoxComponent lFoxComponent;
    try{
      lFoxComponent = ComponentManager.getComponent(lFilePath);
      lFoxComponent.processResponse(lFoxRequest, new StringBuffer(lFilePath)).respond(lFoxRequest);
    }
    catch (Throwable th) {
      throw new ExInternal("Failed to resolve static resource", th);
    }
  }
}
