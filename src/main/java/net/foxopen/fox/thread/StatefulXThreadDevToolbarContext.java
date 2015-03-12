package net.foxopen.fox.thread;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.XQueryUtil;
import net.foxopen.fox.dom.handler.WorkDocDOMHandler;
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.entrypoint.servlets.FoxMainServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.devtoolbar.DebugPage;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;

import java.io.StringWriter;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * DevToolbarContext for a StatefulXThread.
 * Note - a StatefulXThreadDevToolbarContext should never be held permanently - this will cause a memory leak as it contains
 * a strong reference to a StatefulXThread, which may be a stale object.
 */
public class StatefulXThreadDevToolbarContext
implements DevToolbarContext {

  private final StatefulXThread mXThread;
  private final Map<DevToolbarContext.Flag, Boolean> mFlagMap;

  private String mTrackedContextLabel = null;

  static StatefulXThreadDevToolbarContext createDevToolbarContext(RequestContext pRequestContext, StatefulXThread pXThread) {

    StatefulXThreadDevToolbarContext lDevToolbarContext = new StatefulXThreadDevToolbarContext(pXThread);
    if(!XFUtil.isNull(pRequestContext.getFoxRequest().getParameter("selenium"))) {
      lDevToolbarContext.mFlagMap.put(Flag.SEMANTIC_FIELD_IDS, true);
    }

    return lDevToolbarContext;
  }

  private StatefulXThreadDevToolbarContext(StatefulXThread pXThread) {

    mXThread = pXThread;

    //Check the cache for an existing flag map
    FoxCache<String, Map<DevToolbarContext.Flag, Boolean>> lDevToolBarMap = CacheManager.getCache(BuiltInCacheDefinition.DEV_TOOLBAR_FLAGS);
    Map<DevToolbarContext.Flag, Boolean> lCachedMap = lDevToolBarMap.get(pXThread.getThreadId());
    if(lCachedMap != null) {
      mFlagMap = lCachedMap;
    }
    else {
      //Bootstrap a new flag map and default the values
      mFlagMap = new EnumMap<>(DevToolbarContext.Flag.class);
      for(DevToolbarContext.Flag lFlag : DevToolbarContext.Flag.values()) {
        mFlagMap.put(lFlag, lFlag.getDefaultValue());
      }
      //Store the reference to the new map in the cache for subsequent retrieval
      lDevToolBarMap.put(pXThread.getThreadId(), mFlagMap);
    }
  }

  @Override
  public FoxResponse getDebugPage(RequestContext pRequestContext, DebugPage pPageType) {

    Mod lModule = mXThread.getTopModuleCall().getModule();
    boolean lIsXML = true;
    String lResponse = "";

    if (pPageType == DebugPage.MODULE) {
      DOM lModuleDOM = lModule.getModuleRawDOMOrNull();
      if (lModuleDOM != null) {
        lResponse = lModuleDOM.outputNodeToString(true);
      }
      else {
        lIsXML = false;
        lResponse = "Module DOM not cached, only development app mnemonics cache this";
      }
    }
    else if (pPageType == DebugPage.PARSE_TREE) {
      throw new ExInternal("Not implemented");
      //TODO PN HTML
//      //EvaluatedParseTree lEPT = new EvaluatedParseTree(pXThread);
//      EvaluatedParseTree lEPT = null;
//      try {
//        BufferPresentationNode lBuffer = lEPT.getBuffer(null);
//        //ParseTree.printDebug(pWriter, lBuffer);
//        lResponse = lEPT.printDebug(lEPT.getRootBuffer());
//      }
//      catch (ExModule e) {
//      }
    }
    else if (pPageType == DebugPage.MOD_MERGER) {
      DOM lModMergeDOM = lModule.getModuleMergerDOMOrNull();
      if (lModMergeDOM != null) {
        lResponse = lModMergeDOM.outputNodeToString(true);
      }
      else {
        lIsXML = false;
        lResponse = "Merged module DOM not set";
      }
    }
    else if (pPageType ==  DebugPage.EXPANDED) {
      DOM lModMergeExpandedDOM = lModule.getModuleMergerTypeExpandedDOMOrNull();
      if (lModMergeExpandedDOM != null) {
        lResponse = lModMergeExpandedDOM.outputNodeToString(true);
      }
      else {
        lIsXML = false;
        lResponse = "Expanded merged module DOM not cached, only development app mnemonics cache this";
      }
    }
    else if (pPageType == DebugPage.MODEL) {
      lIsXML = false;
      lResponse = lModule.getDiagnsticModelDOMInfo();
    }
    else if(pPageType == DebugPage.FIELD_SET) {
      StringWriter lWriter = new StringWriter();
      mXThread.getFieldSetIn().serialiseToWriter(lWriter);
      lResponse = lWriter.toString();
    }
    else if(pPageType == DebugPage.THREAD) {
      return mXThread.debugPage();
    }
    else {
      throw new ExInternal("Unknown page type " + pPageType.toString());
    }

    return new FoxResponseCHAR(lIsXML ? "text/xml" : "text/plain", new StringBuffer(lResponse), 0);
  }

  @Override
  public DOM getDebugDOM(RequestContext pRequestContext, String pDOMName) {

    ContextUElem lContextUElem = mXThread.getTopModuleCall().getContextUElem();
    //TODO PN better awareness of shared/lazy DOMs for this debug viewer
    if("session".equals(pDOMName) && !lContextUElem.isLabelLoaded(pDOMName)) {
      //Shared DOM may not be loaded in thread (or even exist) but we can still see what's in it
      DOM lSessionDOM = XFUtil.nvl(mXThread.getUserThreadSession().getSessionDOMManager().getDOMCopy(pRequestContext), DOM.createDocument(ContextLabel.SESSION.asString()));
      lSessionDOM.addComment("Note: this DOM was deserialised directly by the debug viewer (it was not loaded in the target thread)");
      return lSessionDOM;
    }
    else if("temp".equals(pDOMName)) {
      return DOM.createDocumentFromXMLString(mXThread.getDOMProvider().getLastTempDOMString());
    }
    else {
      //Force a thread ramp to refresh the workdoc if the thread has not been mounted at least once (i.e. it has been deserialised)
      if (lContextUElem.getDOMHandlerForLabel(pDOMName) instanceof WorkDocDOMHandler && !mXThread.hasBeenMounted()) {
        mXThread.rampAndRun(pRequestContext, new StatefulXThread.XThreadRunnable() {
          public void run(ActionRequestContext pRequestContext)
          throws ExUserRequest {
          }
        }, "DebugWorkDocDOMView");
      }

      return lContextUElem.getUElem(pDOMName);
    }
  }

  @Override
  public String getXPathResult(RequestContext pRequestContext) {

    final FoxRequest lFoxRequest = pRequestContext.getFoxRequest();
    final String lXPath = lFoxRequest.getParameter("xpath");
    final String lQueryType = lFoxRequest.getParameter("query_type");

    final StringBuffer lOut = new StringBuffer();

    StatefulXThread.XThreadRunnable lXPathRunner = new StatefulXThread.XThreadRunnable() {
      @Override
      public void run(ActionRequestContext pRequestContext) {

        boolean lAppendEndTag = true;
        // Run the xpath
        ContextUElem lDOMContexts = pRequestContext.getContextUElem().localise("debug-xpath-runner");
        try {
          // Get and set contexts
          int lContextCount = Integer.valueOf(lFoxRequest.getParameter("context_count"));
          for (int i = 0; i < lContextCount; i++) {
            String lContextLabel = lFoxRequest.getParameter("ctxtLabel"+i);
            String lContextXPath = lFoxRequest.getParameter("ctxtXpath"+i);
            if (!XFUtil.isNull(lContextLabel) && !XFUtil.isNull(lContextXPath)) {
              lDOMContexts.setUElem(lContextLabel, ContextualityLevel.STATE, lDOMContexts.extendedXPath1E(lContextXPath));
            }
          }

          if("xquery".equals(lQueryType)) {
            ContextUElem lContextUElem = pRequestContext.getContextUElem();
            DOM lQueryOutput = DOM.createDocument("xquery-result");
            long lStartTime = System.currentTimeMillis();
            XQueryUtil.runXQuery(lContextUElem.attachDOM().getRootElement(), lContextUElem.attachDOM(), lQueryOutput, lXPath, lContextUElem);
            long lEndTime = System.currentTimeMillis();
            lQueryOutput.setAttr("time", (lEndTime-lStartTime) + "ms");
            lOut.append(lQueryOutput.outputDocumentToString(true));
            lAppendEndTag = false;
          }
          else {
            long lStartTime = System.currentTimeMillis();
            XPathResult lXPR = lDOMContexts.extendedXPathResult(lDOMContexts.attachDOM(), lXPath);
            long lEndTime = System.currentTimeMillis();

            // Set up the root node to keep browsers happy when xpaths return DOM lists
            lOut.append("<xpath-result");
            lOut.append(" xpath=\""+XFUtil.sanitiseStringForOutput(lXPath, XFUtil.SANITISE_HTMLENTITIES)+"\"");
            lOut.append(" time=\"" + (lEndTime-lStartTime) + "ms\"");
            lOut.append(" doms-implicated=\"" + lXPR.getNumberOfImplicatedDocuments() + "\"");
            lOut.append(">\r\n");

            // Add the return data in a datatype appropriate manner
            lXPR.printResultAsXML(lOut);
          }

        }
        catch (Throwable th) {
          lOut.append("<xpath-result xpath=\""+XFUtil.sanitiseStringForOutput(lXPath, XFUtil.SANITISE_HTMLENTITIES)+"\">\r\n");
          lOut.append("<!-- XPath Error -->");
          lOut.append("<!-- ");
          lOut.append(XFUtil.getJavaStackTraceInfo(th));
          lOut.append(" -->");
        }
        finally {
          lDOMContexts.delocalise("debug-xpath-runner");
        }
        if(lAppendEndTag) {
          lOut.append("\r\n</xpath-result>");
        }
      }
    };

    mXThread.rampAndRun(pRequestContext, lXPathRunner, "DebugXPath");

    return lOut.toString();
  }

  @Override
  public Collection<String> getDocumentContextLabels() {
    return mXThread.getTopModuleCall().getContextUElem().getDocumentLabels();
  }

  @Override
  public boolean isFlagOn(DevToolbarContext.Flag pFlag) {
    return mFlagMap.get(pFlag);
  }

  @Override
  public void setFlag(DevToolbarContext.Flag pFlag, boolean pValue) {
    mFlagMap.put(pFlag, pValue);
  }

  @Override
  public Map<String, String> getContextLabelToPathMap() {

    Map<String, String> lResult = new TreeMap<>();
    ContextUElem lContextUElem = mXThread.getTopModuleCall().getContextUElem();

    for(String lLabel : lContextUElem.getContextualLabels()) {
      DOM lDOM = lContextUElem.getUElem(lLabel);
      lResult.put(lLabel, lDOM.absolute() + " (" + lDOM.getFoxId() + ")");
    }

    return lResult;
  }

  @Override
  public void setTrackedContextLabel(String pLabelName) {
    mTrackedContextLabel = pLabelName;
  }

  @Override
  public String getTrackedContextLabelOrNull() {
    return mTrackedContextLabel;
  }

  @Override
  public String getEntryPointURI(RequestURIBuilder pRequestURIBuilder) {
    return FoxMainServlet.buildGetEntryURI(pRequestURIBuilder, mXThread.getThreadAppMnem(), mXThread.getTopModuleCall().getModule().getName(), mXThread.getTopModuleCall().getEntryTheme().getName());
  }
}
