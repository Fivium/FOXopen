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

import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of a FOX <code>while</code> command
 * comparable to that of the Java language itself.
 *
 * <p>Iterates over boolean/node-list condition.
 *
 * @author Gary Watson
 *
 */
public class WhileCommand
extends BuiltInCommand {

  /** The sequence of commands to try. */
  private XDoCommandList doCommand;

  /** The xpath expression over which the iteration will occur. */
  private String xPathExpr;

  private static final int ITERATION_LIMIT = 10000;

   /**
   * Contructs a try command from the XML element specified.
   *
   * @param pModule the fox module where the command resides.
   * @param commandElement the element from which the command will
   *        be constructed.
   */
   private WhileCommand(Mod pModule, DOM commandElement) throws ExDoSyntax {
     super(commandElement);

     DOMList childElements = commandElement.getChildElements();
     if ( childElements.getLength() != 1 ||
          !childElements.item(0).getName().endsWith("do"))
     {
       throw new ExInternal("Error parsing \"while\" command in module \""+pModule.getName()+
                            "\" - expected a \"do\" command as the first and only element!");
     }
     doCommand = new XDoCommandList(pModule, childElements.item(0));

     xPathExpr = getAttribute("xpath");
     if (xPathExpr == null)
     {
       throw new ExInternal("Error parsing \"while\" command in module \""+pModule.getName()+
                            "\" - expected an XPath expression attribute, xpath!");
     }
   }

  public boolean isCallTransition() {
   return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem contextUElem = pRequestContext.getContextUElem();
    XDoRunner lRunner = pRequestContext.createCommandRunner(false);
    XDoControlFlow lResult = XDoControlFlowContinue.instance();

    int i=0;
    try {
      while (contextUElem.extendedXPathBoolean(contextUElem.attachDOM(), xPathExpr) && lResult.canContinue()) {
        if(++i > ITERATION_LIMIT) {
          throw new ExInternal("While command exceeded limit of " + ITERATION_LIMIT + " iterations");
        }

        lResult = lRunner.runCommands(pRequestContext, doCommand);
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Error running test XPath for while command", e);
    }

    return lResult;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new WhileCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("while");
    }
  }
}
