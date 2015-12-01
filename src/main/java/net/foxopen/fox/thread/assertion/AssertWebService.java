package net.foxopen.fox.thread.assertion;

import net.foxopen.fox.App;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.ws.EndPoint;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.entrypoint.ws.WebService;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthDescriptor;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthType;
import net.foxopen.fox.entrypoint.ws.WebServiceResponse;
import net.foxopen.fox.entrypoint.ws.XMLWebServiceResponse;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.thread.RequestContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * WebService which co-ordinates the running of assertion actions.
 */
public class AssertWebService
implements WebService {

  private static final String FILTER_PARAM_NAME = "moduleFilter";

  @Override
  public String getName() {
    return "assert";
  }

  @Override
  public WebServiceAuthDescriptor getAuthDescriptor() {
    return new WebServiceAuthDescriptor(true, WebServiceAuthType.TOKEN);
  }

  @Override
  public String getRequiredConnectionPoolName(FoxRequest pFoxRequest) {
    return null;
  }

  @Override
  public Collection<? extends EndPoint> getAllEndPoints() {
    return Arrays.asList(new AllEndPoint(), new ModuleEndPoint(), new CategoryEndPoint());
  }

  /**
   * Subclasses of this EndPoint can set up an AssertionRunner with varying filter configurations.
   */
  private static abstract class AbstractAssertEndPoint
  implements EndPoint {

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
      return Collections.singleton("GET");
    }


    @Override
    public WebServiceResponse respond(RequestContext pRequestContext, Map<String, String> pParamMap, String pHttpMethod, WebServiceResponse.Type pDesiredResponseType) {

      //Don't allow production servers to run asserts
      if (FoxGlobals.getInstance().isProduction()) {
        throw new ExInternal("Production instances cannot run assertions");
      }

      //Establish app to retrieve assertion modules from
      App lApp;
      String lAppMnem = XFUtil.nvl(pParamMap.get("appMnem"), FoxGlobals.getInstance().getFoxEnvironment().getDefaultAppMnem());
      try {
        lApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(lAppMnem);
      }
      catch (ExServiceUnavailable | ExApp  e) {
        throw new ExInternal("Failed to find app", e);
      }

      //Run assertions and return result as XML

      AssertionRunner lAssertionRunner = createAssertionRunner(lApp, pParamMap);

      AssertionReport lReport = lAssertionRunner.runAssertions(pRequestContext);

      return new XMLWebServiceResponse(lReport.asXML());
    }

    protected abstract AssertionRunner createAssertionRunner(App pApp, Map<String, String> pParamMap);
  }

  /**
   * Runs all assertion modules for an app. Supports a module name filter.
   */
  private static class AllEndPoint
  extends AbstractAssertEndPoint {

    @Override
    public String getName() {
      return "all";
    }


    @Override
    protected AssertionRunner createAssertionRunner(App pApp, Map<String, String> pParamMap) {
      return new AssertionRunner(pApp, XFUtil.nvl(pParamMap.get(FILTER_PARAM_NAME), "*"), "");
    }
  }

  /**
   * Runs assertions for a specific module name specified in the URL path.
   */
  private static class ModuleEndPoint
  extends AbstractAssertEndPoint {

    @Override
    public String getName() {
      return "module";
    }

    @Override
    public PathParamTemplate getPathParamTemplate() {
      return new PathParamTemplate("/{module_name}");
    }

    @Override
    protected AssertionRunner createAssertionRunner(App pApp, Map<String, String> pParamMap) {
      return new AssertionRunner(pApp, pParamMap.get("module_name"), "");
    }
  }

  /**
   * Runs assertions for a given test category name specified in the URL path. Module name filtering is also supported.
   */
  private static class CategoryEndPoint
  extends AbstractAssertEndPoint {

    @Override
    public String getName() {
      return "category";
    }

    @Override
    public PathParamTemplate getPathParamTemplate() {
      return new PathParamTemplate("/{category_name}");
    }

    @Override
    protected AssertionRunner createAssertionRunner(App pApp, Map<String, String> pParamMap) {
      return new AssertionRunner(pApp, XFUtil.nvl(pParamMap.get(FILTER_PARAM_NAME), "*"), pParamMap.get("category_name"));
    }
  }
}
