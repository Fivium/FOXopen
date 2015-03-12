package net.foxopen.fox.enginestatus;

public enum MessageLevel {
  SUCCESS(0),
  INFO(0),
  WARNING(1),
  ERROR(2);

  private final int mSeverity;

  MessageLevel(int pSeverity) {
    mSeverity = pSeverity;
  }

  public String asHTML(){
    return "<span class=\"message-" + this.toString().toLowerCase() + "\">" + this.toString() + "</span>";
  }

  public boolean requiresAttention() {
    return this == WARNING || this == ERROR;
  }

  public String cssClass() {
    return "message-" + this.toString().toLowerCase();
  }

  public int intValue() {
    return mSeverity;
  }
}
