package net.foxopen.fox.entrypoint.engine;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.auth.StandardAuthenticationContext;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.ParamsDOMUtils;
import net.foxopen.fox.entrypoint.servlets.FoxMainServlet;
import net.foxopen.fox.entrypoint.ws.EndPoint;
import net.foxopen.fox.entrypoint.ws.GenericWebServiceResponse;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.entrypoint.ws.WebService;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthDescriptor;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthType;
import net.foxopen.fox.entrypoint.ws.WebServiceResponse;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.ThreadProperty;
import net.foxopen.fox.thread.XThreadBuilder;
import net.foxopen.fox.thread.stack.ModuleCall;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThreadWebService
implements WebService {

  @Override
  public String getName() {
    return "thread";
  }

  @Override
  public WebServiceAuthDescriptor getAuthDescriptor() {
    return new WebServiceAuthDescriptor(false, WebServiceAuthType.TOKEN);
  }

  @Override
  public String getRequiredConnectionPoolName(FoxRequest pFoxRequest) {
    return null;
  }

  @Override
  public Collection<? extends EndPoint> getAllEndPoints() {
    return Collections.singleton(new BootstrapEndPoint());
  }

  private class BootstrapEndPoint
  implements EndPoint {

    private static final String APP_MNEM_PARAM_NAME = "app_mnem";
    private static final String MODULE_PARAM_NAME = "module";
    private static final String ENTRY_THEME_PARAM_NAME = "entry_theme";
    private static final String WUS_ID_PARAM_NAME = "wus_id";
    private static final String PARAMS_DOM_PARAM_NAME = "params_dom";
    private static final String ENV_DOM_PARAM_NAME = "env_dom";
    private static final String EXIT_URI_PARAM_NAME = "exit_uri";
    private static final String ORPHAN_FLAG_PARAM_NAME = "orphan_flag";

    @Override
    public String getName() {
      return "bootstrap";
    }

    @Override
    public PathParamTemplate getPathParamTemplate() {
      return null;
    }

    @Override
    public Collection<String> getMandatoryRequestParamNames() {
      return Collections.emptySet();
    }

    @Override
    public Collection<String> getAllowedHttpMethods() {
      //TODO PN change to POST only
      return Arrays.asList("GET", "POST");
    }

    @Override
    public WebServiceResponse respond(RequestContext pRequestContext, Map<String, String> pParamMap, String pHttpMethod, WebServiceResponse.Type pDesiredResponseType) {

      String lAppMnem = XFUtil.nvl(pParamMap.get(APP_MNEM_PARAM_NAME), FoxGlobals.getInstance().getFoxEnvironment().getDefaultAppMnem());
      String lModuleName = pParamMap.get(MODULE_PARAM_NAME);
      String lEntryThemeName = pParamMap.get(ENTRY_THEME_PARAM_NAME);
      String lWUSId = pParamMap.get(WUS_ID_PARAM_NAME);

      //If a WUS ID was provided, bootstrap the auth context and verify we have a valid session
      StandardAuthenticationContext lAuthenticationContext;
      if(!XFUtil.isNull(lWUSId)) {
        lAuthenticationContext = new StandardAuthenticationContext(lWUSId);
        AuthenticationResult lAuthenticationResult = lAuthenticationContext.verifySession(pRequestContext, "Thread-WebService", "bootstrap");
        if (lAuthenticationResult.getCode() != AuthenticationResult.Code.VALID) {
          throw new ExInternal("Invalid or expired WUS ID specified: " + lAuthenticationResult.getMessage());
        }
      }
      else {
        lAuthenticationContext = new StandardAuthenticationContext(pRequestContext);
      }

      XThreadBuilder lXThreadBuilder = new XThreadBuilder(lAppMnem, lAuthenticationContext);

      lXThreadBuilder.setBooleanThreadProperty(ThreadProperty.Type.IS_RESUME_ALLOWED, true);
      lXThreadBuilder.setBooleanThreadProperty(ThreadProperty.Type.IS_SKIP_FOX_SESSION_CHECK, true);
      lXThreadBuilder.setStringThreadProperty(ThreadProperty.Type.EXIT_URI, XFUtil.nvl(pParamMap.get(EXIT_URI_PARAM_NAME)));

      if("true".equals(pParamMap.get(ORPHAN_FLAG_PARAM_NAME))) {
        lXThreadBuilder.setBooleanThreadProperty(ThreadProperty.Type.IS_ORPHAN, true);
      }

      StatefulXThread lNewXThread = lXThreadBuilder.createXThread(pRequestContext);
      try {
        //Construct new ModuleCall Builder with the target entry theme
        EntryTheme lEntryTheme = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(lAppMnem).getMod(lModuleName).getEntryTheme(lEntryThemeName);
        ModuleCall.Builder lModuleCallBuilder = new ModuleCall.Builder(lEntryTheme);

        //Parse params DOM if provided
        String lParamsDOMString = pParamMap.get(PARAMS_DOM_PARAM_NAME);
        if(!XFUtil.isNull(lParamsDOMString)) {
          try {
            lModuleCallBuilder.setParamsDOM(ParamsDOMUtils.paramsDOMFromXMLString(lParamsDOMString));
          }
          catch (Throwable th) {
            throw new ExInternal("Error parsing params DOM", th);
          }
        }

        //Parse env DOM if provided
        String lEnvDOMString = pParamMap.get(ENV_DOM_PARAM_NAME);
        if(!XFUtil.isNull(lEnvDOMString)) {
          try {
            //Builder will set correct root element name for the DOM
            lModuleCallBuilder.setEnvironmentDOM(DOM.createDocumentFromXMLString(lEnvDOMString));
          }
          catch (Throwable th) {
            throw new ExInternal("Error parsing env DOM", th);
          }
        }

        //Start the thread
        lNewXThread.startThread(pRequestContext, lModuleCallBuilder, false);
      }
      catch (ExUserRequest | ExServiceUnavailable | ExModule | ExApp  e) {
        throw new ExInternal("Failed to start thread", e);
      }

      Map<String, String> lResultMap = new HashMap<>();
      lResultMap.put("new_thread_id",  lNewXThread.getThreadId());
      lResultMap.put("entry_uri", FoxMainServlet.buildThreadResumeEntryURI(pRequestContext.createURIBuilder(), lNewXThread.getThreadId(), lNewXThread.getThreadAppMnem()));

      return new GenericWebServiceResponse(lResultMap);
    }
  }
}
