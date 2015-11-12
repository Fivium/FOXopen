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
  private final String mAbsoluteURI;
  private final Map<String, String> mParamMap;
  private final boolean mBasicLink;

  public StatusAction(String pPrompt, BangHandler pBangHandler, Map<String, String> pParamMap) {
    mPrompt = pPrompt;
    mBangHandler = pBangHandler;
    mParamMap = pParamMap;
    mAbsoluteURI = null;
    mBasicLink = false;
  }

  public StatusAction(String pPrompt, BangHandler pBangHandler, Map<String, String> pParamMap, boolean pBasicLink) {
    mPrompt = pPrompt;
    mBangHandler = pBangHandler;
    mParamMap = pParamMap;
    mAbsoluteURI = null;
    mBasicLink = pBasicLink;
  }

  public StatusAction(String pPrompt, String pAbsoulteURI) {
    mPrompt = pPrompt;
    mAbsoluteURI = pAbsoulteURI;
    mBangHandler = null;
    mParamMap = null;
    mBasicLink = false;
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

    String lURI;
    RequestURIBuilder lURIBuilder = pSerialisationContext.getURIBuilder();
    if(mBangHandler != null) {
      lURIBuilder.setParams(mParamMap);
      lURI = lURIBuilder.buildBangHandlerURI(mBangHandler);
    }
    else {
      lURI = lURIBuilder.buildServletURI(mAbsoluteURI);
    }

    pWriter.append("<a href=\"").append(lURI).append("\" data-status-type=\"action\"");
    if(mBasicLink) {
      pWriter.append(" data-basic-link=\"true\"");
    }
    pWriter.append(">").append(mPrompt).append("</a>");
  }

}
