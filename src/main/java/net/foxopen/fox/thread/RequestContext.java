package net.foxopen.fox.thread;

import net.foxopen.fox.App;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;

public interface RequestContext {

  public FoxRequest getFoxRequest();

  public ContextUCon getContextUCon();

  public String getRequestAppMnem();

  public App getRequestApp();

  public FoxSession getFoxSession();

  public SecurityScope getCurrentSecurityScope();

  public RequestURIBuilder createURIBuilder();
}
