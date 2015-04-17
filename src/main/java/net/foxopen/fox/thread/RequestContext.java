package net.foxopen.fox.thread;

import net.foxopen.fox.App;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;

public interface RequestContext {

  FoxRequest getFoxRequest();

  ContextUCon getContextUCon();

  String getRequestAppMnem();

  App getRequestApp();

  FoxSession getFoxSession();

  void forceNewFoxSession(FoxSession pNewSession);

  SecurityScope getCurrentSecurityScope();

  RequestURIBuilder createURIBuilder();
}
