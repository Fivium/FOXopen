package net.foxopen.fox.entrypoint.ws;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import org.json.simple.JSONAware;

import javax.servlet.http.HttpServletResponse;

/**
 * Overriden WebService response which will return the exact JSON object specified. Only use this object if you need
 * to control the exact JSON returned by an EndPoint. Otherwise you should rely on the generic property map converter in
 * {@link GenericWebServiceResponse}. Requests which have asked for an XML response type will be served an error if this
 * object is returned from the EndPoint.
 */
public class JSONWebServiceResponse
extends WebServiceResponse {

  private final JSONAware mJSONObject;
  private final int mStatusCode;

  public JSONWebServiceResponse(JSONAware pJSONObject) {
    mJSONObject = pJSONObject;
    mStatusCode = HttpServletResponse.SC_OK;
  }

  JSONWebServiceResponse(JSONAware pJSONObject, int pStatusCode) {
    mJSONObject = pJSONObject;
    mStatusCode = pStatusCode;
  }

  @Override
  boolean isTypeSupported(Type pType) {
    return pType == Type.JSON;
  }

  @Override
  FoxResponse generateResponse(FoxRequest pFoxRequest, Type pType) {
    FoxResponseCHAR lResponse = new FoxResponseCHAR(JSON_CONTENT_TYPE, new StringBuffer(mJSONObject.toJSONString()), 0L);
    lResponse.setStatus(mStatusCode);
    return lResponse;
  }
}
