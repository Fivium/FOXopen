package net.foxopen.fox.enginestatus;


import java.io.IOException;
import java.io.Writer;

public class StatusMessage
implements NamedStatusItem {

  private final String mTitle;
  private final StatusItem mMessage;
  private final MessageLevel mLevel;

  public StatusMessage(String pTitle, String pMessage) {
    mTitle = pTitle;
    mMessage = new StatusText(pMessage);
    mLevel = MessageLevel.INFO;
  }

  public StatusMessage(String pTitle, String pMessage, MessageLevel pLevel) {
    mTitle = pTitle;
    mMessage = new StatusText(pMessage, pLevel);
    mLevel = pLevel;
  }

  public StatusMessage(String pTitle, StatusItem pMessage) {
    mTitle = pTitle;
    mMessage = pMessage;
    mLevel = MessageLevel.INFO;
  }

  @Override
  public String getItemName() {
    return mTitle;
  }

  @Override
  public String getMnem() {
    return "";
  }

  @Override
  public MessageLevel getMaxMessageSeverity() {
    return mLevel;
  }

  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext, boolean pIncludeTitle)
  throws IOException {
    if(pIncludeTitle) {
      pWriter.append("<strong>").append(mTitle).append("</strong> = ");
    }

    pWriter.append("<span style=\"").append(mLevel.cssClass()).append("\">");
    mMessage.serialiseHTML(pWriter, pSerialisationContext);
    pWriter.append("</span>");
  }

  @Override
  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext)
  throws IOException {
    serialiseHTML(pWriter, pSerialisationContext, true);
  }
}
