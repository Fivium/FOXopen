package net.foxopen.fox.entrypoint.ws;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;

import org.json.simple.JSONAware;

/**
 * Overriden WebService response which will return the exact JSON object specified. Only use this object if you need
 * to control the exact JSON returned by an EndPoint. Otherwise you should rely on the generic property map converter in
 * {@link GenericWebServiceResponse}. Requests which have asked for an XML response type will be served an error if this
 * object is returned from the EndPoint.
 */
public class JSONWebServiceResponse
extends WebServiceResponse {

  private final JSONAware mJSONObject;

  public JSONWebServiceResponse(JSONAware pJSONObject) {
    mJSONObject = pJSONObject;
  }

  @Override
  boolean isTypeSupported(Type pType) {
    return pType == Type.JSON;
  }

  @Override
  FoxResponse generateResponse(FoxRequest pFoxRequest, Type pType) {
    return new FoxResponseCHAR(JSON_CONTENT_TYPE, new StringBuffer(mJSONObject.toJSONString()), 0L);
  }
}
