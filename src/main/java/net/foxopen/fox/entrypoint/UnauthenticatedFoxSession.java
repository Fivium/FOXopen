package net.foxopen.fox.entrypoint;

import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.ex.ExSessionTimeout;
import net.foxopen.fox.thread.RequestContext;

public class UnauthenticatedFoxSession
implements FoxSession {

  public static FoxSession create() {
    return new UnauthenticatedFoxSession();
  }

  private UnauthenticatedFoxSession() {
  }

  @Override
  public String getSessionId() {
    return null;
  }

  @Override
  public String checkSessionValidity(RequestContext pRequestContext, AuthenticationContext pAuthenticationContext, String pThreadSessionId) {
    return null;
  }

  @Override
  public void forceNewFoxSessionID(RequestContext pRequestContext, String pCurrentAuthContextSessionId) {
  }

  @Override
  public String finaliseSession(RequestContext pRequestContext, String pThreadSessionId) {
    return null;
  }

  @Override
  public String getAuthContextSessionId(RequestContext pRequestContext)
  throws ExSessionTimeout {
    return null;
  }
}
