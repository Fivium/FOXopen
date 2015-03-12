package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;

public class OutputError {
  private final String mContent;
  private final String mErrorURL;
  private final String mErrorURLPrompt;
  
  public OutputError(String pContent, String pErrorURL, String pErrorURLPrompt) {
    mContent = pContent;
    mErrorURL = pErrorURL;
    mErrorURLPrompt = XFUtil.nvl(pErrorURLPrompt, "Read more...");
  }

  public String getContent() {
    return mContent;
  }

  public String getErrorURL() {
    return mErrorURL;
  }

  public String getErrorURLPrompt() {
    return mErrorURLPrompt;
  }
}
