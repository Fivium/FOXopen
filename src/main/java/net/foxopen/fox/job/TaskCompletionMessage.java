package net.foxopen.fox.job;

import java.util.Date;

public class TaskCompletionMessage {

  private final Date mCompletionTime;
  private final String mMessage;

  private long mTimeTakenMS = 0L;

  public TaskCompletionMessage(String pMessage) {
    mCompletionTime = new Date();
    mMessage = pMessage;
  }

  Date getCompletionTime() {
    return mCompletionTime;
  }

  String getMessage() {
    return mMessage;
  }

  void setTimeTakenMS(long pTimeTakenMS) {
    mTimeTakenMS = pTimeTakenMS;
  }

  long getTimeTakenMS() {
    return mTimeTakenMS;
  }
}
