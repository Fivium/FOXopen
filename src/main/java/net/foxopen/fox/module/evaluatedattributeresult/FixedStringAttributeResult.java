package net.foxopen.fox.module.evaluatedattributeresult;

public class FixedStringAttributeResult implements StringAttributeResult {
  private final String mFixedValue;

  public FixedStringAttributeResult(String pFixedValue) {
    mFixedValue = pFixedValue;
  }

  @Override
  public String getString() {
    return mFixedValue;
  }

  @Override
  public boolean isEscapingRequired() {
    return false;
  }

}
