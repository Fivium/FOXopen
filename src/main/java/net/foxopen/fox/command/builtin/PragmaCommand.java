/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
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
