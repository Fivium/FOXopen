package net.foxopen.fox.track;

import net.foxopen.fox.ex.TrackableException;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;


class NoOpTrackLogger
implements TrackLogger {

  NoOpTrackLogger() {}

  @Override
  public void open(String pTrackName) {}

  @Override
  public void close() {}

  @Override
  public void log(Track.SeverityLevel pSeverityLevel, String pMessage) {}

  @Override
  public void log(Track.SeverityLevel pSeverityLevel, String pSubject, String pMessage) {}

  @Override
  public void log(Track.SeverityLevel pSeverityLevel, String pSubject, String pMessage, TrackFlag... pFlags) {}

  @Override
  public void log(Track.SeverityLevel pSeverityLevel, String pSubject, Trackable pInfo) {}

  @Override
  public void push(Track.SeverityLevel pSeverityLevel, String pSubject, String pMessage) {}

  @Override
  public void push(Track.SeverityLevel pSeverityLevel, String pSubject, String pMessage, TrackFlag... pFlags) {}

  @Override
  public void push(Track.SeverityLevel pSeverityLevel, String pSubject) {}

  @Override
  public void push(Track.SeverityLevel pSeverityLevel, String pSubject, Trackable pInfo) {}

  @Override
  public void pop(String pSubject) {}

  @Override
  public void timerStart(String pTimerName) {
  }

  @Override
  public void timerPause(String pTimerName) {
  }


  @Override
  public void logText(Track.SeverityLevel pSeverityLevel, String pSubject, String pText, boolean pIsXML) {
  }

  @Override
  public void recordSuppressedException(String pDescription, Throwable pSuppressedError) {
  }

  @Override
  public String getRequestId() {
    return null;
  }

  @Override
  public String getTrackId() {
    return "";
  }

  @Override
  public TrackEntry getRootEntry() {
    return null;
  }

  @Override
  public long getStartTimeMS() {
    return 0L;
  }
  @Override
  public Map<TrackFlag, List<TrackEntry>> getFlagToEntryMap() {
    return Collections.emptyMap();
  }

  @Override
  public void setProperty(TrackProperty pProperty, String pValue) {
  }

  @Override
  public String getProperty(TrackProperty pProperty) {
    return null;
  }

  @Override
  public void addAttribute(String pAttrName, String pAttrValue) {
  }

  @Override
  public long getTimerValue(TrackTimer pTimer) {
    return 0L;
  }

  @Override
  public long getTimerValue(String pTimerName) {
    return 0L;
  }

  @Override
  public Collection<String> getAllTimerNames() {
    return Collections.emptySet();
  }

  @Override
  public List<TimerEntry> getTimerEntries(String pTimerName) {
    return Collections.emptyList();
  }

  @Override
  public Date getCloseTime() {
    return null;
  }

  @Override
  public Date getOpenTime() {
    return null;
  }

  @Override
  public void counterIncrement(String pCounterName) {
  }

  @Override
  public Collection<String> getAllCounterNames() {
    return Collections.emptySet();
  }

  @Override
  public int getCounterValue(String pCounterName) {
    return 0;
  }

  @Override
  public void recordException(TrackableException pException) {
  }

  @Override
  public void setRootException(Throwable pThrowable) {
  }

  @Override
  public Throwable getRootException() {
    return null;
  }
}
