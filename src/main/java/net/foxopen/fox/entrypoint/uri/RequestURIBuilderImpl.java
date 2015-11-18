package net.foxopen.fox.entrypoint.uri;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.banghandler.BangHandlerServlet;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.servlets.StaticServlet;
import net.foxopen.fox.entrypoint.servlets.TempResourceServlet;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.entrypoint.ws.WebServiceServlet;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.image.ImageServlet;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.storage.TempResource;
import org.apache.http.client.utils.URIBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

public class RequestURIBuilderImpl
implements RequestURIBuilder {

  private final HttpServletRequest mHttpServletRequest;
  private final String mAppMnem;
  private final Map<String, String> mParamMap = new LinkedHashMap<>(4);

  public static RequestURIBuilder createFromRequestContext(RequestContext pRequestContext, boolean pAllowParams) {
    if(pAllowParams) {
      return new RequestURIBuilderImpl(pRequestContext.getFoxRequest().getHttpRequest(), pRequestContext.getRequestAppMnem());
    }
    else {
      return new NoParamRequestURIBuilder(pRequestContext.getFoxRequest().getHttpRequest(), pRequestContext.getRequestAppMnem());
    }
  }

  public static RequestURIBuilder createFromHttpRequest(HttpServletRequest pHttpServletRequest) {
    return new RequestURIBuilderImpl(pHttpServletRequest, FoxGlobals.getInstance().getFoxEnvironment().getDefaultAppMnem());
  }

  public static RequestURIBuilder createFromFoxRequest(FoxRequest pFoxRequest) {
    return new RequestURIBuilderImpl(pFoxRequest.getHttpRequest(), FoxGlobals.getInstance().getFoxEnvironment().getDefaultAppMnem());
  }

  protected RequestURIBuilderImpl(HttpServletRequest pHttpServletRequest, String pAppMnem) {
    mHttpServletRequest = pHttpServletRequest;
    mAppMnem = pAppMnem;
  }

  private String contextPath() {
    return mHttpServletRequest.getContextPath();
  }

  private String buildURIHead(String pServletPath) {
    return contextPath() + "/" +  pServletPath;
  }

  @Override
  public RequestURIBuilderImpl setParam(String pParamName, String pParamValue) {
    mParamMap.put(pParamName, pParamValue);
    return this;
  }

  @Override
  public RequestURIBuilder setParams(Map<String, String> pParamMap) {
    mParamMap.putAll(pParamMap);
    return this;
  }

  @Override
  public String buildStaticResourceURI(String pResourcePath) {
    return buildURIHead(StaticServlet.SERVLET_PATH) + "/" + mAppMnem + "/" + StaticServlet.getStaticResourcePathWithHashParameter(pResourcePath);
  }

  @Override
  public String buildContextResourceURI(String pResourcePath) {
    //TODO PN this can be consolidated with buildStaticResourceURI, need a registry of every built-in resource's location
    return contextPath() + pResourcePath;
  }

  @Override
  public String buildStaticResourceOrFixedURI(String pResourcePathOrFixedURI) {

    if (isFixedURI(pResourcePathOrFixedURI)) {
      return pResourcePathOrFixedURI;
    }
    else {
      return buildStaticResourceURI(pResourcePathOrFixedURI);
    }
  }

  @Override
  public boolean isFixedURI(String pURI) {
    return pURI.startsWith("http://") || pURI.startsWith("https://") || pURI.startsWith("/") || pURI.startsWith("./") || pURI.startsWith("../") || pURI.startsWith("#");
  }

  private String buildURIWithGetParams(String pPath) {

    //Construct an Apache URI builder - we can't simply wrap an instance of this as it allows the same param to be set many times (i.e. ?a=1&a=2)
    URIBuilder lURIBuilder = new URIBuilder();
    //Get the /context/servlet prefix for the URI
    lURIBuilder.setPath(pPath);

    //Add params
    for(Map.Entry<String, String> lParam : mParamMap.entrySet()) {
      if(!XFUtil.isNull(lParam.getValue())) {
        lURIBuilder.addParameter(lParam.getKey(), lParam.getValue());
      }
    }

    //We could validate params here if we were feeling generous (seems like a bit of a waste of time, it's a Java dev time error)
    try {
      return lURIBuilder.build().toString();
    }
    catch (URISyntaxException e) {
      throw new ExInternal("Failed to generate URI", e);
    }
  }

  @Override
  public String buildTempResourceURI(TempResource<?> pTempResource, String pReadableName) {
    return buildURIHead(TempResourceServlet.SERVLET_PATH) + "/" + pTempResource.getTempResourceId() + "/" + pReadableName;
  }

  //Just the location of a servlet with no params (or querystring params)
  @Override
  public String buildServletURI(String pServletPath) {
    String lURIPath = buildURIHead(pServletPath);
    if(mParamMap.isEmpty()) {
      return lURIPath;
    }
    else {
      return buildURIWithGetParams(lURIPath);
    }
  }

  //Appends params using the specified path param template
  @Override
  public String buildServletURI(String pServletPath, PathParamTemplate pPathParamTemplate) {
    String lBaseURI = buildURIHead(pServletPath) + pPathParamTemplate.generateURIFromParamMap(mParamMap);

    URIBuilder lURIBuilder = new URIBuilder();
    lURIBuilder.setPath(lBaseURI);

    //Add params to the query string which are NOT part of the path param
    for(Map.Entry<String, String> lParam : mParamMap.entrySet()) {
      if(!pPathParamTemplate.hasParam(lParam.getKey()) && !XFUtil.isNull(lParam.getValue())) {
        lURIBuilder.addParameter(lParam.getKey(), lParam.getValue());
      }
    }

    try {
      return lURIBuilder.build().toString();
    }
    catch (URISyntaxException e) {
      throw new ExInternal("Failed to generate servlet URI", e);
    }
  }

  private String webServicePath(String pCategoryName, String pWebServiceName, String pEndPointName) {
    return WebServiceServlet.SERVLET_PATH  + "/" +WebServiceServlet.SERVICE_TYPE_REST + "/" + pCategoryName + "/" + pWebServiceName + "/" + pEndPointName;
  }

  @Override
  public String buildWebServiceURI(String pCategoryName, String pWebServiceName, String pEndPointName) {
    return buildServletURI(webServicePath(pCategoryName, pWebServiceName, pEndPointName));
  }

  @Override
  public String buildWebServiceURI(String pCategoryName, String pWebServiceName, String pEndPointName, PathParamTemplate pPathParamTemplate) {
    return buildServletURI(webServicePath(pCategoryName, pWebServiceName, pEndPointName), pPathParamTemplate);
  }

  @Override
  public String buildBangHandlerURI(BangHandler pBangHandler) {
    String lURIPath = buildURIHead(BangHandlerServlet.getServletPath()) + "/!" + pBangHandler.getAlias();
    return buildURIWithGetParams(lURIPath);
  }

  @Override
  public String buildImageURI(String pImageURI) {
    if (!pImageURI.contains(";")) {
      return buildStaticResourceOrFixedURI(pImageURI);
    }
    else {
      return buildURIHead(ImageServlet.SERVLET_PATH) + ImageServlet.generateImageCombinatorURISuffix(mAppMnem, pImageURI);
    }
  }

  @Override
  public String convertToAbsoluteURL(String pRelativeURI) {

    String lScheme = mHttpServletRequest.getScheme();
    int lServerPort = mHttpServletRequest.getServerPort();

    //Only add a port suffix if it's not the standard port for the current scheme
    String lPort = "";
    if(!(("http".equals(lScheme) && lServerPort == 80) || ("https".equals(lScheme) && lServerPort == 443))) {
      lPort = ":" + lServerPort;
    }

    //Add a slash at the start of the URI if it doesn't already have one
    if(!pRelativeURI.startsWith("/")) {
      pRelativeURI = "/" + pRelativeURI;
    }

    return lScheme + "://" + mHttpServletRequest.getServerName() + lPort + pRelativeURI;
  }
}
