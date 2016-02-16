package net.foxopen.fox.entrypoint.ws;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseByteStream;
import net.foxopen.fox.dom.DOM;

import javax.servlet.http.HttpServletResponse;

/**
 * Overriden WebService response which will return the exact XML object specified. Only use this object if you need
 * to control the exact XML returned by an EndPoint. Otherwise you should rely on the generic property map converter in
 * {@link GenericWebServiceResponse}. Requests which have asked for a JSON response type will be served an error if this
 * object is returned from the EndPoint.
 */
public class XMLWebServiceResponse
extends WebServiceResponse {

  private final DOM mXML;
  private final int mStatusCode;

  public XMLWebServiceResponse(DOM pXML) {
    mXML = pXML;
    mStatusCode = HttpServletResponse.SC_OK;
  }

  public XMLWebServiceResponse(DOM pXML, int pStatusCode) {
    mXML = pXML;
    mStatusCode = pStatusCode;
  }

  @Override
  boolean isTypeSupported(Type pType) {
    return pType == WebServiceResponse.Type.XML;
  }

  @Override
  FoxResponse generateResponse(FoxRequest pFoxRequest, Type pType) {
    FoxResponseByteStream lFoxResponse = new FoxResponseByteStream(XML_CONTENT_TYPE, pFoxRequest, 0L, mStatusCode);
    mXML.outputDocumentToOutputStream(lFoxResponse.getHttpServletOutputStream(), false);
    return lFoxResponse;
  }
}
