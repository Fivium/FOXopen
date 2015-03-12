package net.foxopen.fox.plugin.api.track;

import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.Trackable;


public final class FxpTrack {

  private FxpTrack() {}

  public static void open(String pTrackName){
    Track.open(pTrackName);
  }

  public static void close(){
    Track.close();
  }

//  public static void alert(String pSubject, String pMessage, TrackFlag... pFlags){
//    Track2.alert(pSubject, pMessage, pFlags);
//  }

  public static void alert(String pSubject, String pMessage){
    Track.alert(pSubject, pMessage);
  }

  public static void alert(String pMessage){
    Track.alert(pMessage);
  }

//  public static void info(String pSubject, String pMessage, TrackFlag... pFlags){
//    Track2.info(pSubject, pMessage, pFlags);
//  }

  public static void info(String pSubject, String pMessage){
    Track.info(pSubject, pMessage);
  }

  public static void info(String pMessage){
    Track.info(pMessage);
  }

//  public static void debug(String pSubject, String pMessage, TrackFlag... pFlags){
//    Track2.debug(pSubject, pMessage, pFlags);
//  }

  public static void debug(String pSubject, String pMessage){
    Track.debug(pSubject, pMessage);
  }

  public static void debug(String pMessage){
    Track.debug(pMessage);
  }

  public static void alert(String pSubject, FxpTrackable pInfo){
    Track.alert(pSubject, (Trackable) pInfo);
  }

  public static void info(String pSubject, FxpTrackable pInfo){
    Track.info(pSubject, (Trackable) pInfo);
  }

  public static void debug(String pSubject, FxpTrackable pInfo){
    Track.debug(pSubject, (Trackable) pInfo);
  }

  public static void pushAlert(String pSubject, FxpTrackable pInfo){
    Track.pushAlert(pSubject, (Trackable) pInfo);
  }

  public static void pushInfo(String pSubject, FxpTrackable pInfo){
    Track.pushInfo(pSubject, (Trackable) pInfo);
  }

  public static void pushDebug(String pSubject, FxpTrackable pInfo){
    Track.pushDebug(pSubject, (Trackable) pInfo);
  }

  public static void pushAlert(String pSubject, String pMessage){
    Track.pushAlert(pSubject, pMessage);
  }

//   public static void pushAlert(String pSubject, String pMessage, TrackFlag... pFlags){
//    Track2.pushAlert(pSubject, pMessage, pFlags);
//  }

  public static void pushAlert(String pSubject){
    Track.pushAlert(pSubject);
  }

  public static void pushInfo(String pSubject, String pMessage){
    Track.pushInfo(pSubject, pMessage);
  }

//  public static void pushInfo(String pSubject, String pMessage, TrackFlag... pFlags){
//    Track2.pushInfo(pSubject, pMessage, pFlags);
//  }

  public static void pushInfo(String pSubject){
    Track.pushInfo(pSubject);
  }

  public static void pushDebug(String pSubject, String pMessage){
    Track.pushDebug(pSubject, pMessage);
  }

//  public static void pushDebug(String pSubject, String pMessage, TrackFlag... pFlags){
//    Track2.pushDebug(pSubject, pMessage, pFlags);
//  }

  public static void pushDebug(String pSubject){
    Track.pushDebug(pSubject);
  }

  public static void pop(String pSubject){
    Track.pop(pSubject);
  }

  public static void timerStart(String pTimerName){
    Track.timerStart(pTimerName);
  }

  public static void timerPause(String pTimerName){
    Track.timerPause(pTimerName);
  }

  public static void logAlertText(String pSubject, String pText){
    Track.logAlertText(pSubject, pText);
  }

  public static void logInfoText(String pSubject, String pText){
    Track.logInfoText(pSubject, pText);
  }

  public static void logDebugText(String pSubject, String pText){
    Track.logDebugText(pSubject, pText);
  }

  public static void recordSuppressedError(String pDescription, Throwable pSuppressedError){
    Track.recordSuppressedException(pDescription, pSuppressedError);
  }

}

