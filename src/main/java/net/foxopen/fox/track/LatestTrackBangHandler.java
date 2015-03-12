package net.foxopen.fox.track;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.banghandler.InternalAuthLevel;

import java.util.Collection;
import java.util.Collections;

public class LatestTrackBangHandler
implements BangHandler {

  private static final LatestTrackBangHandler INSTANCE = new LatestTrackBangHandler();

  public static String LATEST_TRACK_INDEX_PARAM_NAME = "i";

  public static LatestTrackBangHandler instance() {
    return INSTANCE;
  }

  private LatestTrackBangHandler(){}

  @Override
  public String getAlias() {
    return "LATESTTRACK";
  }

  @Override
  public Collection<String> getParamList() {
    return Collections.singleton(LATEST_TRACK_INDEX_PARAM_NAME);
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.INTERNAL_SUPPORT;
  }

  @Override
  public boolean isDevAccessAllowed() {
    return true;
  }

  @Override
  public FoxResponse respond(FoxRequest pFoxRequest) {
    int lIndex;
    try {
      lIndex = Integer.parseInt(pFoxRequest.getParameter("i"));
    }
    catch (NumberFormatException e) {
      return new FoxResponseCHAR("text/plain", new StringBuffer("Invalid number for index parameter"), 0);
    }

    return  TrackUtils.outputLatestTrack(pFoxRequest, lIndex);
  }
}
