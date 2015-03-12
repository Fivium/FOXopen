package net.foxopen.fox.track;

import net.foxopen.fox.ex.TrackableException;


public final class Track {

  private static ThreadLocal<TrackLogger> gTrackLogger = new ThreadLocal<TrackLogger>() {
    protected TrackLogger initialValue() {
      return new NoOpTrackLogger();
    }
  };

  //TODO this should probably catch errors from track and stop further ones from propogating, i.e. if a pop was wrong and further pops are done

  static enum SeverityLevel{
    ALERT, INFO, DEBUG;
  }

  private Track() {}

  public static void startTracking(TrackLogger pLogger){
    gTrackLogger.set(pLogger);
  }

  public static void stopTracking(){
    gTrackLogger.remove();
  }

  public static void open(String pTrackName){
    gTrackLogger.get().open(pTrackName);
  }

  public static void close(){
    gTrackLogger.get().close();
  }

  public static void alert(String pSubject, String pMessage, TrackFlag... pFlags){
    gTrackLogger.get().log(SeverityLevel.ALERT, pSubject, pMessage, pFlags);
  }

  public static void alert(String pSubject, String pMessage){
    gTrackLogger.get().log(SeverityLevel.ALERT, pSubject, pMessage);
  }

  public static void alert(String pMessage){
    gTrackLogger.get().log(SeverityLevel.ALERT, pMessage);
  }

  public static void info(String pSubject, String pMessage, TrackFlag... pFlags){
    gTrackLogger.get().log(SeverityLevel.INFO, pSubject, pMessage, pFlags);
  }

  public static void info(String pSubject, String pMessage){
    gTrackLogger.get().log(SeverityLevel.INFO, pSubject, pMessage);
  }

  public static void info(String pMessage){
    gTrackLogger.get().log(SeverityLevel.INFO, pMessage);
  }

  public static void debug(String pSubject, String pMessage, TrackFlag... pFlags){
    gTrackLogger.get().log(SeverityLevel.DEBUG, pSubject, pMessage, pFlags);
  }

  public static void debug(String pSubject, String pMessage){
    gTrackLogger.get().log(SeverityLevel.DEBUG, pSubject, pMessage);
  }

  public static void debug(String pMessage){
    gTrackLogger.get().log(SeverityLevel.DEBUG, pMessage);
  }

  public static void alert(String pSubject, Trackable pInfo){
    gTrackLogger.get().log(SeverityLevel.ALERT, pSubject, pInfo);
  }

  public static void info(String pSubject, Trackable pInfo){
    gTrackLogger.get().log(SeverityLevel.INFO, pSubject, pInfo);
  }

  public static void debug(String pSubject, Trackable pInfo){
    gTrackLogger.get().log(SeverityLevel.DEBUG, pSubject, pInfo);
  }

  public static void pushAlert(String pSubject, Trackable pInfo){
    gTrackLogger.get().push(SeverityLevel.ALERT, pSubject, pInfo);
  }

  public static void pushInfo(String pSubject, Trackable pInfo){
    gTrackLogger.get().push(SeverityLevel.INFO, pSubject, pInfo);
  }

  public static void pushDebug(String pSubject, Trackable pInfo){
    gTrackLogger.get().push(SeverityLevel.DEBUG, pSubject, pInfo);
  }

  public static void pushAlert(String pSubject, String pMessage){
    gTrackLogger.get().push(SeverityLevel.ALERT, pSubject, pMessage);
  }

  public static void pushAlert(String pSubject, String pMessage, TrackTimer pTimer){
    gTrackLogger.get().push(SeverityLevel.ALERT, pSubject, pMessage);
    timerStart(pTimer);
  }

   public static void pushAlert(String pSubject, String pMessage, TrackFlag... pFlags){
    gTrackLogger.get().push(SeverityLevel.ALERT, pSubject, pMessage, pFlags);
  }

  public static void pushAlert(String pSubject){
    gTrackLogger.get().push(SeverityLevel.ALERT, pSubject);
  }

  public static void pushInfo(String pSubject, String pMessage){
    gTrackLogger.get().push(SeverityLevel.INFO, pSubject, pMessage);
  }

  public static void pushInfo(String pSubject, String pMessage, TrackTimer pTimer){
    gTrackLogger.get().push(SeverityLevel.INFO, pSubject, pMessage);
    timerStart(pTimer);
  }

  public static void pushInfo(String pSubject, String pMessage, TrackFlag... pFlags){
    gTrackLogger.get().push(SeverityLevel.INFO, pSubject, pMessage, pFlags);
  }

  public static void pushInfo(String pSubject){
    gTrackLogger.get().push(SeverityLevel.INFO, pSubject);
  }

  public static void pushDebug(String pSubject, String pMessage){
    gTrackLogger.get().push(SeverityLevel.DEBUG, pSubject, pMessage);
  }

  public static void pushDebug(String pSubject, String pMessage, TrackTimer pTimer){
    gTrackLogger.get().push(SeverityLevel.DEBUG, pSubject, pMessage);
    timerStart(pTimer);
  }

  public static void pushDebug(String pSubject, String pMessage, TrackFlag... pFlags){
    gTrackLogger.get().push(SeverityLevel.DEBUG, pSubject, pMessage, pFlags);
  }

  public static void pushDebug(String pSubject){
    gTrackLogger.get().push(SeverityLevel.DEBUG, pSubject);
  }

  public static void pop(String pSubject){
    gTrackLogger.get().pop(pSubject);
  }

  public static void pop(String pSubject, TrackTimer pTimer){
    timerPause(pTimer);
    gTrackLogger.get().pop(pSubject);
  }

  public static void timerStart(String pTimerName){
    gTrackLogger.get().timerStart(pTimerName);
  }

  public static void timerPause(String pTimerName){
    gTrackLogger.get().timerPause(pTimerName);
  }

  public static void timerStart(TrackTimer pTimer){
    gTrackLogger.get().timerStart(pTimer.getName());
  }

  public static void timerPause(TrackTimer pTimer){
    gTrackLogger.get().timerPause(pTimer.getName());
  }

  public static void counterIncrement(String pCounterName){
    gTrackLogger.get().counterIncrement(pCounterName);
  }

  public static void logAlertText(String pSubject, String pText){
    gTrackLogger.get().logText(SeverityLevel.ALERT, pSubject, pText, false);
  }

  public static void logInfoText(String pSubject, String pText){
    gTrackLogger.get().logText(SeverityLevel.INFO, pSubject, pText, false);
  }

  public static void logDebugText(String pSubject, String pText){
    gTrackLogger.get().logText(SeverityLevel.DEBUG, pSubject, pText, false);
  }

  public static void logAlertXMLString(String pSubject, String pXMLString){
    gTrackLogger.get().logText(SeverityLevel.ALERT, pSubject, pXMLString, true);
  }

  public static void logInfoXMLString(String pSubject, String pXMLString){
    gTrackLogger.get().logText(SeverityLevel.INFO, pSubject, pXMLString, true);
  }

  public static void logDebugXMLString(String pSubject, String pXMLString){
    gTrackLogger.get().logText(SeverityLevel.DEBUG, pSubject, pXMLString, true);
  }


  public static void setProperty(TrackProperty pProperty, String pValue){
    gTrackLogger.get().setProperty(pProperty, pValue);
  }

  public static void addAttribute(String pAttrName, String pAttrValue){
    gTrackLogger.get().addAttribute(pAttrName, pAttrValue);
  }

  public static void recordSuppressedException(String pDescription, Throwable pSuppressedError){
    gTrackLogger.get().recordSuppressedException(pDescription, pSuppressedError);
  }

  public static void recordException(TrackableException pException){
    gTrackLogger.get().recordException(pException);
  }

  public static void setRootException(Throwable pThrowable){
    gTrackLogger.get().setRootException(pThrowable);
  }
  public static String currentTrackId() {
    return gTrackLogger.get().getTrackId();
  }
}
