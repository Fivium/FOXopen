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

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowCST;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.transform.CallStackTransformation;

public class ExitModuleCommand
extends BuiltInCommand {

  private final CallStackTransformation.Type mOperationType;
  private final String mUrl;

  /**
  * Contructs an ModuleReturnCommand command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  private ExitModuleCommand(DOM pCommandElement) {
    super(pCommandElement);

    String lTypeAttr = pCommandElement.getAttrOrNull("type");
    if(XFUtil.isNull(lTypeAttr)) {
      mOperationType = CallStackTransformation.Type.EXIT_THIS_PRESERVE_CALLBACKS;
    }
    else {
      mOperationType =  CallStackTransformation.Type.forName(lTypeAttr);
    }

    mUrl = pCommandElement.getAttrOrNull("uri");

    // Validate operation is an exit type
    if (mOperationType != CallStackTransformation.Type.EXIT_ALL_CANCEL_CALLBACKS &&
        mOperationType != CallStackTransformation.Type.EXIT_THIS_CANCEL_CALLBACKS &&
        mOperationType != CallStackTransformation.Type.EXIT_THIS_PRESERVE_CALLBACKS
    ) {
      throw new ExInternal("fm:exit-module: Exit module type not known: "+mOperationType.toString());
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    String lExitFoxUrl = null;
    try {
      lExitFoxUrl = mUrl==null ? null : pRequestContext.getContextUElem().extendedStringOrXPathString(pRequestContext.getContextUElem().attachDOM(), mUrl);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Bad XPath for exit-module URL", e);
    }

    CallStackTransformation lCST = CallStackTransformation.createExitCallStackTransformation(mOperationType, lExitFoxUrl);

    return new XDoControlFlowCST(lCST);
  }

  public boolean isCallTransition() {
   return true;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ExitModuleCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("exit-module");
    }
  }
}
