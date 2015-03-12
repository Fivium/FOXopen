package net.foxopen.fox.thread.devtoolbar;

import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;

import java.util.Collections;
import java.util.Set;

public class DevToolbarOptionsBangHandler
extends DevToolbarBangHandler {

  private static final DevToolbarOptionsBangHandler INSTANCE = new DevToolbarOptionsBangHandler();
  public static DevToolbarOptionsBangHandler instance() {
    return INSTANCE;
  }

  private DevToolbarOptionsBangHandler() { }

  @Override
  protected FoxResponse getResponseInternal(RequestContext pRequestContext, StatefulXThread pXThread) {
    return DevToolbarUtils.applyPostedForm(pRequestContext.getFoxRequest(), pXThread.getDevToolbarContext());
  }

  @Override
  protected Set<String> getAdditionalParamList() {
    return Collections.emptySet();
  }

  @Override
  public String getAlias() {
    return "DEVTOOLBAROPTIONS";
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.INTERNAL_SUPPORT;
  }
}
