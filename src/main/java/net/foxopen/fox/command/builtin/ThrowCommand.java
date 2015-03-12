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


import java.util.Arrays;
import java.util.Collection;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowBreak;
import net.foxopen.fox.command.flow.XDoControlFlowError;
import net.foxopen.fox.command.flow.XDoControlFlowIgnore;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

public class ThrowCommand
extends BuiltInCommand {

  private static final String THROW_BREAK_COMMAND_NAME = "throw-break";
  private static final String THROW_IGNORE_COMMAND_NAME = "throw-ignore";

  /** The code of the throw command. */
  private String mCodeOrXPath;

  /** The message top be thrown. */
  private String mMessageOrXPath;

  /**
   * Contructs a throw command from the XML element specified.
   * @param pMod the fox module where the command resides.
   * @param pCommandElement the element from which the command will be constructed.
   * @throws ExInternal
   */
  private ThrowCommand(DOM pCommandElement, String pCode) throws ExInternal {
    super(pCommandElement);
    mCodeOrXPath = XFUtil.nvl(pCode, "NONE");
    mMessageOrXPath = getAttribute("message", "NO-MESSAGE");
  }

  public boolean isCallTransition() {
    return true;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    final String lCode;
    final String lMessage;
    try {
      lCode = pRequestContext.getContextUElem().extendedStringOrXPathString(pRequestContext.getContextUElem().attachDOM(), mCodeOrXPath);
      lMessage = pRequestContext.getContextUElem().extendedStringOrXPathString(pRequestContext.getContextUElem().attachDOM(), mMessageOrXPath);
    }
    // Rethrow if XPath evaluation fails
    catch (ExActionFailed ex) {
      throw new ExInternal("fm:throw was invoked with an invalid XPath for one of the parameters", ex);
    }

    if(XDoControlFlowIgnore.IGNORE_CODE.equals(lCode)){
      return new XDoControlFlowIgnore(lMessage);
    }
    else if(XDoControlFlowBreak.BREAK_CODE.equals(lCode)){
      return new XDoControlFlowBreak(lMessage);
    }
    else {
      return new XDoControlFlowError(lCode, lMessage, new ExActionFailed(lCode, lMessage));
    }
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {

      String lCode;
      if(THROW_BREAK_COMMAND_NAME.equals(pMarkupDOM.getLocalName())) {
        lCode = XDoControlFlowBreak.BREAK_CODE;
      }
      else if(THROW_IGNORE_COMMAND_NAME.equals(pMarkupDOM.getLocalName())) {
        lCode = XDoControlFlowIgnore.IGNORE_CODE;
      }
      else {
        lCode = pMarkupDOM.getAttr("code");
      }

      return new ThrowCommand(pMarkupDOM, lCode);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Arrays.asList("throw", THROW_BREAK_COMMAND_NAME, THROW_IGNORE_COMMAND_NAME);
    }
  }
}
