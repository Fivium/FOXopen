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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;


public class IfCommand
extends BuiltInCommand {

  /** The if part XPath expression. */
  private String mIfTestExpr;

  /** The command list for the then clause. */
  private XDoCommandList mThenCommands;

  /** A list of the else-if clauses. */
  private List<String> mElseIfTestExprs;
  private List<XDoCommandList> mElseIfClauses;

  /** The command list for the else clause. */
  private XDoCommandList mElseClause;

  /**
  * Contructs an if command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  private IfCommand(Mod module, DOM commandElement) {
    super(commandElement);

    try {
      // Parse the If part
      mIfTestExpr = commandElement.getAttr("test");
      if (mIfTestExpr == null) {
        throw new ExInternal("Error parsing \"if\" command in module \""+module.getName()+
                             "\" - expected a \"test\" attribute that defines the test!", commandElement);
      }

      DOMList thenElems = commandElement.getULByLocalName("then");
      if (thenElems.getLength() != 1) {
        throw new ExInternal("Error parsing \"if\" command in module \""+module.getName()+
                             "\" - expected one \"then\" clause as the first element of the \"if\" clause!", commandElement);
      }
      mThenCommands = XDoCommandList.parseNestedDoOrChildElements(module, thenElems.item(0));

      // Parse the else-if clauses
      DOMList elseIfElems = commandElement.getULByLocalName("else-if");
      mElseIfClauses = new ArrayList(elseIfElems.getLength());
      mElseIfTestExprs = new ArrayList(elseIfElems.getLength());

      for (int n=0; n < elseIfElems.getLength(); n++) {
        DOM elseIfElem = elseIfElems.item(n);
        mElseIfTestExprs.add(elseIfElem.getAttr("test"));

        XDoCommandList elseIfClause = XDoCommandList.parseNestedDoOrChildElements(module, elseIfElem);
        mElseIfClauses.add(elseIfClause);
      }

      // Parse any else part
      DOMList elseElems = commandElement.getULByLocalName("else");
      if (elseElems.getLength() == 1) {
        mElseClause = XDoCommandList.parseNestedDoOrChildElements(module, elseElems.item(0));
      }
      else if (elseElems.getLength() > 1) {
        throw new ExInternal("Error parsing \"if\" command in module \""+module.getName()+
                             "\" - expected only one \"else\" clause. Found "+elseElems.getLength()+" else clauses!", commandElement);
      }
    }
    catch (Exception ex) {
      throw new ExInternal("Unexpected error caught parsing \""+getName()+"\" command - see nested exception for details.", commandElement, ex);
    }
  }

   /**
   * Validates that the if statement adheres to the if...then...else-if...else
   * structure in that order.
   *
   * @param pModule the module where the component resides
   * @param commandElement the XML element that comprises the command
   * @throws ExInternal if the component syntax is invalid.
   */
  @Override
  public void validate(Mod pModule) {
    mThenCommands.validate(pModule);

    for (XDoCommandList lElseIf : mElseIfClauses) {
      lElseIf.validate(pModule);
    }

    if(mElseClause != null) {
      mElseClause.validate(pModule);
    }
  }

  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    XDoRunner lXDoRunner = pRequestContext.createCommandRunner(false);

    // If...then part check
    boolean lTestResult;
    try {
      lTestResult = lContextUElem.extendedXPathBoolean(lContextUElem.getUElem(ContextLabel.ATTACH), mIfTestExpr);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("If command xpath",e); //PN TODO EXCEPTIONS
    }

    if(lTestResult) {
      return lXDoRunner.runCommands(pRequestContext, mThenCommands);
    }

    // Else-If... part checkAC
    for(int n=0; n < mElseIfClauses.size(); n++) {
      String lElseIfTest = mElseIfTestExprs.get(n);
      try {
        lTestResult = lContextUElem.extendedXPathBoolean(lContextUElem.getUElem(ContextLabel.ATTACH), lElseIfTest);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("ElseIf command xpath",e); //PN TODO EXCEPTIONS
      }
      if (lTestResult) {
        return lXDoRunner.runCommands(pRequestContext, (mElseIfClauses.get(n)));
      }
    }

    // else part check
    if(mElseClause != null) {
      return lXDoRunner.runCommands(pRequestContext, mElseClause);
    }

    //In any other cases allow calling code to continue
    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new IfCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("if");
    }
  }
}
