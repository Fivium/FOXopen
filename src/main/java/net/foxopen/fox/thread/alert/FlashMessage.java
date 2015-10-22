package net.foxopen.fox.thread.alert;

import net.foxopen.fox.command.XDoResult;
import net.foxopen.fox.module.NotificationDisplayType;

/**
 * A text message to be displayed to the user as a non-modal "flash" at the top of the screen.
 */
public class FlashMessage
implements XDoResult {

  private final String mMessage;
  private final boolean mEscapingRequired;
  private final NotificationDisplayType mDisplayType;
  private final String mCSSClass;

  /**
   * Creates a new FlashMessage.
   * @param pMessage Message text to display. This may contain HTML tags if pEscapingRequired is false.
   * @param pEscapingRequired If true, HTML tags in the message will be escaped.
   * @param pDisplayType Styling to apply to the flash container.
   * @param pCSSClass Additional CSS class(es) to set on the flash container.
   */
  public FlashMessage(String pMessage, boolean pEscapingRequired, NotificationDisplayType pDisplayType, String pCSSClass) {
    mMessage = pMessage;
    mEscapingRequired = pEscapingRequired;
    mDisplayType = pDisplayType;
    mCSSClass = pCSSClass;
  }

  /**
   * @return The message to display in the flash - may contain HTML tags.
   */
  public String getMessage() {
    return mMessage;
  }

  /**
   * @return True if HTML tags in the message text should be escaped.
   */
  public boolean isEscapingRequired() {
    return mEscapingRequired;
  }

  /**
   * @return NotificationDisplayType styling rules to be applied to the flash container.
   */
  public NotificationDisplayType getDisplayType() {
    return mDisplayType;
  }

  /**
   * @return Additional CSS class(es) to set on the flash container.
   */
  public String getCSSClass() {
    return mCSSClass;
  }
}
