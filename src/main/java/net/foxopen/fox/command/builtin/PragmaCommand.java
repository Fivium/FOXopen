package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoResult;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.ResponseOverride;

import java.util.Collection;
import java.util.Collections;


public final class PragmaCommand {

  private static class PragmaSetHTMLResponse extends BuiltInCommand {

    private final String mTargetXPath;

    public PragmaSetHTMLResponse(DOM pCommandDOM)
    throws ExDoSyntax {
      super(pCommandDOM);
      mTargetXPath = getAttribute("target");

      if(XFUtil.isNull(mTargetXPath)) {
        throw new ExDoSyntax("pragma set-xml-response must have a target attribute");
      }
    }

    @Override
    public XDoControlFlow run(ActionRequestContext pRequestContext) {
      // Build up a buffer containing the response HTML
      DOM lHtml;
      try {
        lHtml = pRequestContext.getContextUElem().extendedXPath1E(mTargetXPath, false);
      }
      catch (ExActionFailed | ExCardinality e) {
        throw new ExInternal("Error when locating target for HTML response", e);
      }

      StringBuffer lHtmlStringBuffer = new StringBuffer(lHtml.outputNodeToString(false));

      //Set the override response as an XDoResult to be picked up before HTML gen
      FoxResponseCHAR lHTMLResponse = new FoxResponseCHAR("text/html; charset=UTF-8", lHtmlStringBuffer, -1);

      pRequestContext.addXDoResult(new ResponseOverride(lHTMLResponse));

      return XDoControlFlowContinue.instance();
    }

    @Override
    public boolean isCallTransition() {
      return false;
    }
  }

  private static class PragmaSetXMLResponse extends BuiltInCommand {

    private final String mTargetXPath;
    private final String mResponseMimeType;

    public PragmaSetXMLResponse(DOM pCommandDOM)
    throws ExDoSyntax {
      super(pCommandDOM);
      mTargetXPath = getAttribute("target");
      mResponseMimeType = XFUtil.nvl(getAttribute("response-mime-type"), "text/xml");

      if(XFUtil.isNull(mTargetXPath)) {
        throw new ExDoSyntax("pragma set-xml-response must have a target attribute");
      }
    }

    @Override
    public XDoControlFlow run(ActionRequestContext pRequestContext) {

      DOMList lXmlDOMList;
      try {
        lXmlDOMList = pRequestContext.getContextUElem().extendedXPathUL(mTargetXPath, null);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Error when locating target for XML response", e);
      }

      DOM lXmlDOM;
      boolean lRootNodeFound = false;
      for(int i=0; i<lXmlDOMList.getLength(); i++) {
        lXmlDOM=lXmlDOMList.item(i);
        DOM.NodeType lInternedNodeType = lXmlDOM.nodeType();
        if(lInternedNodeType == DOM.NodeType.ELEMENT) {
          if(lRootNodeFound) {
            throw new ExInternal("target match contains more than one top level element");
          } else {
            lRootNodeFound=true;
          }
        }
        //PN: This code previously removed the XML declaration PI, but this has been removed as XOM will not return
        //it as a PI
      }

      DOM lResponseContainer = DOM.createDocument("xml-response-container");
      lXmlDOMList.copyContentsTo(lResponseContainer);

      StringBuffer lXmlStringBuffer = new StringBuffer(lResponseContainer.outputNodeContentsToString(true, true));

      //Set the override response as an XDoResult to be picked up before HTML gen
      FoxResponseCHAR lXMLResponse = new FoxResponseCHAR(mResponseMimeType + "; charset=UTF-8", lXmlStringBuffer, -1);

      pRequestContext.addXDoResult(new ResponseOverride(lXMLResponse));

      return XDoControlFlowContinue.instance();
    }

    @Override
    public boolean isCallTransition() {
      return false;
    }
  }

  private static class PragmaHibernateThread extends BuiltInCommand {

    private PragmaHibernateThread(DOM pCommandDOM) {
      super(pCommandDOM);
    }

    @Override
    public XDoControlFlow run(ActionRequestContext pRequestContext) {

      ContextUElem lContextUElem = pRequestContext.getContextUElem();
      String lResumeAction;
      try {
        lResumeAction = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), getAttribute("resume-action"));
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate resume-action attribute of pragma hibernate", e);
      }

      pRequestContext.addXDoResult(new HibernateThread(lResumeAction));

      return XDoControlFlowContinue.instance();
    }

    @Override
    public boolean isCallTransition() {
      return false;
    }
  }

  public static class HibernateThread implements XDoResult {
    private final String mResumeAction;

    private HibernateThread(String pResumeAction) {
      mResumeAction = pResumeAction;
    }

    public String getResumeAction() {
      return mResumeAction;
    }
  }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {

      String lFunction = pMarkupDOM.getAttr("function");
      if(XFUtil.isNull(lFunction)) {
        throw new ExDoSyntax("function attribute must be specified on pragma command");
      }

      switch(lFunction) {
        case "set-html-response":
          return new PragmaSetHTMLResponse(pMarkupDOM);
        case "set-xml-response":
          return new PragmaSetXMLResponse(pMarkupDOM);
        case "hibernate-thread":
          return new PragmaHibernateThread(pMarkupDOM);
        case "flush-applications":
        case "accessibility-mode":
        case "dump-cached-apps":
        case "set-audited-output-response":
        case "remote-action-call":
        case "save-document-changes":
        case "load-document-template":
          throw new ExDoSyntax("Pragma function " + lFunction + " is no longer supported in FOX5");
        default:
          throw new ExDoSyntax("Pragma function " + lFunction + " is not recognised");
      }
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("pragma");
    }
  }
}
