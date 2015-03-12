package net.foxopen.fox.enginestatus;


import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class StatusAction
implements StatusItem {

  private final String mPrompt;
  private final BangHandler mBangHandler;
  private final Map<String, String> mParamMap;

  public StatusAction(String pPrompt, BangHandler pBangHandler, Map<String, String> pParamMap) {
    mPrompt = pPrompt;
    mBangHandler = pBangHandler;
    mParamMap = pParamMap;
  }

  @Override
  public String getMnem() {
    return "";
  }

  @Override
  public MessageLevel getMaxMessageSeverity() {
    return MessageLevel.INFO;
  }

  @Override
  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext)
  throws IOException {

    RequestURIBuilder lURIBuilder = pSerialisationContext.getURIBuilder().setParams(mParamMap);
    String lURI = lURIBuilder.buildBangHandlerURI(mBangHandler);

    pWriter.append("<a href=\"").append(lURI).append("\">").append(mPrompt).append("</a>");
  }

}
