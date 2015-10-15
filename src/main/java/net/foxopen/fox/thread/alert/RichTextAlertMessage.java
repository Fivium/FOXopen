package net.foxopen.fox.thread.alert;

public class RichTextAlertMessage
extends RichAlertMessage {

  private final String mMessage;
  private final boolean mEscapingRequired;

  public RichTextAlertMessage(String pMessage, String pTitle, DisplayStyle pDisplayStyle, String pClosePrompt, String pCSSClass, boolean pEscapingRequired) {
    super(pTitle, pDisplayStyle, pClosePrompt, pCSSClass);
    mMessage = pMessage;
    mEscapingRequired = pEscapingRequired;
  }

  public String getMessage() {
    return mMessage;
  }

  public boolean isEscapingRequired() {
    return mEscapingRequired;
  }
}
