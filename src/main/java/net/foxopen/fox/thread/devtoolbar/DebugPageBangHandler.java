package net.foxopen.fox.thread.devtoolbar;

import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;

import java.util.Collections;
import java.util.Set;

public class DebugPageBangHandler
extends DevToolbarBangHandler {

  public static final String DEBUG_PAGE_TYPE_PARAM_NAME = "page_type";

  private static final DebugPageBangHandler INSTANCE = new DebugPageBangHandler();
  public static DebugPageBangHandler instance() {
    return INSTANCE;
  }

  private DebugPageBangHandler() {}

  @Override
  protected FoxResponse getResponseInternal(RequestContext pRequestContext, StatefulXThread pXThread) {

    DebugPage lPageType = DebugPage.fromRequestParam(pRequestContext.getFoxRequest().getParameter(DEBUG_PAGE_TYPE_PARAM_NAME));
    return pXThread.getDevToolbarContext().getDebugPage(pRequestContext, lPageType);
  }

  @Override
  protected Set<String> getAdditionalParamList() {
    return Collections.singleton(DEBUG_PAGE_TYPE_PARAM_NAME);
  }

  @Override
  public String getAlias() {
    return "DEBUGPAGE";
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.INTERNAL_SUPPORT;
  }
}
