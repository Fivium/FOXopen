package net.foxopen.fox.job;

import java.util.Date;

public class TaskCompletionMessage {

  private final Date mCompletionTime;
  private final String mTaskDescription;
  private final String mMessage;

  private long mTimeTakenMS = 0L;

  public TaskCompletionMessage(FoxJobTask pJobTask, String pMessage) {
    mTaskDescription = pJobTask.getTaskDescription();
    mCompletionTime = new Date();
    mMessage = pMessage;
  }

  Date getCompletionTime() {
    return mCompletionTime;
  }

  public String getTaskDescription() {
    return mTaskDescription;
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
