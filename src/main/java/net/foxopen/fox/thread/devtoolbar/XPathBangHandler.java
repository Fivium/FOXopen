package net.foxopen.fox.thread.devtoolbar;

import net.foxopen.fox.ComponentText;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.BangHandlerServlet;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;

import java.util.Collections;
import java.util.Set;

public class XPathBangHandler
extends DevToolbarBangHandler {

  private static final XPathBangHandler INSTANCE = new XPathBangHandler();
  public static XPathBangHandler instance() {
    return INSTANCE;
  }

  private XPathBangHandler() { }

  @Override
  protected FoxResponse getResponseInternal(RequestContext pRequestContext, StatefulXThread pXThread) {

    FoxRequest pFoxRequest = pRequestContext.getFoxRequest();

    // Can't get an XThreads DOMs without a reference to the thread
    String lThreadID = pFoxRequest.getParameter("thread_id");

    String lXPath = pFoxRequest.getHttpRequest().getParameter("xpath");
    if (XFUtil.isNull(lXPath)) { // If no xpath given yet, show a form
      try {
        ComponentText lXPathPage = (ComponentText) ComponentManager.getComponent("html/xpath-runner");
        String lXP = lXPathPage.getText().toString();
        lXP = lXP.replaceAll("%JQUERY%", pRequestContext.createURIBuilder().buildStaticResourceURI("js/core-header.js"));
        lXP = lXP.replaceAll("%THREAD_ID%", lThreadID);
        return new FoxResponseCHAR("text/html", new StringBuffer(lXP), 0);
      }
      catch (Throwable th) {
        return BangHandlerServlet.basicHtmlResponse("Failed to load xpath page!");
      }
    }
    else { // Else run the xpath and display results accordingly
      String lXPathResult = pXThread.getDevToolbarContext().getXPathResult(pRequestContext);
      return new FoxResponseCHAR("text/xml", new StringBuffer(lXPathResult), 0);
    }

  }

  @Override
  protected Set<String> getAdditionalParamList() {
    return Collections.emptySet();
  }

  @Override
  public String getAlias() {
    return "XPATH";
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.INTERNAL_SUPPORT;
  }
}
