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
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.alert.BasicAlertMessage;
import net.foxopen.fox.thread.alert.BufferAlertMessage;
import net.foxopen.fox.thread.alert.RichAlertMessage;
import net.foxopen.fox.thread.alert.RichTextAlertMessage;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Collections;

/**
 * Simple command that sends alerts to the users browser.
 */
public class AlertCommand
extends BuiltInCommand {

  private static final String DEFAULT_CLOSE_PROMPT = "OK";

  private enum DisplayType {
    NATIVE, POPOVER;
  }

  private final DisplayType mDisplayType;
  private final RichAlertMessage.DisplayStyle mDisplayStyle;

  private final String mMessageXPath;

  private final String mBufferXPath;
  private final String mBufferAttachXPath;

  private final String mTitleXPath;
  private final String mClosePromptXPath;
  private final String mCSSClassXPath;

  private AlertCommand(DOM pCommandElement)
  throws ExDoSyntax {
    super(pCommandElement);

    mMessageXPath = pCommandElement.getAttr("message");
    mBufferXPath = pCommandElement.getAttr("buffer");

    if(!(XFUtil.isNull(mMessageXPath) ^ XFUtil.isNull(mBufferXPath))) {
      throw new ExDoSyntax("message and buffer attributes are mutually exclusive and exactly one must be specified");
    }

    mBufferAttachXPath = pCommandElement.getAttr("bufferAttach");

    String lDisplayType = pCommandElement.getAttr("displayType");
    try {
      //TODO: app-level default switch
      mDisplayType = XFUtil.isNull(lDisplayType) ? DisplayType.NATIVE : DisplayType.valueOf(lDisplayType.toUpperCase());
    }
    catch (IllegalArgumentException e) {
      throw new ExDoSyntax("Not a valid displayType for fm:alert: " + lDisplayType, e);
    }

    String lDisplayStyle = pCommandElement.getAttr("displayStyle");
    try {
      mDisplayStyle = XFUtil.isNull(lDisplayStyle) ? RichAlertMessage.DisplayStyle.INFO : RichAlertMessage.DisplayStyle.valueOf(lDisplayStyle.toUpperCase());
    }
    catch (IllegalArgumentException e) {
      throw new ExDoSyntax("Not a valid displayStyle for fm:alert: " + lDisplayStyle, e);
    }

    mTitleXPath = pCommandElement.getAttr("title");
    mClosePromptXPath = pCommandElement.getAttr("closePrompt");
    mCSSClassXPath = pCommandElement.getAttr("class");
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    if (XFUtil.exists(mBufferXPath)) {
      displayBufferAlert(pRequestContext);
    }
    else {
      if (mDisplayType == DisplayType.NATIVE) {
        displayBasicAlert(pRequestContext);
      }
      else {
        displayRichTextAlert(pRequestContext);
      }
    }

    return XDoControlFlowContinue.instance();
  }

  private void displayBasicAlert(ActionRequestContext pRequestContext) {

    String lMessage = getMessageXPathResult(pRequestContext.getContextUElem()).asString();

    if (!XFUtil.isNull(lMessage)) {
      pRequestContext.addXDoResult(new BasicAlertMessage(lMessage));
    }
    else {
      Track.info("Skipping alert registration as string result was empty");
    }
  }

  private void displayRichTextAlert(ActionRequestContext pRequestContext) {
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    XPathResult lMessage = getMessageXPathResult(lContextUElem);

    if (!XFUtil.isNull(lMessage.asString())) {
      RichTextAlertMessage lAlert = new RichTextAlertMessage(lMessage.asString(), getTitle(lContextUElem), mDisplayStyle,
                                                             getClosePrompt(lContextUElem), getCSSClass(lContextUElem), lMessage.isEscapingRequired());
      pRequestContext.addXDoResult(lAlert);
    }
    else {
      Track.info("Skipping alert registration as string result was empty");
    }
  }

  private void displayBufferAlert(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    String lBufferName;
    try {
      lBufferName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mBufferXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Bad buffer XPath in alert command", e);
    }

    if(XFUtil.isNull(lBufferName)) {
      throw new ExInternal("Alert buffer name cannot be null");
    }

    DOM lAttachDOM = lContextUElem.attachDOM();
    if(XFUtil.exists(mBufferAttachXPath)) {
      try {
        lAttachDOM = lContextUElem.extendedXPath1E(lContextUElem.attachDOM(), mBufferAttachXPath);
      }
      catch (ExTooMany | ExTooFew | ExActionFailed e) {
        throw new ExInternal("Failed to evaluate bufferAttach XPath in alert command", e);
      }
    }

    BufferAlertMessage lMessage = new BufferAlertMessage(lBufferName, lAttachDOM.getFoxId(), getTitle(lContextUElem),
                                                         mDisplayStyle, getClosePrompt(lContextUElem), getCSSClass(lContextUElem));
    pRequestContext.addXDoResult(lMessage);

  }

  private String getTitle(ContextUElem pContextUElem) {
    try {
      return pContextUElem.extendedStringOrXPathString(pContextUElem.attachDOM(), mTitleXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluated alert title XPath", e);
    }
  }

  private String getClosePrompt(ContextUElem pContextUElem) {
    try {
      return XFUtil.nvl(pContextUElem.extendedStringOrXPathString(pContextUElem.attachDOM(), mClosePromptXPath), DEFAULT_CLOSE_PROMPT);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluated alert closePrompt XPath", e);
    }
  }

  private String getCSSClass(ContextUElem pContextUElem) {
    try {
      return pContextUElem.extendedStringOrXPathString(pContextUElem.attachDOM(), mCSSClassXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluated alert class XPath", e);
    }
  }

  private XPathResult getMessageXPathResult(ContextUElem pContextUElem) {
    XPathResult lMessage;
    try {
      lMessage = pContextUElem.extendedConstantOrXPathResult(pContextUElem.attachDOM(), mMessageXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Bad XPath in alert command '" + mMessageXPath + "'", e);
    }
    return lMessage;
  }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new AlertCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("alert");
    }
  }
}
