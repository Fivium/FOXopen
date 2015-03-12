package net.foxopen.fox.entrypoint.servlets;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import net.foxopen.fox.App;
import net.foxopen.fox.ComponentText;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxRequestHttp;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.FoxResponseCHARStream;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.FlushBangHandler;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilderImpl;
import net.foxopen.fox.ex.ExInternal;
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

  public static final String SERVLET_PATH = "error";

  public static final String ERROR_REF_ATTRIBUTE = "error_ref";
  public static final String TRACK_ID_ATTRIBUTE = "track";
  public static final String THREAD_ID_ATTRIBUTE = "thread";
  public static final String FIELDSET_ID_ATTRIBUTE = "fieldset";

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
      lFoxRequest.setCurrentFieldSet(lFoxRequest.getParameter(THREAD_ID_ATTRIBUTE), lFoxRequest.getParameter(FIELDSET_ID_ATTRIBUTE));
    }

    respondWithErrorPage(lFoxRequest, lFoxRequest.getParameter(ERROR_REF_ATTRIBUTE),
      lFoxRequest.getParameter(TRACK_ID_ATTRIBUTE),
      TrackUtils.getRootErrorStack(XFUtil.nvl(lFoxRequest.getParameter(TRACK_ID_ATTRIBUTE))));
  }

  /**
   * Respond to a pFoxRequest with an error page, branded is possible.
   *
   * @param pFoxRequest Request to respond to
   * @param pErrorRef Error reference to display on screen
   * @param pTrackID Track ID to use when getting track information
   * @param pStackTrace Stack trace of the error to display
   */
  public static void respondWithErrorPage(FoxRequest pFoxRequest, String pErrorRef, String pTrackID, String pStackTrace) {
    try {
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
    catch (Throwable fatal) {
      if (FoxGlobals.getInstance().canShowStackTracesOnError()) {
        throw new ExInternal("Fox encountered an error while generating the error page", fatal);
      }
      else {
        FoxResponseCHAR lFoxResponse = new FoxResponseCHAR("text/html",new StringBuffer("<html><head><title>Internal Error</title></head><body><h1>An internal error has occurred</h1><p>Please contact support</p></body></html>"), 0);
        lFoxResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        lFoxResponse.respond(pFoxRequest);
      }
    }
  }
}
