package net.foxopen.fox.enginestatus;


import net.foxopen.fox.ex.ExInternal;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Lengthy detail messages which are presented to the user in a modal popup.
 */
public class StatusDetail
implements NamedStatusItem {

  private final String mTitle;
  private final Provider mProvider;
  private final boolean mPreserveFormatting;

  public StatusDetail(String pTitle, final String pDetailText) {
    mTitle = pTitle;
    mProvider = new Provider() {
      @Override
      public StatusItem getDetailMessage() {
        return new StatusText(pDetailText);
      }
    };
    mPreserveFormatting = true;
  }

  public StatusDetail(String pTitle, final StatusItem pDetailItem) {
    mTitle = pTitle;
    mProvider = new Provider() {
      @Override
      public StatusItem getDetailMessage() {
        return pDetailItem;
      }
    };
    mPreserveFormatting = false;
  }

  public StatusDetail(String pTitle, Provider pProvider) {
    mTitle = pTitle;
    mProvider = pProvider;
    mPreserveFormatting = false;
  }


  @Override
  public String getMnem() {
    return EngineStatus.promptToMnem(mTitle);
  }

  @Override
  public MessageLevel getMaxMessageSeverity() {
    return mProvider.getDetailMessage().getMaxMessageSeverity();
  }

  @Override
  public String getItemName() {
    return mTitle;
  }

  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext, String pLinkPrompt)
  throws IOException {
    pWriter.append("<a data-status-type=\"detail\" href=\"" + pSerialisationContext.getDetailURI(this) + "\">" + pLinkPrompt + "</a>");
  }

  @Override
  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext)
  throws IOException {
    serialiseHTML(pWriter, pSerialisationContext, mTitle);
  }

  public String getContent(StringWriter pWriter) {

    pWriter.append("<html><body>");

    if(mPreserveFormatting) {
      pWriter.append("<pre>");
    }

    //TODO PN not null
    try {
      mProvider.getDetailMessage().serialiseHTML(pWriter, new StatusSerialisationContext(null));
    }
    catch (IOException e) {
      throw new ExInternal("Failed to generate detail message", e);
    }

    if(mPreserveFormatting) {
      pWriter.append("</pre>");
    }

    pWriter.append("</body></html>");

    return pWriter.toString();
  }

  public static interface Provider {
    StatusItem getDetailMessage();
  }
}
