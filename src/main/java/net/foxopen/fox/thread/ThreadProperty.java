package net.foxopen.fox.thread;

import net.foxopen.fox.ex.ExInternal;

public class ThreadProperty {

  public enum Type {
    EXIT_URI(false, ""),
    IS_ORPHAN(true, false),
    IS_RESUME_ALLOWED(true, false),
    IS_SKIP_FOX_SESSION_CHECK(true, false);

    private final boolean mIsBooleanValue;
    private final Object mDefaultValue;

    private Type(boolean pIsBooleanValue, Object pDefaultValue) {
      mIsBooleanValue = pIsBooleanValue;
      mDefaultValue = pDefaultValue;
    }

    Object getDefaultValue() {
      return mDefaultValue;
    }
  }

  private final Type mType;
  private final Object mValue;

  public ThreadProperty(Type pType, Object pValue) {
    mType = pType;
    if(pType.mIsBooleanValue && !(pValue instanceof Boolean)) {
      throw new ExInternal("Thread properties of type " + pType + " must be a Boolean value");
    }
    else if(!pType.mIsBooleanValue &&  !(pValue instanceof String)) {
      throw new ExInternal("Thread properties of type " + pType + " must be a String value");
    }
    mValue = pValue;
  }

  public boolean booleanValue() {
    if(mType.mIsBooleanValue) {
      return (Boolean) mValue;
    }
    else {
      throw new ExInternal("Thread property " + mType + " is not a Boolean value");
    }
  }

  public String stringValue() {
    if(!mType.mIsBooleanValue) {
      return (String) mValue;
    }
    else {
      throw new ExInternal("Thread property " + mType + " is not a String value");
    }
  }

  @Override
  public String toString() {
    return mValue.toString();
  }
}
