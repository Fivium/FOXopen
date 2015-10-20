package net.foxopen.fox.thread.alert;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.NotificationDisplayType;

import java.util.Iterator;

/**
 * An alert message which displays serialised buffer content in a modal popover.
 */
public class BufferAlertMessage
extends RichAlertMessage {

  /** Used to assign an ID to the message at construction time, so buffer serialisation can be linked to the alert display JS.  */
  private static final Iterator<String> ID_ITERATOR = XFUtil.getUniqueIterator();

  private final String mMessageId;
  private final String mBufferName;
  private final String mBufferAttachFoxId;

  /**
   * Creates a new alert message for displaying buffer content.
   * @param pBufferName Name of buffer to be displayed.
   * @param pBufferAttachFoxId FOXID of the attach node for buffer evaluation.
   * @param pTitle Alert title.
   * @param pDisplayType Basic display styling to apply to the alert. Can be null if no styling is required.
   * @param pClosePrompt Prompt of the "close" button of the alert (i.e. "OK").
   * @param pCSSClass Additional CSS classes to apply to the alert modal.
   */
  public BufferAlertMessage(String pBufferName, String pBufferAttachFoxId, String pTitle, NotificationDisplayType pDisplayType, String pClosePrompt, String pCSSClass) {
    super(pTitle, pDisplayType, pClosePrompt, pCSSClass);
    mMessageId = ID_ITERATOR.next();
    mBufferName = pBufferName;
    mBufferAttachFoxId = pBufferAttachFoxId;
  }

  /**
   * @return Gets the unique ID for this BufferAlertMessage within a churn.
   */
  public String getMessageId() {
    return mMessageId;
  }

  /**
   * @return Gets the name of the buffer to use as the alert content.
   */
  public String getBufferName() {
    return mBufferName;
  }

  /**
   * @return Gets the FOXID for buffer attach point evaluation.
   */
  public String getBufferAttachFoxId() {
    return mBufferAttachFoxId;
  }
}
