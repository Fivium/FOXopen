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
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.NotificationDisplayType;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.alert.FlashMessage;

import java.util.Collection;
import java.util.Collections;

/**
 * Simple command that flashes an alert to the user.
 */
public class FlashCommand
extends BuiltInCommand {

  private final String mMessageXPath;
  private final NotificationDisplayType mDisplayType;

  private final String mCSSClassXPath;

  private FlashCommand(DOM pCommandElement)
  throws ExDoSyntax {
    super(pCommandElement);

    mMessageXPath = pCommandElement.getAttr("message");

    if(XFUtil.isNull(mMessageXPath)) {
      throw new ExDoSyntax("message attribute cannot be null");
    }

    String lType = pCommandElement.getAttr("type");
    if(XFUtil.isNull(lType)) {
      mDisplayType = NotificationDisplayType.INFO;
    }
    else {
      mDisplayType = NotificationDisplayType.fromExternalString(lType);
    }

    mCSSClassXPath = pCommandElement.getAttr("class");
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    XPathResult lMessage;
    try {
      lMessage = lContextUElem.extendedConstantOrXPathResult(lContextUElem.attachDOM(), mMessageXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate flash message XPath", e);
    }

    String lCSSClass;
    try {
      lCSSClass = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mCSSClassXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate flash class XPath", e);
    }

    pRequestContext.addXDoResult(new FlashMessage(lMessage.asString(), lMessage.isEscapingRequired(), mDisplayType, lCSSClass));

    return XDoControlFlowContinue.instance();
  }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new FlashCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("flash");
    }
  }
}
