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

import java.util.Collection;
import java.util.Collections;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.CommandProvider;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;


public class EvalCommand
extends BuiltInCommand {

  private final String mMatch;
  private final String mExpr;
  private final DOM mEvalDefinitionDOM;

  /**
  * Contructs the command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  public EvalCommand(DOM pCommandElement)
  throws ExDoSyntax {
    super(pCommandElement);
    mMatch = pCommandElement.getAttrOrNull("match");
    mExpr = pCommandElement.getAttrOrNull("expr");
    mEvalDefinitionDOM = pCommandElement.createDocument();

    if(!(XFUtil.exists(mMatch) ^ XFUtil.exists(mExpr))) {
      throw new ExDoSyntax("eval command must have either a match or expr clause", pCommandElement);
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    DOMList lCommandDOMList;
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    try {
      if (mMatch != null) {
        lCommandDOMList = lContextUElem.extendedXPathUL(mMatch, ContextUElem.ATTACH);
      }
      else if (mExpr != null) {
        // e.g. <fm:eval expr="concat('<fm:assign to=',./NAME,' from='/ROOT/TEST/'>')" />
        String lCommandText = lContextUElem.extendedXPathString(lContextUElem.attachDOM(), mExpr);
        lCommandDOMList = DOM.createDocumentFromXMLString("<do>"+lCommandText+"</do>").getUL("*");
      }
      else {
        throw new ExInternal("fm:eval error");
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("XPath error in fm:eval", e);
    }

    try {
      //TODO PN XTHREAD deal with nested do's
      if(lCommandDOMList.size() > 0 && lCommandDOMList.get(0).getLocalName().equals("do")){
        lCommandDOMList = lCommandDOMList.get(0).getChildElements();
      }

      // Convert DOMList to List of Commands
      Command[] lCommandArray = new Command[lCommandDOMList.getLength()];
      Mod lMod = pRequestContext.getCurrentModule();
      DOM lCommandDOM;
      int i=0;
      while((lCommandDOM = lCommandDOMList.popHead()) != null) {
         // Parse command returning tokenised object representation
         lCommandArray[i++] = CommandProvider.getInstance().getCommand(lMod, lCommandDOM);
      }

      // Build and validate XDo
      XDoCommandList lEvaluatedCommands = new XDoCommandList(mEvalDefinitionDOM, lCommandArray);
      lEvaluatedCommands.validate(lMod);

      return pRequestContext.createCommandRunner(false).runCommands(pRequestContext, lEvaluatedCommands);
    }
    catch (ExDoSyntax e) {
      throw new ExInternal("Evaluated command syntax", e);
    }
  }

  public boolean isCallTransition() {
   return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new EvalCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("eval");
    }
  }
}
