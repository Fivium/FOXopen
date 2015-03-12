package net.foxopen.fox.track;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.StatusCategory;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusTable;
import net.foxopen.fox.entrypoint.filter.RequestLogFilter;
import net.foxopen.fox.ex.TrackableException;
import net.foxopen.fox.logging.ErrorLogger;
import net.foxopen.fox.track.Track.SeverityLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


class DefaultTrackLogger
implements TrackLogger {

  static {
    EngineStatus.instance().registerStatusProvider(new StatusProvider() {
      @Override
      public void refreshStatus(StatusCategory pCategory) {

        //Exclude the current request from the overall count
        pCategory.addMessage("Active request counter", Integer.toString(RequestLogFilter.getActiveRequestCount() - 1));

        StatusTable lTable = pCategory.addTable("Hot Tracks", "Track ID", "View Track");
        lTable.setRowProvider(new StatusTable.RowProvider() {
          @Override
          public void generateRows(StatusTable.RowDestination pRowDestination) {
            for (Map.Entry<String, TrackLogger> lHotTrack : CacheManager.<String, TrackLogger>getCache(BuiltInCacheDefinition.HOT_TRACKS).entrySet()) {
              pRowDestination.addRow(lHotTrack.getKey())
                .setColumn(lHotTrack.getKey())
                .setActionColumn("View", ShowTrackBangHandler.instance(), Collections.singletonMap(ShowTrackBangHandler.TRACK_ID_PARAM_NAME, lHotTrack.getKey()));
            }
          }
        });
      }

      @Override
      public String getCategoryTitle() {
        return "Active Requests";
      }

      @Override
      public String getCategoryMnemonic() {
        return "activeRequests";
      }

      @Override
      public boolean isCategoryExpandedByDefault() {
        return true;
      }
    });
  }

  private static final Iterator<String> TRACK_ID_ITERATOR = XFUtil.getUniqueIterator();

  private final Map<String, List<TimerEntry>> mTimers = new LinkedHashMap<>();
  private final Map<String, Integer> mCounters = new HashMap<>();
  private final Map<TrackProperty,String> mProperties = new EnumMap<>(TrackProperty.class);
  private final Map<String, ExceptionTrackEntry> mErrorIdToErrorEntryMap = new HashMap<>();

  private TrackEntry mCurrentEntry;
  private TrackEntry mTopEntry;

  private final String mTrackId;
  private final String mRequestId;

  /** Where the track will be written when closed. */
  private final TrackLogWriter mLogWriter;

  private Throwable mRootException;

  private long mStartTimeMS;

  /** Map which places ALERT flags first */
  private final Map<TrackFlag, List<TrackEntry>> mFlagsToEntries = new TreeMap<>(
    new Comparator<TrackFlag>() {
      @Override
      public int compare(TrackFlag pO1, TrackFlag pO2) {
        if(pO1 == pO2) {
          return pO1.compareTo(pO2);
        }
        else if(pO1 == TrackFlag.ALERT) {
          return -1;
        }
        else {
          return 1; //po2 == Alert
        }
      }
    }
  );

  DefaultTrackLogger(String pRequestId, TrackLogWriter pLogWriter) {
    mTrackId = TRACK_ID_ITERATOR.next();
    mRequestId = pRequestId;
    mLogWriter = pLogWriter;
  }

  private void mapEntryToFlags(TrackEntry pEntry, TrackFlag... pFlags) {
    for(TrackFlag lFlag : pFlags) {
      //Create an empty list if this is the first time this flag has been logged
      if(!mFlagsToEntries.containsKey(lFlag)) {
        mFlagsToEntries.put(lFlag, new ArrayList<TrackEntry>());
      }

      mFlagsToEntries.get(lFlag).add(pEntry);
    }
  }

  private TrackEntry createTrackEntry(String pSubject, String pInfo, SeverityLevel pSeverity, TrackEntry pParent, TrackFlag[] pFlags) {
    TrackEntry lNewTrackEntry = new StandardTrackEntry(pSubject, pInfo, pSeverity, pParent, pFlags);

    //Always record alerts against the ALERT flag
    if(pSeverity == SeverityLevel.ALERT) {
      mapEntryToFlags(lNewTrackEntry, TrackFlag.ALERT);
    }

    return lNewTrackEntry;
  }

  @Override
  public void open(String pTrackName) {
    mCurrentEntry = createTrackEntry(pTrackName, "", Track.SeverityLevel.INFO, null, null);
    mTopEntry = mCurrentEntry;
    mStartTimeMS = System.currentTimeMillis();
    CacheManager.getCache(BuiltInCacheDefinition.HOT_TRACKS).put(mTrackId, this);
  }

  @Override
  public void close() {
    mCurrentEntry.setOutTime(System.currentTimeMillis());
    mLogWriter.writeTrack(this);
    CacheManager.getCache(BuiltInCacheDefinition.HOT_TRACKS).remove(mTrackId);
  }

  @Override
  public void log(SeverityLevel pSeverityLevel, String pSubject, String pMessage) {
    mCurrentEntry.addChildEntry(createTrackEntry(pSubject, pMessage, pSeverityLevel, mCurrentEntry, null));
  }

  @Override
  public void log(SeverityLevel pSeverityLevel, String pSubject, String pMessage, TrackFlag... pFlags) {
    TrackEntry lNewEntry = createTrackEntry(pSubject, pMessage, pSeverityLevel, mCurrentEntry, pFlags);
    mCurrentEntry.addChildEntry(lNewEntry);
    mapEntryToFlags(lNewEntry, pFlags);
  }

  @Override
  public void log(SeverityLevel pSeverityLevel, String pMessage) {
    mCurrentEntry.addChildEntry(createTrackEntry(pSeverityLevel.toString().toLowerCase(), pMessage, pSeverityLevel, mCurrentEntry, null));
  }

  @Override
  public void log(SeverityLevel pSeverityLevel, String pSubject, Trackable pInfo) {
    push(pSeverityLevel, pSubject, "");
    pInfo.writeTrackData();
    pop(pSubject);
  }

  @Override
  public void push(SeverityLevel pSeverityLevel, String pSubject, String pMessage) {
    TrackEntry lEntry = createTrackEntry(pSubject, pMessage, pSeverityLevel, mCurrentEntry, null);
    mCurrentEntry.addChildEntry(lEntry);
    mCurrentEntry = lEntry;
  }

  @Override
  public void push(SeverityLevel pSeverityLevel, String pSubject, String pMessage, TrackFlag... pFlags) {
    TrackEntry lNewEntry = createTrackEntry(pSubject, pMessage, pSeverityLevel, mCurrentEntry, pFlags);
    mCurrentEntry.addChildEntry(lNewEntry);
    //Record other flags
    mapEntryToFlags(lNewEntry, pFlags);

    mCurrentEntry = lNewEntry;
  }

  @Override
  public void push(SeverityLevel pSeverityLevel, String pSubject) {
    TrackEntry lEntry = createTrackEntry(pSubject, "", pSeverityLevel, mCurrentEntry, null);
    mCurrentEntry.addChildEntry(lEntry);
    mCurrentEntry = lEntry;
  }

  @Override
  public void push(SeverityLevel pSeverityLevel, String pSubject, Trackable pInfo) {
    push(pSeverityLevel, pSubject, "");
    pInfo.writeTrackData();
  }

  @Override
  public void pop(String pSubject) {
    if(!pSubject.equals(mCurrentEntry.getSubject())){
      throw new IllegalArgumentException(pSubject + " does not match current subject " + mCurrentEntry.getSubject());
    }
    else {
      mCurrentEntry.setOutTime(System.currentTimeMillis());
      mCurrentEntry = mCurrentEntry.getParent();
    }
  }

  private static TimerEntry getLastTimerEntry(List<TimerEntry> lEntryList) {
    if(lEntryList.size() > 0) {
      return lEntryList.get(lEntryList.size() - 1);
    }
    else {
      return null;
    }
  }

  @Override
  public void timerStart(String pTimerName) {

    List<TimerEntry> lEntryList = mTimers.get(pTimerName);
    if(lEntryList == null) {
      lEntryList = new ArrayList<TimerEntry>();
      mTimers.put(pTimerName, lEntryList);
    }

    TimerEntry lLastEntry = getLastTimerEntry(lEntryList);
    if(lLastEntry != null && lLastEntry.mEndTime == -1) {
      //Timer is already running; increment the usage count
      lEntryList.get(lEntryList.size() - 1).mUsageCount++;
    }
    else {
      log(SeverityLevel.DEBUG, "TimerStart", pTimerName);
      lEntryList.add(new TimerEntry());
    }
  }

  @Override
  public void timerPause(String pTimerName) {
    List<TimerEntry> lEntryList = mTimers.get(pTimerName);


    if(lEntryList == null) {
      throw new IllegalArgumentException("Timer with name " + pTimerName + " has not been defined");
    }

    TimerEntry lLastEntry = getLastTimerEntry(lEntryList);

    if(lLastEntry.mEndTime != -1) {
      throw new IllegalStateException("Timer " + pTimerName + " already paused");
    }

    if(lLastEntry.mUsageCount > 1) {
      //Timer is being used further up the stack, decrement the usage count but do not pause
      lLastEntry.mUsageCount--;
    }
    else {
      log(SeverityLevel.DEBUG, "TimerPause", pTimerName);
      lLastEntry.mEndTime = System.currentTimeMillis();
    }
  }

  @Override
  public void logText(Track.SeverityLevel pSeverityLevel, String pSubject, String pText, boolean pIsXML) {
    TrackEntry lNewEntry = new StandardTrackEntry(pSubject, pText, pSeverityLevel, mCurrentEntry, null);
    lNewEntry.setType(pIsXML ? TrackEntryType.XML : TrackEntryType.TEXT);
    mCurrentEntry.addChildEntry(lNewEntry);
  }

  @Override
  public void addAttribute(String pAttrName, String pAttrValue) {
    mCurrentEntry.addChildEntry(new TrackAttribute(pAttrName, pAttrValue));
  }

  @Override
  public void recordSuppressedException(String pDescription, Throwable pSuppressedException) {
    ErrorLogger.instance().logError(pSuppressedException, ErrorLogger.ErrorType.SUPPRESSED, getRequestId(), pDescription);
  }

  @Override
  public void recordException(TrackableException pException) {
    ExceptionTrackEntry lNewEntry = new ExceptionTrackEntry(mCurrentEntry, pException.getErrorId());
    mErrorIdToErrorEntryMap.put(pException.getErrorId(), lNewEntry);
    mCurrentEntry.addChildEntry(lNewEntry);
  }

  @Override
  public TrackEntry getRootEntry() {
    return mTopEntry;
  }

  @Override
  public String getRequestId() {
    return mRequestId;
  }

  @Override
  public String getTrackId() {
    return mTrackId;
  }

  @Override
  public long getStartTimeMS() {
    return mStartTimeMS;
  }

  @Override
  public void setProperty(TrackProperty pProperty, String pValue) {
    mProperties.put(pProperty, pValue);
    log(SeverityLevel.INFO, pProperty.getTrackSubject(), pValue);
  }

  @Override
  public String getProperty(TrackProperty pProperty) {
    return mProperties.get(pProperty);
  }

  @Override
  public Date getOpenTime() {
    return new Date(mStartTimeMS);
  }

  @Override
  public Date getCloseTime() {
    return new Date(mTopEntry.getOutTime());
  }

  @Override
  public Map<TrackFlag, List<TrackEntry>> getFlagToEntryMap() {
    return mFlagsToEntries;
  }

  @Override
  public long getTimerValue(TrackTimer pTimer) {
    return getTimerValue(pTimer.getName());
  }

  @Override
  public long getTimerValue(String pTimerName) {

    List<TimerEntry> lTimerEntries = mTimers.get(pTimerName);

    if(lTimerEntries != null) {
      long lTotalTime = 0L;
      for(TimerEntry lTimerEntry : lTimerEntries) {
        lTotalTime += lTimerEntry.mEndTime - lTimerEntry.mStartTime;
      }
      return lTotalTime;
    }
    else {
      return -1L;
    }
  }

  @Override
  public Collection<String> getAllTimerNames() {
    return mTimers.keySet();
  }

  @Override
  public List<TimerEntry> getTimerEntries(String pTimerName) {
    return Collections.unmodifiableList(mTimers.get(pTimerName));
  }

  @Override
  public void counterIncrement(String pCounterName) {
    Integer lCurVal = mCounters.get(pCounterName);
    if(lCurVal == null) {
      lCurVal = 0;
    }

    mCounters.put(pCounterName, ++lCurVal);
    log(SeverityLevel.DEBUG, "CounterIncrement", pCounterName);
  }

  @Override
  public Collection<String> getAllCounterNames() {
    return mCounters.keySet();
  }

  @Override
  public int getCounterValue(String pCounterName) {
    Integer lCounterValue = mCounters.get(pCounterName);
    return lCounterValue == null ? 0 : lCounterValue;
  }

  @Override
  public void setRootException(Throwable pThrowable) {
    mRootException = pThrowable;

    //Loop through every "cause" and mark its corresponding entry as requiring serialisation (if it has one).
    //At this point we can give the ExceptionTrackEntry a reference to the Throwable because we know it needs to be serialised.
    Throwable lThrowable = pThrowable;
    do {
      if(lThrowable instanceof TrackableException) {
        String lErrorId = ((TrackableException) lThrowable).getErrorId();
        ExceptionTrackEntry lExceptionTrackEntry = mErrorIdToErrorEntryMap.get(lErrorId);
        if(lExceptionTrackEntry != null) {
          lExceptionTrackEntry.setSerialiseException(lThrowable);
        }
      }

      lThrowable = lThrowable.getCause();
    }
    while(lThrowable != null);
  }

  @Override
  public Throwable getRootException() {
    return mRootException;
  }
}
