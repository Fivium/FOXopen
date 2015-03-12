package net.foxopen.fox.entrypoint.auth;

import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.ex.ExSessionTimeout;
import net.foxopen.fox.thread.RequestContext;


public interface AuthType {
  public AuthenticationContext processBeforeEntry(RequestContext pRequestContext) throws ExSessionTimeout;
}
