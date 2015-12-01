package net.foxopen.fox.thread;

import net.foxopen.fox.ex.ExInternal;

public class ThreadProperty {

  public enum Type {
    /** URI to redirect the user to when a thread is exited. */
    EXIT_URI(false, ""),
    /** Action to run when the thread is resumed from hibernation. */
    HIBERNATE_RESUME_ACTION(false, ""),
    /** Tracks whether a thread is hibernated (must be resumed before actions can be run). */
    IS_HIBERNATED(true, false),
    /** If true, assert commands fail gracefully so assertion failures can be reported in bulk. */
    IS_ASSERTION_MODE(true, false),
    /** Tracks if the thread was created modelessly. */
    IS_ORPHAN(true, false),
    /** Tracks if the thread can be resumed from a GET request. */
    IS_RESUME_ALLOWED(true, false),
    /** If true, the FOX session for the thread will not be validated. */
    IS_SKIP_FOX_SESSION_CHECK(true, false);

    private final boolean mIsBooleanValue;
    private final Object mDefaultValue;

    Type(boolean pIsBooleanValue, Object pDefaultValue) {
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
