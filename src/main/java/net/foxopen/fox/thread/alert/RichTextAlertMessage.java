package net.foxopen.fox.thread.alert;

import net.foxopen.fox.module.NotificationDisplayType;

/**
 * A textual alert message which may contain arbitrary HTML.
 */
public class RichTextAlertMessage
extends RichAlertMessage {

  private final String mMessage;
  private final boolean mEscapingRequired;

  /**
   * Creates a new alert message for displaying rich text.
   * @param pMessage Message string to be displayed, which may contain HTML tags.
   * @param pTitle Title of the alert message.
   * @param pDisplayType Basic display styling to apply to the alert. Can be null if no styling is required.
   * @param pClosePrompt Prompt of the "close" button of the alert (i.e. "OK").
   * @param pCSSClass Additional CSS classes to apply to the alert modal.
   * @param pEscapingRequired If true, HTML entities in the message string will be escaped. This must be set to true
   *                          if the message is likely to contain user input data, to mitigate against XSS attacks.
   */
  public RichTextAlertMessage(String pMessage, String pTitle, NotificationDisplayType pDisplayType, String pClosePrompt, String pCSSClass, boolean pEscapingRequired) {
    super(pTitle, pDisplayType, pClosePrompt, pCSSClass);
    mMessage = pMessage;
    mEscapingRequired = pEscapingRequired;
  }

  /**
   * @return Gets the alert message to be displayed, which may contain HTML tags.
   */
  public String getMessage() {
    return mMessage;
  }

  /**
   * @return True if HTML tags in the message should be escaped before being sent to the page.
   */
  public boolean isEscapingRequired() {
    return mEscapingRequired;
  }
}
