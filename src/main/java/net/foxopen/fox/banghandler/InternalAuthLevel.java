package net.foxopen.fox.banghandler;

public enum InternalAuthLevel {
  NONE(0),
  INTERNAL_SUPPORT(1),
  INTERNAL_ADMIN(2);

  private final int mIntValue;

  InternalAuthLevel(int pIntValue) {
    mIntValue = pIntValue;
  }

  public int intValue() {
    return mIntValue;
  }
}
