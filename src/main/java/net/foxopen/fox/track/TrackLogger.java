package net.foxopen.fox.track;

import net.foxopen.fox.ex.TrackableException;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;


public interface TrackLogger {

  public void open(String pTrackName);

  public void close();

  public void log(Track.SeverityLevel pSeverityLevel, String pMessage);

  public void log(Track.SeverityLevel pSeverityLevel, String pSubject, String pMessage);

  public void log(Track.SeverityLevel pSeverityLevel, String pSubject, String pMessage, TrackFlag... pFlags);

  public void log(Track.SeverityLevel pSeverityLevel, String pSubject, Trackable pInfo);

  public void push(Track.SeverityLevel pSeverityLevel, String pSubject, String pMessage);

  public void push(Track.SeverityLevel pSeverityLevel, String pSubject, String pMessage, TrackFlag... pFlags);

  public void push(Track.SeverityLevel pSeverityLevel, String pSubject);

  public void push(Track.SeverityLevel pSeverityLevel, String pSubject, Trackable pInfo);

  public void pop(String pSubject);

  public void timerStart(String pTimerName);

  public void timerPause(String pTimerName);

  public void counterIncrement(String pCounterName);

  public void logText(Track.SeverityLevel pSeverityLevel, String pSubject, String pText, boolean pIsXML);

  public void setProperty(TrackProperty pProperty, String pValue);

  public String getProperty(TrackProperty pProperty);

  public void addAttribute(String pAttrName, String pAttrValue);

  public void recordSuppressedException(String pDescription, Throwable pSuppressedException);

  public void recordException(TrackableException pException);

  public void setRootException(Throwable pThrowable);

  /**
   * Get the root exception, or null if no root exception has been set.
   *
   * @return Throwable root exception
   */
  public Throwable getRootException();

  public Date getOpenTime();

  public Date getCloseTime();

  public String getRequestId();

  public String getTrackId();

  public TrackEntry getRootEntry();

  public long getStartTimeMS();

  /**
   * Returns -1 if the timer was not started.
   * @param pTimerName
   * @return
   */
  public long getTimerValue(TrackTimer pTimer);

  /**
   * Returns -1 if the timer was not started.
   * @param pTimerName
   * @return
   */
  public long getTimerValue(String pTimerName);

  /**
   * Gets the names of all the timers registered on this TrackLogger.
   * @return
   */
  public Collection<String> getAllTimerNames();


  /**
   *
   * @return
   */
  public int getCounterValue(String pCounterName);


  public Collection<String> getAllCounterNames();

  public List<TimerEntry> getTimerEntries(String pTimerName);

  public Map<TrackFlag, List<TrackEntry>> getFlagToEntryMap();
}
