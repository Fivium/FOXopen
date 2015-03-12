package net.foxopen.fox.track;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.banghandler.InternalAuthLevel;

import java.util.Collection;
import java.util.Collections;

public class ShowTrackBangHandler
implements BangHandler {

  public static final String TRACK_ID_PARAM_NAME = "track_id";
  private static final ShowTrackBangHandler INSTANCE = new ShowTrackBangHandler();

  public static ShowTrackBangHandler instance() {
    return INSTANCE;
  }

  private ShowTrackBangHandler(){}

  @Override
  public String getAlias() {
    return "SHOWTRACK";
  }

  @Override
  public Collection<String> getParamList() {
    return Collections.singleton(TRACK_ID_PARAM_NAME);
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
    return TrackUtils.outputTrack(pFoxRequest.getParameter(TRACK_ID_PARAM_NAME), pFoxRequest);
  }
}
