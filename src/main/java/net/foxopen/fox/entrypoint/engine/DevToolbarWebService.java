package net.foxopen.fox.entrypoint.engine;

import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.ws.EndPoint;
import net.foxopen.fox.entrypoint.ws.GenericWebServiceResponse;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.entrypoint.ws.WebService;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthDescriptor;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthType;
import net.foxopen.fox.entrypoint.ws.WebServiceResponse;
import net.foxopen.fox.entrypoint.ws.XMLWebServiceResponse;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class DevToolbarWebService
implements WebService {

  @Override
  public String getName() {
    return "devtoolbar";
  }

  @Override
  public WebServiceAuthDescriptor getAuthDescriptor() {
    return new WebServiceAuthDescriptor(true, InternalAuthLevel.INTERNAL_SUPPORT, WebServiceAuthType.INTERNAL);
  }

  @Override
  public String getRequiredConnectionPoolName() {
    return null;
  }

  @Override
  public Collection<? extends EndPoint> getAllEndPoints() {
    return Arrays.asList(
      new DOMViewEndPoint()
    );
  }

  private static class DOMViewEndPoint
  implements EndPoint {

    @Override
    public String getName() {
      return "dom";
    }

    @Override
    public PathParamTemplate getPathParamTemplate() {
      return new PathParamTemplate("/{thread_id}/{dom_name}");
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
      String lDOMName = pParamMap.get("dom_name");

      //TODO generic
      StatefulXThread lThread = StatefulXThread.getAndLockXThread(pRequestContext, pParamMap.get("thread_id"));
      try {
        if(lThread == null){
          return new GenericWebServiceResponse(Collections.singletonMap("error","thread not found"));
        }
        else {
          return new XMLWebServiceResponse(DOM.createDocumentFromXMLString("<TODO/>"));
        }
        //TODO other return types?

      }
      finally {
        StatefulXThread.unlockThread(pRequestContext, lThread);
      }
    }
  }
}
