package net.foxopen.fox.thread.devtoolbar;

import com.google.common.collect.Sets;
import net.foxopen.fox.App;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.entrypoint.CookieBasedFoxSession;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.RequestContextImpl;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.ThreadLockManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

abstract class DevToolbarBangHandler
implements BangHandler {

  public static final String THREAD_ID_PARAM = "thread_id";

  private static final String CONNECTION_NAME = "DEV_TOOLBAR_BANG_HANDLER";

  public FoxResponse respond(FoxRequest pFoxRequest) {

    //TODO PN this should get an app from the request params (i.e. so it's specific to the thread)
    final RequestContext lRequestContext;
    try {
      FoxSession lFoxSession = CookieBasedFoxSession.getFoxSession(pFoxRequest);
      App lApp = FoxGlobals.getInstance().getFoxEnvironment().getDefaultApp();
      lRequestContext = RequestContextImpl.createFromFoxRequest(pFoxRequest, lApp, "DebugRequest", CONNECTION_NAME, lFoxSession);
    }
    catch (ExServiceUnavailable | ExApp e) {
      throw new ExInternal("Default app not available", e);
    }

    String lThreadId = XFUtil.nvl(pFoxRequest.getHttpRequest().getParameter("thread_id")).trim();
    ThreadLockManager<FoxResponse> lLockManager = new ThreadLockManager<>(lThreadId, false);

    //Lock the thread and delegate to subclasses to get a response
    return lLockManager.lockAndPerformAction(lRequestContext, new ThreadLockManager.LockedThreadRunnable<FoxResponse>() {
      @Override
      public FoxResponse doWhenLocked(RequestContext pRequestContext, StatefulXThread pXThread) {
        return getResponseInternal(lRequestContext, pXThread);
      }
    });
  }

  @Override
  public Collection<String> getParamList() {
    return Sets.union(Collections.singleton("thread_id"), getAdditionalParamList());
  }

  @Override
  public final boolean isDevAccessAllowed() {
    return true;
  }

  protected abstract FoxResponse getResponseInternal(RequestContext pRequestContext, StatefulXThread pXThread);

  protected abstract Set<String> getAdditionalParamList();

}
