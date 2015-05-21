package net.foxopen.fox.track;

import com.google.common.collect.EvictingQueue;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.banghandler.InternalAuthentication;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.thread.RequestContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class TrackUtils {

  public static final int MAX_RECENT_TRACKS = 5;

  private TrackUtils() {}

  /**
   * Creates a TrackLogger which can be recorded against the current session id, for later retrieval from the dev toolbar.
   * @param pRequestContext
   * @return
   */
  public static TrackLogger createDefaultTrackLogger(RequestContext pRequestContext) {
    TrackLogger lNewTrackLogger = createDefaultTrackLogger(pRequestContext.getFoxRequest());

    //Get or create the recent track list for the given origin ID, then record this new track in it
    if(FoxGlobals.getInstance().isDevelopment() || InternalAuthentication.instance().getSessionAuthLevel(pRequestContext.getFoxRequest()).intValue() >= InternalAuthLevel.INTERNAL_SUPPORT.intValue()) {
      FoxCache<String, Queue<String>> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.RECENT_TRACK_IDS_FOR_SESSION_ID);
      Queue<String> lRecentTrackQueue = lFoxCache.get(pRequestContext.getFoxRequest().getHttpRequest().getSession().getId());
      if(lRecentTrackQueue == null) {
        lRecentTrackQueue = EvictingQueue.create(MAX_RECENT_TRACKS);
        lFoxCache.put(pRequestContext.getFoxRequest().getHttpRequest().getSession().getId(), lRecentTrackQueue);
      }
      lRecentTrackQueue.add(lNewTrackLogger.getTrackId());
    }

    return lNewTrackLogger;
  }

  /**
   * Creates a new TrackLogger for tracking the given request.
   * @param pFoxRequest Request being tracked.
   * @return
   */
  public static TrackLogger createDefaultTrackLogger(FoxRequest pFoxRequest) {
    DefaultTrackLogger lNewTrackLogger = new DefaultTrackLogger(pFoxRequest.getRequestLogId(), DatabaseTrackLogWriter.instance());
    CacheManager.getCache(BuiltInCacheDefinition.RECENT_TRACKS).put(lNewTrackLogger.getTrackId(), lNewTrackLogger);
    return lNewTrackLogger;
  }

  public static FoxResponse outputLatestTrack(FoxRequest pFoxRequest, int pIndex){

    FoxCache<String, Queue<String>> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.RECENT_TRACK_IDS_FOR_SESSION_ID);
    Queue<String> lRecentTrackQueue = lFoxCache.get(pFoxRequest.getHttpRequest().getSession().getId());
    if(lRecentTrackQueue == null) {
      return new FoxResponseCHAR("text/plain", new StringBuffer("No recent tracks available"), 0);
    }
    else {
      //Reverse the queue and loop through tail-first (last item is "index 1" (i.e. latest))
      ArrayList<String> lRecentTrackList = new ArrayList<String>(lRecentTrackQueue);
      Collections.reverse(lRecentTrackList);
      int i = 0;
      for(String lTrackId : lRecentTrackList) {
        if(++i == pIndex) {
          return outputTrack(lTrackId, pFoxRequest, false);
        }
      }
      //Loop completed without finding a corresponding track
      return new FoxResponseCHAR("text/plain", new StringBuffer("Track " + pIndex + " not available"), 0);
    }
  }

  public static FoxResponse outputTrack(String pTrackId, FoxRequest pFoxRequest){
    String lTimerDetail = pFoxRequest.getParameter("timerDetail");
    return outputTrack(pTrackId, pFoxRequest, !XFUtil.isNull(lTimerDetail));
  }

  private static FoxResponse outputTrack(String pTrackId, FoxRequest pFoxRequest, boolean lTimerDetail){
    FoxCache<String, TrackLogger> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.RECENT_TRACKS);
    TrackLogger lTrackLogger = lFoxCache.get(pTrackId);
    if(lTrackLogger != null) {
      return XMLTrackSerialiser.instance().createFoxResponse(lTrackLogger, pFoxRequest, lTimerDetail);
    }
    else {
      return new FoxResponseCHAR("text/plain", new StringBuffer("Track not found"), 0);
    }
  }

  public static void outputTrackToWriter(String pTrackId, Writer pOutputWriter, boolean lTimerDetail){
    FoxCache<String, TrackLogger> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.RECENT_TRACKS);
    TrackLogger lTrackLogger = lFoxCache.get(pTrackId);
    try {
      if (lTrackLogger != null) {
        XMLTrackSerialiser.instance().serialiseToWriter(lTrackLogger, pOutputWriter, lTimerDetail);
      }
      else {
        pOutputWriter.append("Track not found");
      }
    }
    catch (IOException e) {
      /* ignore */
    }
  }

  public static String getRootErrorStack(String pTrackId){
    FoxCache<String, TrackLogger> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.RECENT_TRACKS);
    TrackLogger lTrackLogger = lFoxCache.get(pTrackId);
    if (lTrackLogger != null) {
      return XFUtil.getJavaStackTraceInfo(lTrackLogger.getRootException());
    }
    else {
      return "Track not found";
    }
  }

  /**
   * Return a JSON Summary of Track, this can be used by the developer toolbar to show timing information and
   * notifications logged in Track for a given TrackId
   *
   * @param pTrackId
   * @return
   */
  public static FoxResponse generateJSONSummaryResponse(String pTrackId){
    FoxCache<String, TrackLogger> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.RECENT_TRACKS);
    TrackLogger lTrackLogger = lFoxCache.get(pTrackId);
    if(lTrackLogger == null) {
      FoxResponse lErrorResponse = new FoxResponseCHAR("text/plain", new StringBuffer("Track not found"), 0);
      lErrorResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return lErrorResponse;
    }

    JSONObject lTrackSummary = new JSONObject();

    JSONArray lMessageArray = new JSONArray();
    Set<TrackEntry> lTracksDone = new HashSet<>();
    for(Map.Entry<TrackFlag, List<TrackEntry>> lEntry : lTrackLogger.getFlagToEntryMap().entrySet()) {
      TrackFlag lFlag = lEntry.getKey();
      if(lFlag.isRequiresAttention()) {
        for(TrackEntry lTrack : lEntry.getValue()) {
          if(!lTracksDone.contains(lTrack)) {
            Map<String, Object> lMessageMap = new HashMap<>();
            if (lTrack.getSeverity() == Track.SeverityLevel.ALERT) {
              lMessageMap.put("level", "error");
            }
            else {
              lMessageMap.put("level", "warning");
            }
            lMessageMap.put("type", XFUtil.initCap(lFlag.toString()));
            lMessageMap.put("subject", lTrack.getSubject());
            lMessageMap.put("message", (!XFUtil.isNull(lTrack.getInfo()) ? lTrack.getInfo() : ""));

            if( lTrack.hasFlag(TrackFlag.CRITICAL)) {
              lMessageMap.put("criticalFlag", true);
            }

            lMessageArray.add(lMessageMap);
            lTracksDone.add(lTrack);
          }
        }
      }
    }
    lTrackSummary.put("messages", lMessageArray);


    Map<String, Object> lTimingsMap = new HashMap<>();
    lTimingsMap.put("pageTime", (lTrackLogger.getRootEntry().getOutTime() -  lTrackLogger.getRootEntry().getInTime()));

    JSONArray lComponentTimingsArray = new JSONArray();
    JSONArray lCumulativeTimingsArray = new JSONArray();

    for(TrackTimer lTimer : TrackTimer.values()) {
      if(lTimer.isReportInSummary()) {
        long lTimerVal = lTrackLogger.getTimerValue(lTimer);
        if(lTimerVal != -1) {
          JSONObject lComponentTimeObject = new JSONObject();
          lComponentTimeObject.put("type", XFUtil.initCap(lTimer.toString()));
          lComponentTimeObject.put("label", XFUtil.initCap(lTimer.toString()));
          lComponentTimeObject.put("time", lTimerVal);
          lComponentTimeObject.put("warningThreshold", 500); // TODO - NP - Remove this or do something about it
          if(!lTimer.isCumulative()) {
            lComponentTimingsArray.add(lComponentTimeObject);
          }
          else {
            lCumulativeTimingsArray.add(lComponentTimeObject);
          }
        }
      }
    }
    lTimingsMap.put("componentTimes", lComponentTimingsArray);
    lTimingsMap.put("cumulativeTimes", lCumulativeTimingsArray);

    lTrackSummary.put("timings", lTimingsMap);

    return new FoxResponseCHAR("application/json", new StringBuffer(lTrackSummary.toJSONString()), 0);
  }

}
