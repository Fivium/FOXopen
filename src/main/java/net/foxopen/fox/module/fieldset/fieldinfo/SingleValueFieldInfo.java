package net.foxopen.fox.module.fieldset.fieldinfo;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;


public abstract class SingleValueFieldInfo
extends FieldInfo {

  /** The value in the DOM which is being sent to the HTML form. Note the final value in the HTML form may be different if a transformer is active for this field. */
  private final String mSentValue;

  protected SingleValueFieldInfo(String pExternalName, String pDOMRef, String pChangeActionName, String pSentValue) {
    super(pExternalName, pDOMRef, pChangeActionName);
    mSentValue = pSentValue;
  }

  protected String getSentValue() {
    return mSentValue;
  }

  /**
   * Validates at most 1 field was sent, handles null replacement.
   * @param pPostedValues Array containing at most one field.
   * @param pRef For debug/error reporting.
   * @return
   */
  public static String singlePostedValue(String[] pPostedValues, String pRef) {
    if(pPostedValues == null) {
      return "";
    }
    else if(pPostedValues.length != 1) {
      throw new ExInternal("Posted value for field " + pRef + " expected exactly one text value, got " + pPostedValues.length);
    }
    else {
      return XFUtil.nvl(pPostedValues[0], "");
    }
  }

  /**
   * Validates at most 1 field was sent, handles null replacement.
   * @param pPostedValues
   * @return
   */
  protected String getSinglePostedValue(String[] pPostedValues) {
    return singlePostedValue(pPostedValues, getDOMRef());
  }
}
