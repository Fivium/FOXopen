package net.foxopen.fox.thread.devtoolbar;

import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseByteStream;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;

import java.util.Collections;
import java.util.Set;

public class ViewDOMBangHandler
extends DevToolbarBangHandler {

  private static final ViewDOMBangHandler INSTANCE = new ViewDOMBangHandler();

  public static ViewDOMBangHandler instance() {
    return INSTANCE;
  }

  public static final String DOM_NAME_PARAM = "dom_name";

  private ViewDOMBangHandler() { }

  @Override
  protected Set<String> getAdditionalParamList() {
    return Collections.singleton(DOM_NAME_PARAM);
  }

  @Override
  public String getAlias() {
    return "VIEWDOM";
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.INTERNAL_SUPPORT;
  }

  @Override
  protected FoxResponse getResponseInternal(RequestContext pRequestContext, StatefulXThread pXThread) {
    DOM lDOM =  pXThread.getDevToolbarContext().getDebugDOM(pRequestContext, pRequestContext.getFoxRequest().getParameter(DOM_NAME_PARAM));

    FoxResponseByteStream lByteStreamResponse = new FoxResponseByteStream("text/xml", pRequestContext.getFoxRequest(), 0);
    lDOM.outputDocumentToOutputStream(lByteStreamResponse.getHttpServletOutputStream(), true);

    return lByteStreamResponse;
  }
}
