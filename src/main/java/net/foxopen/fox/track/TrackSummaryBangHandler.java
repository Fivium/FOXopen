package net.foxopen.fox.track;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.banghandler.InternalAuthLevel;

import java.util.Collection;
import java.util.Collections;

public class TrackSummaryBangHandler
implements BangHandler {

  public static final String TRACK_ID = "id";

  public static TrackSummaryBangHandler instance() {
    return INSTANCE;
  }

  private static final TrackSummaryBangHandler INSTANCE = new TrackSummaryBangHandler();

  private TrackSummaryBangHandler() { }

  @Override
  public String getAlias() {
    return "TRACKSUMMARY";
  }

  @Override
  public Collection<String> getParamList() {
    return Collections.singleton(TRACK_ID);
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
    return TrackUtils.generateJSONSummaryResponse(pFoxRequest.getHttpRequest().getParameter(TRACK_ID));
  }
}
