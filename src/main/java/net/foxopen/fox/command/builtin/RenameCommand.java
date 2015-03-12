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
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;


/**
 * Simple command that renames DOM Nodes
 *
 * @author Jason Brown
 */
public class RenameCommand
extends BuiltInCommand {

  private String mMatch;
  private String mRenameTo;
  private DOM mCommandElement;

  /**
  * Contructs the command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  private RenameCommand(DOM commandElement) throws ExDoSyntax {
    super(commandElement);
    parseCommand(commandElement);
  }

  /**
  * Parses the command structure. Relies on the XML Schema to
  * ensure the command adheres to the required format.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  private void parseCommand(DOM commandElement)
  throws ExInternal, ExDoSyntax {
    if(!commandElement.hasAttr("match")) {
      throw new ExDoSyntax("rename command with no match clause", commandElement);
    }
    mMatch = commandElement.getAttr("match");
    if(mMatch.equals("")) {
      throw new ExDoSyntax("rename command with match=\"\" clause", commandElement);
    }
    if(!commandElement.hasAttr("rename-to")) {
      throw new ExDoSyntax("rename command with no rename-to clause", commandElement);
    }
    mRenameTo = commandElement.getAttr("rename-to");
    if(mRenameTo.equals("")) {
      throw new ExDoSyntax("rename command with rename-to=\"\" clause", commandElement);
    }
    mCommandElement = commandElement;
  }

  public boolean isCallTransition() {
   return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    String lEvaluatedString;
    try {
      lEvaluatedString = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mRenameTo);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaulate rename-to attribute of fm:rename commad", e);
    }

    if(lEvaluatedString.equals("")) {
      throw new ExInternal("XPATH error: rename command results in rename-to=\"\"", mCommandElement);
    }

    DOMList lRenameTarget;

    try {
      lRenameTarget = lContextUElem.extendedXPathUL(mMatch, ContextUElem.ATTACH);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaulate match attribute of fm:rename commad", e);
    }

    lRenameTarget.renameAll(lEvaluatedString);

    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new RenameCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("rename");
    }
  }
}
