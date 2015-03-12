package net.foxopen.fox.enginestatus;


import java.io.IOException;
import java.io.Writer;

public class StatusText
implements StatusItem {

  private final String mText;
  private final MessageLevel mMessageLevel;

  public StatusText(String pText) {
    mText = pText;
    mMessageLevel = MessageLevel.INFO;
  }

  public StatusText(String pText, MessageLevel pMessageLevel) {
    mText = pText;
    mMessageLevel = pMessageLevel;
  }

  @Override
  public String getMnem() {
    return "";
  }

  @Override
  public MessageLevel getMaxMessageSeverity() {
    return mMessageLevel;
  }

  @Override
  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext)
  throws IOException {
    pWriter.append("<span class=\"").append(mMessageLevel.cssClass()).append("\">").append(EngineStatus.textToHtml(mText)).append("</span>");
  }
}
