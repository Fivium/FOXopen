package net.foxopen.fox.entrypoint.ws;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.ResponseErrorHandler;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * ResponseErrorHandler which serves an error message in the correct format for a WebService request.
 */
public class WebServiceResponseErrorHandler
implements ResponseErrorHandler {

  private final WebServiceResponse.Type mResponseType;

  WebServiceResponseErrorHandler(WebServiceResponse.Type pResponseType) {
    mResponseType = pResponseType;
  }

  private WebServiceResponse generateErrorResponse(Throwable pError, String pErrorRef) {

    //Suppress stack traces on production
    //TODO PN only for external requests, internal/token should be ok to serve out the stack
    String lStackTrace;
    if(FoxGlobals.getInstance().canShowStackTracesOnError()) {
      lStackTrace = XFUtil.getJavaStackTraceInfo(pError);
    }
    else {
      lStackTrace = "Not available";
    }

    if(mResponseType == WebServiceResponse.Type.JSON) {
      JSONObject lErrorDetails = new JSONObject();
      lErrorDetails.put("message", pError.getMessage());
      lErrorDetails.put("stackTrace", lStackTrace);
      lErrorDetails.put("reference", pErrorRef);

      JSONObject lJSONContainer = new JSONObject();
      lJSONContainer.put("status", "error");
      lJSONContainer.put("errorDetails", lErrorDetails);

      return new JSONWebServiceResponse(lJSONContainer, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    else {
      DOM lErrorXML = DOM.createDocument("web-service-response");
      lErrorXML.addElem("status", "error");
      lErrorXML.addElem("error-details")
        .addElem("message", pError.getMessage()).getParentOrNull()
        .addElem("stack-trace", lStackTrace)
        .addElem("reference", pErrorRef);

      return new XMLWebServiceResponse(lErrorXML, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void handleError(FoxRequest pFoxRequest, Throwable pError, String pErrorRef) {

    //Note: this handler may cause an additional error if it is invoked when the response has already been sent
    //This indicates a severe error (probably caused by DB commit failing or similar) and there is not much we can do about it.

    FoxResponse lFoxResponse = generateErrorResponse(pError, pErrorRef).generateResponse(pFoxRequest, mResponseType);

    //Only attempt to change a header if we haven't started responding yet
    if(!pFoxRequest.getHttpResponse().isCommitted()) {
      //TODO NP/PN - correct error codes etc
      lFoxResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    lFoxResponse.respond(pFoxRequest);
  }
}
