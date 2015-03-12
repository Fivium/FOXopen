package net.foxopen.fox.logging;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.dom.DOM;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class BoomerangBangHandler
implements BangHandler {

  private static final BoomerangBangHandler INSTANCE = new BoomerangBangHandler();

  public static BoomerangBangHandler instance() {
    return INSTANCE;
  }

  public static final String REQUEST_ID_PARAM_NAME = "request_id";

  private BoomerangBangHandler() { }

  @Override
  public String getAlias() {
    return "BOOM";
  }

  @Override
  public Collection<String> getParamList() {
    return Collections.singleton(REQUEST_ID_PARAM_NAME);
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.NONE;
  }

  @Override
  public boolean isDevAccessAllowed() {
    return true;
  }

  @Override
  public FoxResponse respond(FoxRequest pFoxRequest) {

    //Bang handler servlet should have validated this is not null
    String lRequestId = pFoxRequest.getParameter(REQUEST_ID_PARAM_NAME);

    String lUserExperienceTimeString = pFoxRequest.getParameter("t_done");
    long lUserExperienceTimeMS = -1;
    if (lUserExperienceTimeString != null) {
      lUserExperienceTimeMS = Long.parseLong(lUserExperienceTimeString);
    }

    DOM lParamsDOM = DOM.createDocument("BeaconParams");
    for(Map.Entry<String, String[]> lParam : pFoxRequest.getHttpRequest().getParameterMap().entrySet()) {
      lParamsDOM.addElem(lParam.getKey(), lParam.getValue()[0]);
    }

    RequestLogger.instance().logUserExperienceTime(lRequestId, lUserExperienceTimeMS, lParamsDOM);

    return new FoxResponseCHAR("text/plain", new StringBuffer("OK"), 0L);
  }
}
