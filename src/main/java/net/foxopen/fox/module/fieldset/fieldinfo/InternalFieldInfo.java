package net.foxopen.fox.module.fieldset.fieldinfo;

import net.foxopen.fox.module.fieldset.PostedValueProcessor;

/**
 * FieldInfo for internal (typically hidden) fields. These will not apply changes to DOMs but may need to actuate other
 * modifications on the thread/module call.
 */
public abstract class InternalFieldInfo
implements PostedValueProcessor {

  /** HTML name for this FieldInfo */
  private final String mExternalName;
  private final String mSentValue;

  protected InternalFieldInfo(String pExternalName, String pSentValue) {
    mExternalName = pExternalName;
    mSentValue = pSentValue;
  }

  @Override
  public String getExternalName() {
    return mExternalName;
  }

  protected String getSentValue() {
    return mSentValue;
  }
}
