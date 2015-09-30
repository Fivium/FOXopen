package net.foxopen.fox.entrypoint.servlets;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import net.foxopen.fox.App;
import net.foxopen.fox.ComponentText;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxRequestHttp;
import net.foxopen.fox.FoxResponseCHARStream;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.FlushBangHandler;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.ResponseErrorHandler;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilderImpl;
import net.foxopen.fox.module.fieldset.FieldSetCookieManager;
import net.foxopen.fox.track.ShowTrackBangHandler;
import net.foxopen.fox.track.TrackUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;


public class ErrorServlet extends HttpServlet {
  private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();

  private static final String ERROR_RESPONSE_HANDLER_ATTRIBUTE_NAME = "net.foxopen.fox.entrypoint.servlets.ErrorServlet.ERROR_RESPONSE_HANDLER";

  public static final String SERVLET_PATH = "error";

  public static final String ERROR_REF_ATTRIBUTE = "error_ref";
  public static final String TRACK_ID_ATTRIBUTE = "track";
  public static final String THREAD_ID_ATTRIBUTE = "thread";
  public static final String FIELDSET_ID_ATTRIBUTE = "fieldset";

  /**
   * Sets a specialised ResponseErrorHandler on the given FoxRequest. This should be invoked as soon as the request processor
   * is aware of how it is responding to the request. The given ResponseErrorHandler should be able to handle an error in a
   * contextual manner. E.g. for streaming HTML responses, the handler should write a JS redirect to the error page, whereas
   * a WebService response should contain JSON/XML information about the error. If a ResponseErrorHandler is set on a request,
   * FOX will use it to generate a response to any error which is caught by the ErrorServlet. If no ResponseErrorHandler is set
   * on the request, FOX sends its default HTML error page.
   *
   * @param pFoxRequest Request to set the error handler for.
   * @param pErrorHandler Handler which will deal with any errors caught by the ErrorServlet.
   */
  public static void setResponseErrorHandlerForRequest(FoxRequest pFoxRequest, ResponseErrorHandler pErrorHandler) {
    pFoxRequest.getHttpRequest().setAttribute(ERROR_RESPONSE_HANDLER_ATTRIBUTE_NAME, pErrorHandler);
  }

  @Override
  public final void init(ServletConfig pServletConfig) throws ServletException {
    super.init(pServletConfig);
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
    FoxRequest lFoxRequest = new FoxRequestHttp(pRequest, pResponse);

    // Reset the fieldset ID so that we can navigate back
    if(!XFUtil.isNull(lFoxRequest.getParameter(THREAD_ID_ATTRIBUTE)) && !XFUtil.isNull(lFoxRequest.getParameter(FIELDSET_ID_ATTRIBUTE))) {
      FieldSetCookieManager
        .createForAdHocRequest(lFoxRequest, lFoxRequest.getParameter(THREAD_ID_ATTRIBUTE), lFoxRequest.getParameter(FIELDSET_ID_ATTRIBUTE))
        .setCurrentFieldSet();
    }

    respondWithErrorPage(lFoxRequest, lFoxRequest.getParameter(ERROR_REF_ATTRIBUTE),
      lFoxRequest.getParameter(TRACK_ID_ATTRIBUTE),
      TrackUtils.getRootErrorStack(XFUtil.nvl(lFoxRequest.getParameter(TRACK_ID_ATTRIBUTE))));
  }

  /**
   * Responds to the given request with an error response, either based on the request's ErrorResponseHandler if it has one,
   * or the default HTML response if not. Consumers must catch and handle fatal errors from this method, in the event that the
   * error handling code itself fails.
   *
   * @param pFoxRequest Request on which an error has occurred.
   * @param pError The root error which occurred.
   * @param pErrorRef Error reference assigned by the error handler/logger.
   * @param pTrackID Optional Track ID for the request.
   */
  public static void respondWithErrorResponse(FoxRequest pFoxRequest, Throwable pError, String pErrorRef, String pTrackID) {

    ResponseErrorHandler lErrorHandler = (ResponseErrorHandler) pFoxRequest.getHttpRequest().getAttribute(ERROR_RESPONSE_HANDLER_ATTRIBUTE_NAME);
    if (lErrorHandler != null) {
      //The request has been provided with a dedicated error handler - use this if we can, as it will be able to provide a more contextual error response
      lErrorHandler.handleError(pFoxRequest, pError, pErrorRef);
    }
    else {
      //No explicit error handler was set, so generate the default HTML error page
      respondWithErrorPage(pFoxRequest, pErrorRef, pTrackID, XFUtil.getJavaStackTraceInfo(pError));
    }
  }

  /**
   * Respond to a pFoxRequest with an error page, branded if possible.
   *
   * @param pFoxRequest Request to respond to
   * @param pErrorRef Error reference to display on screen
   * @param pTrackID Optional Track ID to use when getting track information
   * @param pStackTrace Stack trace of the error to display
   */
  public static void respondWithErrorPage(FoxRequest pFoxRequest, String pErrorRef, String pTrackID, String pStackTrace) {

    FoxResponseCHARStream lFoxResponse = new FoxResponseCHARStream("text/html; charset=UTF-8", pFoxRequest, 0, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    Map<String, Object> lTemplateVars = new HashMap<>();
    Mustache lErrorTemplate;
    App lRequestApp = null;

    // Get app-specific error page template if engine initialised
    if(FoxGlobals.getInstance().isEngineInitialised()) {
      try {
        lRequestApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem((String) pFoxRequest.getHttpRequest().getAttribute(EntryPointServlet.REQUEST_ATTRIBUTE_APP_MNEM), true);
        String lErrorComponentName = lRequestApp.getAppMnem() + "/" + lRequestApp.getErrorComponentName();
        ComponentText lErrorPage = (ComponentText) ComponentManager.getComponent(lErrorComponentName);
        lErrorTemplate = MUSTACHE_FACTORY.compile(new StringReader(lErrorPage.getText().toString()), lErrorComponentName);
      }
      catch (Throwable th) {
        lErrorTemplate = ComponentManager.getInternalErrorComponent();
        if (FoxGlobals.getInstance().canShowStackTracesOnError()) {
          lTemplateVars.put("InternalStackTrace", XFUtil.getJavaStackTraceInfo(th));
        }
      }
    }
    else {
      lErrorTemplate = ComponentManager.getInternalErrorComponent();
    }

    try {
      RequestURIBuilder lURIBuilder = RequestURIBuilderImpl.createFromFoxRequest(pFoxRequest);

      lTemplateVars.put("ErrorRef", pErrorRef);
      lTemplateVars.put("StaticResourceURI", StaticServlet.getURIWithAppMnem(lURIBuilder, lRequestApp != null ? lRequestApp.getMnemonicName() : ""));
      lTemplateVars.put("ContextResourceURI", lURIBuilder.buildContextResourceURI(""));
      lTemplateVars.put("FlushURL", lURIBuilder.buildBangHandlerURI(FlushBangHandler.instance()));
      if(!XFUtil.isNull(pTrackID)) {
        lTemplateVars.put("TrackURL", lURIBuilder.setParam(ShowTrackBangHandler.TRACK_ID_PARAM_NAME, pTrackID).buildBangHandlerURI(ShowTrackBangHandler.instance()));
      }
      lTemplateVars.put("ShowDevInfo", FoxGlobals.getInstance().canShowStackTracesOnError());
      // Show developer information if not in production mode
      if (FoxGlobals.getInstance().canShowStackTracesOnError()) {
        lTemplateVars.put("ErrorStackTrace", pStackTrace);
      }
    }
    catch (Exception lInternalError) {
      if (FoxGlobals.getInstance().canShowStackTracesOnError()) {
        lTemplateVars.put("InternalStackTrace", XFUtil.getJavaStackTraceInfo(lInternalError));
      }
    }

    lErrorTemplate.execute(lFoxResponse.getWriter(), lTemplateVars);
  }
}
