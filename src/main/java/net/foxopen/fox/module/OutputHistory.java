package net.foxopen.fox.module;

public class OutputHistory {
  private final String mLabel;
  private final String mOperation;
  private final String mValue;

  public OutputHistory(String pLabel, String pOperation, String pValue) {
    mLabel = pLabel;
    mOperation = pOperation;
    mValue = pValue;
  }

  public String getLabel() {
    return mLabel;
  }

  public String getOperation() {
    return mOperation;
  }

  public String getValue() {
    return mValue;
  }
}
