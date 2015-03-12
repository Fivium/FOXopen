package net.foxopen.fox.thread;

import net.foxopen.fox.command.XDoResult;

public class PopupXDoResult
implements XDoResult {

  private final String mURI;
  private final String mWindowType = "WINDOW";
  private final String mWindowName;
  private final String mWindowFeatures;

  public PopupXDoResult(String pURI, String pWindowName, String pWindowFeatures) {
    mURI = pURI;
    mWindowName = pWindowName;
    mWindowFeatures = pWindowFeatures;
  }

  public String getURI() {
    return mURI;
  }

  public String getWindowName() {
    return mWindowName;
  }

  public String getWindowFeatures() {
    return mWindowFeatures;
  }

}
