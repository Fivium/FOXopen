package net.foxopen.fox.module;

import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;

public class OutputHint {
  private final String mHintID;
  private final StringAttributeResult mTitle;
  private final StringAttributeResult mContent;
  private final StringAttributeResult mDescription;
  private final String mHintURL;

  public OutputHint(String pHintID, StringAttributeResult pTitle, StringAttributeResult pContent, StringAttributeResult pDescription, String pHintURL) {
    mHintID = pHintID;
    mTitle = pTitle;
    mContent = pContent;
    mDescription = pDescription;
    mHintURL = pHintURL;
  }

  public String getHintID() {
    return mHintID;
  }

  public StringAttributeResult getTitle() {
    return mTitle;
  }

  public StringAttributeResult getContent() {
    return mContent;
  }

  public StringAttributeResult getDescription() {
    return mDescription;
  }

  public String getHintURL() {
    return mHintURL;
  }
}
