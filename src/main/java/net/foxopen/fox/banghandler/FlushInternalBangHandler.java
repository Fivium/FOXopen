package net.foxopen.fox.banghandler;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.entrypoint.ComponentManager;

import java.util.Collection;
import java.util.Collections;

public class FlushInternalBangHandler
implements BangHandler {

  private static final FlushInternalBangHandler INSTANCE = new FlushInternalBangHandler();
  public static FlushInternalBangHandler instance() {
    return INSTANCE;
  }

  private FlushInternalBangHandler() { }

  @Override
  public String getAlias() {
    return "FLUSHINTERNAL";
  }

  @Override
  public Collection<String> getParamList() {
    return Collections.emptySet();
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.INTERNAL_ADMIN;
  }

  @Override
  public boolean isDevAccessAllowed() {
    return false;
  }

  @Override
  public FoxResponse respond(FoxRequest pFoxRequest) {
    ComponentManager.loadInternalComponents();
    return BangHandlerServlet.basicHtmlResponse("FLUSHINTERNAL OK");
  }
}
