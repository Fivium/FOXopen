package net.foxopen.fox.entrypoint.ws;

import com.google.common.base.Joiner;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.InternalAuthentication;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.StatusDestination;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusTable;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.UnauthenticatedFoxSession;
import net.foxopen.fox.entrypoint.engine.EngineWebServiceCategory;
import net.foxopen.fox.entrypoint.servlets.EntryPointServlet;
import net.foxopen.fox.ex.ExAlreadyHandled;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This servlet is responsible for handling the delegation of web service requests to the appropriate EndPoint. EndPoints
 * are defined on WebServices, which are in turn defined on WebServiceCategories, which are registered on this servlet.
 * This servlet resolves the correct EndPoint based on the request URI, validates the request, delegates the request to
 * the EndPoint, then performs any required conversions so the client is sent the correct response type.
 */
public class WebServiceServlet
extends EntryPointServlet {

  public static final String RESPONSE_TYPE_PARAM_NAME = "responseType";

  private static final String MAIN_CONNECTION_NAME = "WEBSERVICE";

  private static final String SERVICE_TYPE_REST = "rest";

  private static final Map<String, WebServiceCategory> gCategoryMap = new TreeMap<>();

  //Forces known WebService categories to be registered so we don't have to wait for their class to be loaded (which may never happen)
  static {
    registerWebServiceCategory(new EngineWebServiceCategory());
  }

  static {
    EngineStatus.instance().registerStatusProvider(new WebServiceStatusProvider());
  }

  @Override
  protected String getContextUConInitialConnectionName() {
    return MAIN_CONNECTION_NAME;
  }

  @Override
  protected String getConnectionPoolName(FoxRequest pFoxRequest) {
    WebService lWebService = resolveWebService(new StringBuilder(pFoxRequest.getHttpRequest().getPathInfo()));
    return lWebService.getRequiredConnectionPoolName();
  }

  @Override
  public FoxSession establishFoxSession(RequestContext pRequestContext) {
    return UnauthenticatedFoxSession.create();
  }

  @Override
  protected String getTrackElementName(RequestContext pRequestContext) {
    return "WebServiceRequest";
  }

  @Override
  public void processGet(RequestContext pRequestContext) {
    processHttpRequest(pRequestContext);
  }

  @Override
  public void processPost(RequestContext pRequestContext) {
    processHttpRequest(pRequestContext);
  }

  @Override
  public void processPut(RequestContext pRequestContext) {
    processHttpRequest(pRequestContext);
  }

  @Override
  public void processDelete(RequestContext pRequestContext) {
    processHttpRequest(pRequestContext);
  }

  @Override
  public void processTrace(RequestContext pRequestContext) {
    processHttpRequest(pRequestContext);
  }

  @Override
  public void processHead(RequestContext pRequestContext) {
    processHttpRequest(pRequestContext);
  }

  @Override
  public void processOptions(RequestContext pRequestContext) {
    processHttpRequest(pRequestContext);
  }

  public synchronized static void registerWebServiceCategory(WebServiceCategory pCategory) {
    String lCategoryName = pCategory.getName();
    if(gCategoryMap.containsKey(lCategoryName)) {
      throw new ExInternal("WebServiceCategory " + lCategoryName + " already registered");
    }
    else {
      gCategoryMap.put(lCategoryName, pCategory);
    }
  }

  private WebService resolveWebService(StringBuilder pUriPath) {

    String lServiceType = XFUtil.pathPopHead(pUriPath, true);

    if(!SERVICE_TYPE_REST.equals(lServiceType)) {
      throw new ExInternal("Only 'rest' request type is supported at this time");
    }

    String lCategoryName = XFUtil.pathPopHead(pUriPath, true);
    if(XFUtil.isNull(lCategoryName)) {
      throw new ExInternal("A web service category must be specified in the URI");
    }
    WebServiceCategory lCategory = gCategoryMap.get(lCategoryName);
    if(lCategory == null) {
      throw new ExInternal("WebService category " + lCategoryName + " not registered");
    }

    Track.info("WebServiceCategoryIdentified", lCategory.getName());

    String lWebServiceName = XFUtil.pathPopHead(pUriPath, true);
    if(XFUtil.isNull(lWebServiceName)) {
      throw new ExInternal("A web service name must be specified in the URI");
    }
    //Search for the given web service

    for(WebService lWebService : lCategory.getAllWebServices()) {
      if(lWebServiceName.equals(lWebService.getName())) {
        Track.info("WebServiceIdentified", lWebService.getName());
        return lWebService;
      }
    }

    //No web service found
    throw new ExInternal("WebService " + lWebServiceName + " not found in category " + lCategoryName);
  }

  private EndPoint resolveEndPoint(WebService pWebService, StringBuilder pUriPath) {

    String lEndPointName = XFUtil.pathPopHead(pUriPath, true);
    if(XFUtil.isNull(lEndPointName)) {
      throw new ExInternal("An endpoint name must be specified in the URI");
    }

    Collection<? extends EndPoint> lEndPoints = pWebService.getAllEndPoints();
    for(EndPoint lEndPoint : lEndPoints) {
      if(lEndPointName.equals(lEndPoint.getName())){
        Track.info("EndPointIdentified", lEndPoint.getName());
        return lEndPoint;
      }
    }

    //Not found
    throw new ExInternal("Endpoint " + lEndPointName + " not known by service " + pWebService.getName());
  }

  private boolean authenticate(FoxRequest pFoxRequest, WebService pWebService) {

    WebServiceAuthDescriptor lAuthDescriptor = pWebService.getAuthDescriptor();

    if(lAuthDescriptor.authenticationRequired()) {

      String lSecurityToken = pFoxRequest.getParameter("security_token");

      //Check a security token first
      if(lAuthDescriptor.isAuthTypeAllowed(WebServiceAuthType.TOKEN) && !XFUtil.isNull(lSecurityToken)) {
        return FoxGlobals.getInstance().getEngineSecurityToken().equals(lSecurityToken);
      }
      else if(lAuthDescriptor.isAuthTypeAllowed(WebServiceAuthType.INTERNAL)) {
        //If internal access is required we need to check HTTP auth headers
        return InternalAuthentication.instance().authenticate(pFoxRequest, lAuthDescriptor.getRequiredInternalAuthLevel());
      }
      else {
        if(lAuthDescriptor.isAuthTypeAllowed(WebServiceAuthType.TOKEN) && XFUtil.isNull(lSecurityToken)) {
          throw new ExInternal("Request missing mandatory security_token parameter");
        }
        else {
          throw new ExInternal("Don't know how to authenticate this auth descriptor");
        }
      }
    }
    else {
      return true;
    }
  }

  private Map<String, String> parseParameterMap(HttpServletRequest pRequest, String pUriPath, EndPoint pEndPoint) {

    Map<String, String> lParamMap = new HashMap<>();
    PathParamTemplate lParamTemplate = pEndPoint.getPathParamTemplate();
    if(lParamTemplate != null) {
      lParamMap.putAll(lParamTemplate.parseURI(pUriPath));
    }

    //Validate http params
    Set<String> lHttpParamNames = pRequest.getParameterMap().keySet();
    Collection<String> lMandatoryRequestParamNames = pEndPoint.getMandatoryRequestParamNames();

    if(lMandatoryRequestParamNames == null) {
      lMandatoryRequestParamNames = Collections.emptySet();
    }

    Set<String> lMandParamNames = new HashSet<>(lMandatoryRequestParamNames);
    if(!lHttpParamNames.containsAll(lMandatoryRequestParamNames)) {
      lMandParamNames.removeAll(lHttpParamNames);
      throw new ExInternal("Request missing mandatory parameter(s): " + Joiner.on(", ").join(lMandParamNames));
    }

    for(Map.Entry<String, String[]> lParam : pRequest.getParameterMap().entrySet()) {

      String lParamName = lParam.getKey();
      String[] lParamValue = lParam.getValue();

      if(lParamMap.containsKey(lParamName)) {
        //Check not a duplicate from the path params
        throw new ExInternal("Parameter " + lParamName + " was provided in both the path and the request parameters");
      }
      else if(lParamValue.length > 1) {
        //Check not a duplicate in the params themselves
        throw new ExInternal("Parameter " + lParamName + " was specified multiple times");
      }
      else {
        lParamMap.put(lParamName, lParamValue[0]);
      }
    }

    return lParamMap;
  }

  private WebServiceResponse generateErrorResponse(WebServiceResponse.Type pResponseType, Throwable pError) {

    //Suppress stack traces on production
    //TODO PN only for external requests, internal/token should be ok to serve out the stack
    String lStackTrace;
    if(FoxGlobals.getInstance().canShowStackTracesOnError()) {
      lStackTrace = XFUtil.getJavaStackTraceInfo(pError);
    }
    else {
      lStackTrace = "Not available";
    }

    if(pResponseType == WebServiceResponse.Type.JSON) {
      JSONObject lErrorDetails = new JSONObject();
      lErrorDetails.put("message", pError.getMessage());
      lErrorDetails.put("stack-trace", lStackTrace);

      JSONObject lJSONContainer = new JSONObject();
      lJSONContainer.put("status", "error");
      lJSONContainer.put("error-details", lErrorDetails);

      return new JSONWebServiceResponse(lJSONContainer);
    }
    else {
      DOM lErrorXML = DOM.createDocument("web-service-response");
      lErrorXML.addElem("status", "error");
      lErrorXML.addElem("error-details")
        .addElem("message", pError.getMessage()).getParentOrNull()
        .addElem("stack-trace", lStackTrace);

      return new XMLWebServiceResponse(lErrorXML);
    }
  }

  public final void processHttpRequest(RequestContext pRequestContext) {

    FoxRequest lFoxRequest = pRequestContext.getFoxRequest();
    HttpServletRequest lHttpRequest = pRequestContext.getFoxRequest().getHttpRequest();

    FoxResponse lFoxResponse;
    //Default response type is JSON
    WebServiceResponse.Type lResponseType = WebServiceResponse.Type.JSON;
    try {
      //First thing we do as any error should be sent as the correct type
      String lResponseTypeParam = lHttpRequest.getParameter(RESPONSE_TYPE_PARAM_NAME);
      if(!XFUtil.isNull(lResponseTypeParam)) {
        lResponseType = WebServiceResponse.Type.fromParamString(lResponseTypeParam);
      }

      StringBuilder lUriPath = new StringBuilder(lHttpRequest.getPathInfo());

      WebService lWebService = resolveWebService(lUriPath);

      //Authenticate web service based on its auth descriptor
      boolean lAuthSuccess = authenticate(lFoxRequest, lWebService);

      if(lAuthSuccess) {
        try {
          EndPoint lEndPoint = resolveEndPoint(lWebService, lUriPath);

          //Validate http method being used
          if(!lEndPoint.getAllowedHttpMethods().isEmpty() && !lEndPoint.getAllowedHttpMethods().contains(lHttpRequest.getMethod())) {
            throw new ExInternal("Endpoint " + lEndPoint.getName() + " cannot respond to " + lHttpRequest.getMethod() + " requests");
          }

          //Parse params from request and URI
          Map<String, String> lParamMap = parseParameterMap(lHttpRequest, lUriPath.toString(), lEndPoint);

          //Generate a WS response object - note the response returned may not conform to the requested response type
          Track.pushInfo("WebServiceResponse");
          WebServiceResponse lWSResponse;
          try {
            lWSResponse = lEndPoint.respond(pRequestContext, lParamMap, lHttpRequest.getMethod(), lResponseType);
          }
          finally {
            Track.pop("WebServiceResponse");
          }

          //Ask the WS response for a FOX response (check that it conforms to the requested response type)
          if(lWSResponse.isTypeSupported(lResponseType)) {
            lFoxResponse = lWSResponse.generateResponse(lFoxRequest, lResponseType);
            lFoxResponse.respond(lFoxRequest);
          }
          else {
            throw new ExInternal("WebService " + lWebService.getName() + " EndPoint " + lEndPoint.getName() + " cannot provide responses for the " + lResponseType + " response type");
          }

          //Connection cleanup code

          //Validates all but MAIN transaction are committed
          pRequestContext.getContextUCon().closeAllRetainedConnections();

          //Commit the MAIN connection - commits all work done by web service
          pRequestContext.getContextUCon().commit(MAIN_CONNECTION_NAME);

          //This could throw an error but we're already committed everything - TODO what to do - need to report the error but likely to be a developer problem and not too serious
          pRequestContext.getContextUCon().popConnection(MAIN_CONNECTION_NAME);
        }
        catch(Throwable th) {
          pRequestContext.getContextUCon().rollbackAndCloseAll(true);
          throw th;
        }
      }
      else {
        //TODO improved auth failure handling
        throw new ExInternal("Failed to authenticate");
      }
    }
    catch (Throwable th) {
      lFoxResponse = generateErrorResponse(lResponseType, th).generateResponse(lFoxRequest, lResponseType);
      //TODO NP/PN - correct error codes etc
      if(!lFoxRequest.getHttpResponse().isCommitted()) {
        lFoxResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
      lFoxResponse.respond(lFoxRequest);

      //Ensure the error logging filter sees this error
      throw new ExAlreadyHandled("Error in WebService (reported to client)", th);
    }
  }

  private static class WebServiceStatusProvider
  implements StatusProvider {

    @Override
    public void refreshStatus(StatusDestination pDestination) {

      StatusTable lTable = pDestination.addTable("EndPoint Summary", "Category", "Service", "End Point", "URI Path Params", "Request Params", "Allowed methods", "Auth Type");
      lTable.setRowProvider(new StatusTable.RowProvider() {
        @Override
        public void generateRows(StatusTable.RowDestination pRowDestination) {
          for(WebServiceCategory lCategory : gCategoryMap.values()) {
            for(WebService lWebService : lCategory.getAllWebServices()) {
              for(EndPoint lEndPoint : lWebService.getAllEndPoints()) {
                pRowDestination.addRow()
                  .setColumn(lCategory.getName())
                  .setColumn(lWebService.getName())
                  .setColumn(lEndPoint.getName())
                  .setColumn(lEndPoint.getPathParamTemplate() != null ? lEndPoint.getPathParamTemplate().toString() : "")
                  .setColumn(Joiner.on("," ).join(lEndPoint.getMandatoryRequestParamNames()))
                  .setColumn(lEndPoint.getAllowedHttpMethods().size() == 0 ? "all" : Joiner.on("," ).join(lEndPoint.getAllowedHttpMethods()))
                  .setColumn(lWebService.getAuthDescriptor().toString());
              }
            }
          }
        }
      });
    }

    @Override
    public String getCategoryTitle() {
      return "Web Services";
    }

    @Override
    public String getCategoryMnemonic() {
      return "webServices";
    }

    @Override
    public boolean isCategoryExpandedByDefault() {
      return false;
    }
  }
}
