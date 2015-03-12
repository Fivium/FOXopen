package net.foxopen.fox.thread.stack.transform;

public class ModelessWindowOptions {
  
  private final String mWindowName;
  private final String mWindowProperties;

  public ModelessWindowOptions(String pWindowName, String pWindowOptions) {
    mWindowName = pWindowName;
    mWindowProperties = pWindowOptions;
  }

  public String getWindowName() {
    return mWindowName;
  }

  public String getWindowProperties() {
    return mWindowProperties;
  }
}
