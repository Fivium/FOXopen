package net.foxopen.fox.entrypoint;

import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.ex.ExSessionTimeout;
import net.foxopen.fox.thread.RequestContext;

public interface FoxSession {

  String getSessionId();

  String checkSessionValidity(RequestContext pRequestContext, AuthenticationContext pAuthenticationContext, String pThreadSessionId);

  /**
   * Forces this FoxSession to move its ID on. This should be invoked when a user's authentication state changes.
   * @param pRequestContext
   * @param pCurrentAuthContextSessionId
   */
  void forceNewFoxSessionID(RequestContext pRequestContext, String pCurrentAuthContextSessionId);

  /**
   * Finalises this session at the end of a churn.
   * @param pRequestContext
   * @param pThreadSessionId Current session ID held by the thread.
   * @return The latest version of the session ID, which may be different from the thread's current session ID if another
   * thread has moved the session on concurrently. The consuming thread should ensure it stores this latest session ID.
   */
  String finaliseSession(RequestContext pRequestContext, String pThreadSessionId);


  String getAuthContextSessionId(RequestContext pRequestContext) throws ExSessionTimeout;
}
