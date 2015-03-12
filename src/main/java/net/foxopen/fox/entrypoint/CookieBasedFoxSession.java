package net.foxopen.fox.entrypoint;


import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxRequestHttp;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExSessionTimeout;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.util.RandomString;

import javax.servlet.http.HttpServletRequest;

/**
 * This class manages the FOX Session. This is a cookie and thread member that is used to check requests to threads
 * came from the session that originally spawned the thread. The session therefore stops thread-hijacks.
 *
 * The FOX Sessions gets invalidated every time a users auth status changes, e.g. a login, and sessions have their own
 * history of which session ID moved to another on update. This is mostly used so that two threads sharing a session can
 * run independently and still check they have a common origin.
 */
public class CookieBasedFoxSession
implements FoxSession {

  private static final String INSERT_FOX_SESSION = "InsertFoxSession.sql";
  private static final String UPDATE_FOX_SESSION = "UpdateFoxSession.sql";
  private static final String GET_LATEST_SESSION_ID = "GetLatestSessionID.sql";
  private static final String GET_SESSION_INITIAL_FLAG = "GetSessionInitialFlag.sql";
  private static final String GET_SESSION_WUS_ID = "GetSessionWusId.sql";

  private static final String FOX_SESSION_ID_COOKIE_NAME = "FOX_SESSION_ID";

  private String mSessionId;

  public static FoxSession getOrCreateFoxSession(RequestContext pRequestContext) {
    String lFoxSessionID = pRequestContext.getFoxRequest().getCookieValue(FOX_SESSION_ID_COOKIE_NAME);

    if (!XFUtil.isNull(lFoxSessionID)) {
      return new CookieBasedFoxSession(lFoxSessionID);
    }
    else {
      return createNewSession(pRequestContext, true, null);
    }
  }

  public static FoxSession getFoxSession(FoxRequest pFoxRequest) {
    String lFoxSessionID = pFoxRequest.getCookieValue(FOX_SESSION_ID_COOKIE_NAME);

    if (!XFUtil.isNull(lFoxSessionID)) {
      return new CookieBasedFoxSession(lFoxSessionID);
    }
    else {
      throw new ExInternal("Missing required session cookie " + FOX_SESSION_ID_COOKIE_NAME);
    }
  }

  public static FoxSession createNewSession(RequestContext pRequestContext, boolean pSetInitialFlag, String pCurrentWusId) {
    return new CookieBasedFoxSession(newSessionId(pRequestContext, pSetInitialFlag, pCurrentWusId));
  }

  public static String getSessionIdFromRequest(HttpServletRequest pRequest) {
    return FoxRequestHttp.getCookieValue(pRequest.getCookies(), FOX_SESSION_ID_COOKIE_NAME);
  }

  private static String newSessionId(RequestContext pRequestContext, boolean pSetInitialFlag, String pCurrentWusId) {
    Track.pushDebug("FoxSession", "Inserting FOX Session");
    Track.timerStart("FoxSession");
    try {
      String lFoxSessionID = RandomString.getString(128);

      // Buffer up new cookie out value to be applied at the end
      FoxCache<String, String> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.SESSION_COOKIES);
      lFoxCache.put(pRequestContext.getFoxRequest().getHttpRequest().getSession().getId(), lFoxSessionID);

      // Insert new session id to the DB now however, so concurrent requests can see it
      pRequestContext.getContextUCon().pushConnection("INSERT_FOX_SESSION");
      UCon lUCon = pRequestContext.getContextUCon().getUCon("Inserting FOX Session");
      try {
        UConBindMap lBindMap = new UConBindMap()
          .defineBind(":fox_session_id", lFoxSessionID)
          .defineBind(":current_wus_id",  pCurrentWusId)
          .defineBind(":initial_flag",  (pSetInitialFlag ? "Y" : null));
        lUCon.executeAPI(SQLManager.instance().getStatement(INSERT_FOX_SESSION, CookieBasedFoxSession.class), lBindMap);
        lUCon.commit();
      }
      catch (ExDB | ExServiceUnavailable e) {
        throw e.toUnexpected();
      }
      finally {
        pRequestContext.getContextUCon().returnUCon(lUCon, "Inserting FOX Session");
        pRequestContext.getContextUCon().popConnection("INSERT_FOX_SESSION");
      }

      return lFoxSessionID;
    }
    finally {
      Track.timerPause("FoxSession");
      Track.pop("FoxSession");
    }
  }

  public CookieBasedFoxSession(String pSessionId) {
    mSessionId = pSessionId;
  }

  @Override
  public String getSessionId() {
    return mSessionId;
  }

  @Override
  public void forceNewFoxSessionID(RequestContext pRequestContext, String pCurrentAuthContextSessionId) {
    Track.pushDebug("FoxSession", "Refreshing FOX Session");
    Track.timerStart("FoxSession");
    try {
      String lNewFOXSessionID = newSessionId(pRequestContext, false, pCurrentAuthContextSessionId);

      //Update the old Fox session immediately so changes are visible to other threads
      pRequestContext.getContextUCon().pushConnection("UPDATE_FOX_SESSION");
      UCon lUCon = pRequestContext.getContextUCon().getUCon("Updating FOX Sessions");
      try {
        UConBindMap lBindMap = new UConBindMap()
          .defineBind(":old_session_id", mSessionId)
          .defineBind(":new_session_id",  lNewFOXSessionID);
        lUCon.executeAPI(SQLManager.instance().getStatement(UPDATE_FOX_SESSION, getClass()), lBindMap);
        lUCon.commit();
      }
      catch (ExDB | ExServiceUnavailable e) {
        throw e.toUnexpected();
      }
      finally {
        pRequestContext.getContextUCon().returnUCon(lUCon, "Updating FOX Sessions");
        pRequestContext.getContextUCon().popConnection("UPDATE_FOX_SESSION");
      }

      //Update the stored session ID to the latest ID
      mSessionId = lNewFOXSessionID;
    }
    finally {
      Track.timerPause("FoxSession");
      Track.pop("FoxSession");
    }
  }

  /**
   * For a given pCurrentSessionID follow the session chain up to find the latest one
   *
   * @param pCurrentSessionID
   * @param pRequestContext
   * @return
   * @throws ExSessionTimeout
   */
  private String getLatestSessionID(String pCurrentSessionID, RequestContext pRequestContext) throws ExSessionTimeout {
    Track.pushInfo("FoxSession", "Getting latest FOX Session");
    Track.timerStart("FoxSession");
    try {
      UCon lUCon = pRequestContext.getContextUCon().getUCon("Getting Latest FOX Session");
      try {
        String lLatestSessionID = lUCon.queryScalarString(SQLManager.instance().getStatement(GET_LATEST_SESSION_ID, getClass()), pCurrentSessionID);

        if (pCurrentSessionID.equals(lLatestSessionID)){
          Track.info("Session ID was the latest anyway");
        }

        return lLatestSessionID;
      }
      catch (ExDBTooFew e) {
        Track.info("A stale cookie may have been sent, no row found for given FOX Session ID '" + pCurrentSessionID + "'");
        throw new ExSessionTimeout("Session ID from cookie is stale and not found in session table", e);
      }
      catch (ExDB e) {
        throw e.toUnexpected();
      }
      finally {
        pRequestContext.getContextUCon().returnUCon(lUCon, "Getting Latest FOX Session");
      }
    }
    finally {
      Track.timerPause("FoxSession");
      Track.pop("FoxSession");
    }
  }

  @Override
  public String finaliseSession(RequestContext pRequestContext, String pThreadSessionId) {
    Track.pushDebug("FoxSession", "Finalise FOX Session");
    Track.timerStart("FoxSession");
    String lLatestSessionId = pThreadSessionId;
    try {
      // Check the cache for a new FOX session ID corresponding to the current browser session
      FoxCache<String, String> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.SESSION_COOKIES);
      String lCookieSessionID = lFoxCache.remove(pRequestContext.getFoxRequest().getHttpRequest().getSession().getId());

      // If the cache has an entry, we need to make sure the latest correct value is sent in the cookie (otherwise we don't need to do anything as the cookie is already correct)
      if (!XFUtil.isNull(lCookieSessionID)) {

        // Get latest and change cookie to that?
        if (!lCookieSessionID.equals(pThreadSessionId)) {
          Track.info("Cookie and thread out of step, making sure it will use latest");
          String lCookieLatest = getLatestSessionID(lCookieSessionID, pRequestContext);
          String lThreadLatest = getLatestSessionID(pThreadSessionId, pRequestContext);

          if (!lThreadLatest.equals(lCookieLatest)) {
            throw new ExInternal("Cookie and thread do not appear to share the same root session - cookie current: " + lCookieSessionID  +  ", thread current: " + pThreadSessionId +
              ", cookie latest: " + lCookieLatest +  ", thread latest: " + lThreadLatest);
          }
          else if (!pThreadSessionId.equals(lThreadLatest)) {
            // If the cookie and thread are from the same root, make sure the thread gets on the latest session ID if it wasn't already
            lLatestSessionId = lThreadLatest;
            Track.info("ThreadSessionIdOutOfDate", "Thread needs later session ID " + lLatestSessionId);
          }

          // Attempt to make sure outbound cookie this time is the latest
          pRequestContext.getFoxRequest().addCookie(FOX_SESSION_ID_COOKIE_NAME, lCookieLatest, true, true);
        }
        else {
          // If the thread and outbound cookie agree, send it out
          pRequestContext.getFoxRequest().addCookie(FOX_SESSION_ID_COOKIE_NAME, lCookieSessionID, true, true);
        }
      }
    }
    catch (ExSessionTimeout e) {
      // thrown when getLatestSessionID is given an invalid/stale session, just ignore it
    }
    finally {
      Track.timerPause("FoxSession");
      Track.pop("FoxSession");
    }

    return lLatestSessionId;
  }

  @Override
  public String checkSessionValidity(RequestContext pRequestContext, AuthenticationContext pAuthenticationContext, String pThreadSessionId) {
    Track.pushDebug("FoxSession", "Check Session Validity");
    Track.timerStart("FoxSession");
    try {
      String lCookieSessionID = pRequestContext.getFoxRequest().getCookieValue(FOX_SESSION_ID_COOKIE_NAME);
      if (XFUtil.isNull(lCookieSessionID)) {
        Track.info("No session cookie found");
        throw new ExInternal("Invalid session detected: request missing FOX session cookie");
      }

      if (lCookieSessionID.equals(pThreadSessionId)) {
        // Check if thread and cookie match, if so all okay
        Track.info("Thread and cookie matched");
        return pThreadSessionId;
      }
      else {
        Track.info("Thread and cookie didn't match");
        // Bring thread up to latest and check initial_flag
        String lThreadLatest = getLatestSessionID(pThreadSessionId, pRequestContext);

        if (lCookieSessionID.equals(lThreadLatest)) {
          // Check if thread latest and cookie match, if so all okay
          Track.info("Thread latest and cookie matched");
          return lThreadLatest;
        }
        else if (!pAuthenticationContext.isAuthenticated()) {
          Track.info("Thread latest and cookie didn't match and user is not currently authenticated");
          // If still no match but the initial_flag was set and the thread isn't marked up as logged in, all okay otherwise blow up
          String lThreadInitialFlag;
          pRequestContext.getContextUCon().pushConnection("GET_FOX_SESSION_FLAG");
          UCon lUCon = pRequestContext.getContextUCon().getUCon("Getting FOX Session Initial Flag");
          UConStatementResult lAPIResult;
          try {
            UConBindMap lGetFlagBindMap = new UConBindMap()
              .defineBind(":fox_session_id", lThreadLatest)
              .defineBind(":flag_value", UCon.bindOutString());

            lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(GET_SESSION_INITIAL_FLAG, getClass()), lGetFlagBindMap);
            lUCon.commit();

            lThreadInitialFlag = lAPIResult.getString(":flag_value");

            if ("Y".equals(lThreadInitialFlag)) {
              Track.info("Thread latest session appeared to be an initial request, giving it a free pass");

              // Move the thread on to the cookie session and let it pass

              // Update in the table so we can see this pointing to the new session route
              UConBindMap lUpdateBindMap = new UConBindMap()
                .defineBind(":old_session_id", lThreadLatest)
                .defineBind(":new_session_id",  lCookieSessionID);
              lUCon.executeAPI(SQLManager.instance().getStatement(UPDATE_FOX_SESSION, getClass()), lUpdateBindMap);
              lUCon.commit();

              return lThreadLatest;
            }
            else {
              throw new ExInternal("Invalid session replay detected: Attempted to use a stale cookie and an unassociated thread");
            }
          }
          catch (ExDB | ExServiceUnavailable e) {
            throw e.toUnexpected();
          }
          finally {
            pRequestContext.getContextUCon().returnUCon(lUCon, "Getting FOX Session Initial Flag");
            pRequestContext.getContextUCon().popConnection("GET_FOX_SESSION_FLAG");
          }
        }
        else {
          throw new ExInternal("Invalid session replay detected: Attempted to get in to logged in thread with stale cookie");
        }
      }
    }
    catch (ExSessionTimeout pExSessionTimeout) {
      throw new ExInternal("Invalid session on Thread: FOX Session ID '" + pThreadSessionId  + "' not found in sessions table", pExSessionTimeout);
    }
    finally {
      Track.timerPause("FoxSession");
      Track.pop("FoxSession");
    }
  }

  @Override
  public String getAuthContextSessionId(RequestContext pRequestContext) throws ExSessionTimeout {
    String lSessionID = getLatestSessionID(mSessionId, pRequestContext);

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Getting FOX Session WUS ID");
    try {
      return lUCon.queryScalarString(SQLManager.instance().getStatement(GET_SESSION_WUS_ID, CookieBasedFoxSession.class), lSessionID);
    }
    catch (ExDB e) {
      throw e.toUnexpected();
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Getting FOX Session WUS ID");
    }
  }
}
