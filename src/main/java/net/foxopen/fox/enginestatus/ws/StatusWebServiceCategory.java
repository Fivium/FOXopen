package net.foxopen.fox.enginestatus.ws;

import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.StatusCategory;
import net.foxopen.fox.enginestatus.StatusDetail;
import net.foxopen.fox.entrypoint.ws.EndPoint;
import net.foxopen.fox.entrypoint.ws.GenericWebServiceResponse;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.entrypoint.ws.WebService;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthDescriptor;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthType;
import net.foxopen.fox.entrypoint.ws.WebServiceCategory;
import net.foxopen.fox.entrypoint.ws.WebServiceResponse;
import net.foxopen.fox.thread.RequestContext;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class StatusWebServiceCategory
implements WebServiceCategory {

  public static final String DETAIL_PATH_PARAM = "detailPath";

  public StatusWebServiceCategory() {
  }

  @Override
  public String getName() {
    return "status";
  }

  @Override
  public Collection<? extends WebService> getAllWebServices() {

    List<WebService> lWebServices = new ArrayList<>();
    for(StatusCategory lCategory : EngineStatus.instance().getAllCategories()) {
      lWebServices.add(new WebServiceImpl(lCategory));
    }

    return lWebServices;
  }

  private class WebServiceImpl implements WebService {

    private final StatusCategory mStatusCategory;

    WebServiceImpl(StatusCategory pStatusCategory) {
      mStatusCategory = pStatusCategory;
    }

    @Override
    public String getName() {
      return mStatusCategory.getMnem();
    }

    @Override
    public WebServiceAuthDescriptor getAuthDescriptor() {
      return new WebServiceAuthDescriptor(false, InternalAuthLevel.INTERNAL_ADMIN, WebServiceAuthType.INTERNAL);
    }

    @Override
    public String getRequiredConnectionPoolName() {
      return null;
    }

    @Override
    public Collection<? extends EndPoint> getAllEndPoints() {
      return Collections.singleton(new DetailEndPoint());
    }

    private class DetailEndPoint
    implements EndPoint {

      @Override
      public String getName() {
        return "detail";
      }

      @Override
      public PathParamTemplate getPathParamTemplate() {
        return null;
      }

      @Override
      public Collection<String> getMandatoryRequestParamNames() {
        return Collections.singleton(DETAIL_PATH_PARAM);
      }

      @Override
      public Collection<String> getAllowedHttpMethods() {
        return Collections.singleton("GET");
      }

      @Override
      public WebServiceResponse respond(RequestContext pRequestContext, Map<String, String> pParamMap, String pHttpMethod, WebServiceResponse.Type pDesiredResponseType) {

        StatusDetail lStatusDetail = WebServiceImpl.this.mStatusCategory.resolveDetail(pParamMap.get(DETAIL_PATH_PARAM));
        StringWriter lStringWriter = new StringWriter();
        lStatusDetail.getContent(lStringWriter);

        Map<String, ?> lResultMap = Collections.singletonMap("detail", lStringWriter.toString());
        return new GenericWebServiceResponse(lResultMap);
      }
    }
  }
}
