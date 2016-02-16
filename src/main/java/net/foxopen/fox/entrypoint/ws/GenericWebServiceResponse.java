package net.foxopen.fox.entrypoint.ws;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.dom.DOM;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * WebService response which can convert a generic property map to either XML or JSON, depending on what the client requested.
 * EndPoints are encouraged to use this when responding to WebService requests as it gives the client flexibility when it
 * interacts with the web service.<br><br>
 *
 * JSON conversion of the property map is handled by the JSONSimple library.<br><br>
 *
 * XML conversion is not yet implemented. TODO PN documentation when it is. JIRA FOXRD-450.
 */
public class GenericWebServiceResponse
extends WebServiceResponse {

  private final Map<String, ?> mPropertyMap;
  private final int mStatusCode;

  public GenericWebServiceResponse(Map<String, ?> pPropertyMap) {
    mPropertyMap = pPropertyMap;
    mStatusCode = HttpServletResponse.SC_OK;
  }

  public GenericWebServiceResponse(Map<String, ?> pPropertyMap, int pStatusCode) {
    mPropertyMap = pPropertyMap;
    mStatusCode = pStatusCode;
  }

  @Override
  boolean isTypeSupported(Type pType) {
    //This response object supports all response types
    return true;
  }

  @Override
  FoxResponse generateResponse(FoxRequest pFoxRequest, Type pType) {

    if(pType == Type.JSON) {
      JSONObject lJSONContainer = new JSONObject();
      lJSONContainer.put("status", "ok");
      lJSONContainer.put("result", mPropertyMap);

      FoxResponseCHAR lResponse = new FoxResponseCHAR(JSON_CONTENT_TYPE, new StringBuffer(lJSONContainer.toJSONString()), 0L);
      lResponse.setStatus(mStatusCode);
      return lResponse;
    }
    else {
      DOM lDOM = DOM.createDocument("FOXWebServiceResponse");
      lDOM.addElem("status", "ok");
      DOM lResultDOM = lDOM.addElem("result");
      for(Map.Entry<String, ?> lEntry : mPropertyMap.entrySet()) {
        //TODO safer element naming
        lResultDOM.addElem(lEntry.getKey(), lEntry.getValue().toString());
      }

      FoxResponseCHAR lResponse = new FoxResponseCHAR(XML_CONTENT_TYPE, new StringBuffer(lDOM.outputDocumentToString(false)), 0L);
      lResponse.setStatus(mStatusCode);
      return lResponse;
    }
  }
}
