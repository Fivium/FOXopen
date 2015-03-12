package net.foxopen.fox.ex;

import net.foxopen.fox.App;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.State;
import net.foxopen.fox.module.entrytheme.EntryTheme;

public class ErrorContext {

  private String mMessage;
  private Throwable mException;
  private State mState;
  private EntryTheme mTheme;
  private Mod mModule;
  private App mApp;
  private String mErrorID;

  public ErrorContext () {
  }

  public ErrorContext (Throwable pException, String pMessage) {
    mException = pException;
    mMessage = pMessage;
  }


  public final String getMessage() {
    return mMessage;
  }

  public final String getMessage(String pDefaultMessage) {
    if (mMessage != null) {
      return mMessage;
    }
    return pDefaultMessage;
  }

  public final String getErrorID() {
    return mErrorID;
  }

  public final String getErrorID(String pDefaultID) {
    if (mErrorID != null) {
      return mErrorID;
    }
    return pDefaultID;
  }

  public final Throwable getException() {
    return mException;
  }

  public final Throwable getException(Throwable pDefaultException) {
    if (mException != null ) {
      return mException;
    }
    return pDefaultException;
  }

  public final App getApp() {
    return mApp;
  }

  public final State getState() {
    return mState;
  }

  public final EntryTheme getTheme() {
    return mTheme;
  }

  public final Mod getModule() {
    return mModule;
  }



  public final void setMessage(String pMessage) {
    this.mMessage = pMessage;
  }

  public final void setException(Throwable pException) {
    this.mException = pException;
  }

  public final void setState(State pState) {
    this.mState = pState;
  }

  public final void setTheme(EntryTheme pTheme) {
    this.mTheme = pTheme;
  }

  public final void setModule(Mod pModule) {
    this.mModule = pModule;
  }

  public final void setApp(App pApp) {
    this.mApp = pApp;
  }

  public final void setErrorID(String pErrorID) {
    this.mErrorID = pErrorID;
  }
}
