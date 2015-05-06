package net.foxopen.fox.entrypoint.uri;

import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.thread.storage.TempResource;

import java.util.Map;

/**
 * A builder for constructing URIs within the FOX engine. The builder should be aware of the client's entry point, and can
 * construct a URI which correctly corresponds to the request path.<br/><br/>
 *
 * All URIs returned start with a leading slash and should be assumed to be accessible directly under the incoming request's
 * hostname/IP.<br/><br/>
 *
 * In the example URIs documented on this class, the FOX engine is assumed to be running under a context of "englocal" with
 * an app mnem of "foxapp".
 */
public interface RequestURIBuilder {

  /**
   * Sets a parameter which will be appended to the URI query string, or merged into a PathParamTemplate, depending on
   * which of the buildXXX methods is invoked. Parameters will always be correctly encoded by the URIBuilder.<br/><br/>
   *
   * If this URIBuilder has been marked as stateless, this method will raise an exception.
   *
   * @param pParamName Name of paramter to set.
   * @param pParamValue Value of parameter.
   * @return Self reference for method chaining.
   */
  RequestURIBuilder setParam(String pParamName, String pParamValue);

  /**
   * Sets multiple parameters at the same time, in the same way as repeated calls to {@link #setParam(String, String)} would.
   * The map's contents is copied.
   * @param pParamMap Map containing multiple parameters (names to values).
   * @return Self reference for method chaining.
   */
  RequestURIBuilder setParams(Map<String, String> pParamMap);

  /**
   * Builds a URI for accessing a resource stored directly under the current context of the servlet container. <br/><br/>
   *
   * E.g. for the servlet resource <tt>/jquery/jqueryui.js</tt>, this method returns <tt>/englocal/jquery/jqueryui.js</tt>.<br/><br/>
   *
   * Note: a future enhancement to the FOX engine could involve registering known context resources, thereby allowing this method
   * to be consolidated with {@link #buildStaticResourceURI}.
   *
   * @param pResourcePath Path to the resource, including the leading slash.
   * @return Path to a context resource.
   */
  String buildContextResourceURI(String pResourcePath);

  /**
   * Builds a URI for accessing a resource via the static servlet. Static resources are any resources defined in FoxComponents.xml,
   * or defined in one of the request app's component tables. The builder should include the current app mnem in the generated
   * URI if it is known. <br/><br/>
   *
   * E.g. for the static resource <tt>css/style.css</tt>, this method returns <tt>/englocal/static/foxapp/css/style.css</tt>.<br/><br/>
   *
   * This method never appends parameters.
   *
   * @param pResourcePath The resource "path", typically just its identifer. For instance "js/fox.js" or "help.htm". This
   *                      should not include any leading slashes.
   * @return Path to a resource to be served out by the static servlet.
   */
  String buildStaticResourceURI(String pResourcePath);

  /**
   * Builds a URI for accessing a resource via the static servlet, OR just the given URI if it is determined to be "fixed".
   * See {@link #isFixedURI(String)} for a description of a "fixed" URI.<br/><br/>
   *
   * This method should be used when the URI is provided by a module developer and may point to an external resource,
   * or a relative resource path which the engine does not have knowledge of. See {@link #buildStaticResourceURI} for a
   * description of how static resource URIs are resolved in the case that the given URI is not a fixed URI. <br/><br/>
   *
   * This method never appends parameters.
   *
   * @param pResourcePathOrFixedURI Path to a static resource, e.g. "help.htm", or a fixed path such as "http://www.google.com"
   *                                or "#anchor".
   * @return The given URI, if it is a "fixed" URI, or a static resource path.
   */
  String buildStaticResourceOrFixedURI(String pResourcePathOrFixedURI);

  /**
   * Tests if the given URI is "fixed" based on its prefix. Fixed URIs start with one of the following sequences:
   *
   * <ul>
   *   <li>http://</li>
   *   <li>https://</li>
   *   <li>/</li>
   *   <li>./</li>
   *   <li>../</li>
   *   <li>#</li>
   * </ul>
   *
   * @param pURI URI to test.
   * @return True if the URI is "fixed" and is not considered to be a reference to an internal resource.
   */
  boolean isFixedURI(String pURI);

  /**
   * Builds a URI for accessing a temp resource. This method never appends parameters.
   * @param pTempResource Temp resource to generate the URI for.
   * @param pReadableName A readable suffix to append to the URI. This can help browsers determine content types etc.
   * @return URI for accessing a temp resource.
   */
  String buildTempResourceURI(TempResource<?> pTempResource, String pReadableName);

  /**
   * Builds a URI for accessing a servlet at the given path. Any parameters are appended to the URI query string. The
   * servlet path provided should be a constant stored on a servlet class.<br/><br/>
   *
   * E.g. for the servlet path <tt>download</tt> and a param <tt>id = 1</tt>, this method returns <tt>/englocal/download?id=1</tt><br/><br/>
   *
   * @param pServletPath Path to a servlet, not including any slashes.
   * @return URI for accessing a servlet.
   */
  String buildServletURI(String pServletPath);

  /**
   * Builds a URI for accessing a servlet at the given path. Parameters are first merged into the given PathParamTemplate.
   * Any remaining parameters are added to the URI's query string. The servlet path provided should be a constant stored
   * on a servlet class.<br/><br/>
   *
   * E.g. for the servlet path <tt>download</tt>, a param <tt>filename = hello.txt</tt>, and template of <tt>/get/{filename}</tt>,
   * this method returns <tt>/englocal/download/get/hello.txt<br/><br/>.
   *
   * @param pServletPath Path to a servlet, not including any slashes.
   * @param pPathParamTemplate ParamTemplate to be filled in and appended to the URI.
   * @return URI for accessing a servlet.
   */
  String buildServletURI(String pServletPath, PathParamTemplate pPathParamTemplate);

  /**
   * Builds a URI for accessing a bang handler, based on the BangHandler's alias. Parameters are appended to the URI's query string.
   *
   * E.g. for the bang handler <tt>FLUSH</tt>, this method returns <tt>/englocal/handle/!FLUSH</tt><br/><br/>
   *
   * @param pBangHandler BangHandler to access.
   * @return URI for a BangHandler.
   */
  String buildBangHandlerURI(BangHandler pBangHandler);

  /**
   * Builds a URI for accessing a {@link net.foxopen.fox.entrypoint.ws.WebService}. String arguments should be constants
   * defined on their respective classes. This signature does not use a PathParamTemplate - any parameters in the URI builder
   * are appended to the URI's query string.<br/><br/>
   *
   * E.g. for a category of <tt>engine</tt>, service of <tt>alive</tt> and endpoint of <tt>check</tt>, this method returns
   * <tt>/englocal/ws/rest/engine/alive/check</tt>.
   *
   * @param pCategoryName WebServiceCategory name.
   * @param pWebServiceName WebService name.
   * @param pEndPointName EndPoint name.
   * @return URI for accessing the given WebService.
   */
  String buildWebServiceURI(String pCategoryName, String pWebServiceName, String pEndPointName);

  /**
   * Builds a URI for accessing a {@link net.foxopen.fox.entrypoint.ws.WebService}. String arguments should be constants
   * defined on their respective classes. The PathParamTemplate is filled in and appended to the end of the URI based on
   * the parameters provided to this RequestURIBuilder.<br/><br/>
   *
   * E.g. for a category of <tt>engine</tt>, service of <tt>alive</tt> and endpoint of <tt>check</tt>, with a template of
   * <tt>/{id}</tt> and param map <tt>id => 123</tt>, this method returns <tt>/englocal/ws/rest/engine/alive/check/123</tt>.
   *
   * @param pCategoryName WebServiceCategory name.
   * @param pWebServiceName WebService name.
   * @param pEndPointName EndPoint name.
   * @param pPathParamTemplate PathParamTemplate to append to end of URI.
   * @return URI for accessing the given WebService.
   */
  String buildWebServiceURI(String pCategoryName, String pWebServiceName, String pEndPointName, PathParamTemplate pPathParamTemplate);

  /**
   * Builds a URI for accessing an image. If the provided URI identifies a composite image, the URI to a composite image
   * is returned. Otherwise, the image is treated as a static resource - see {@link #buildStaticResourceURI(String)}.<br/><br/>
   *
   * This method never appends parameters.
   *
   * @param pImageURI URI to a static image or composite image.
   * @return URI for accessing the image.
   */
  String buildImageURI(String pImageURI);
}
