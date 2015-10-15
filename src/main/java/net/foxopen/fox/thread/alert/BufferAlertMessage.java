package net.foxopen.fox.thread.alert;

import net.foxopen.fox.XFUtil;

import java.util.Iterator;

public class BufferAlertMessage
extends RichAlertMessage {

  private static final Iterator<String> ID_ITERATOR = XFUtil.getUniqueIterator();

  private final String mMessageId;
  private final String mBufferName;
  private final String mBufferAttachFoxId;

  public BufferAlertMessage(String pBufferName, String pBufferAttachFoxId, String pTitle, DisplayStyle pDisplayStyle, String pClosePrompt, String pCSSClass) {
    super(pTitle, pDisplayStyle, pClosePrompt, pCSSClass);
    mMessageId = ID_ITERATOR.next();
    mBufferName = pBufferName;
    mBufferAttachFoxId = pBufferAttachFoxId;
  }

  public String getMessageId() {
    return mMessageId;
  }

  public String getBufferName() {
    return mBufferName;
  }

  public String getBufferAttachFoxId() {
    return mBufferAttachFoxId;
  }
}
