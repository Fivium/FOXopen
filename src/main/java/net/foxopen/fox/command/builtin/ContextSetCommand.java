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
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Collections;

public class ContextSetCommand
extends BuiltInCommand {

  static final String SCOPE_STATE = "state";
  static final String SCOPE_LOCALISED = "localised";

  private final String mScope;
  private final String mName;
  private final String mXPath;

  /** Constructs the command from the XML element specified */
  private ContextSetCommand(DOM pCmdDOM)
  throws ExDoSyntax {
    super(pCmdDOM);
    mScope = pCmdDOM.getAttr("scope");
    mName = pCmdDOM.getAttr("name");
    mXPath = pCmdDOM.getAttr("xpath");
    if(XFUtil.isNull(mScope) || XFUtil.isNull(mName) || XFUtil.isNull(mXPath)) {
      throw new ExDoSyntax("context-set: missing attrs: scope, name or xpath");
    }
    else if(!mScope.equals(SCOPE_STATE) && !mScope.equals(SCOPE_LOCALISED)) {
      throw new ExDoSyntax("context-set: scope must be 'state' or 'localised'");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    //Cannot set a localised label when ContextUElem is not localised
    if(!lContextUElem.isLocalised() && SCOPE_LOCALISED.equals(mScope)) {
      throw new ExInternal("context-set: scope='localised' (consider state) not permitted when not inside: fm:context-localise; fm:for-each");
    }

    // Get context name - convert when in string(...) format
    String lName;
    try {
      lName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mName);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("context-set: Error getting context name: "+mName, e);
    }
    if(XFUtil.isNull(lName)) {
      throw new ExInternal("context-set: name attr returned null: "+mName);
    }

    if(ContextUElem.ATTACH.equals(lName)) {
      //Special handling for attach point as it requires scroll position to be reset (legacy behaviour)
      pRequestContext.changeAttachPoint(mXPath);
    }
    else {
      //Set a state label
      DOM lDOM;
      try {
        lDOM = lContextUElem.extendedXPath1E(mXPath);
      }
      catch (ExActionFailed | ExCardinality  e) {
        throw new ExInternal("context-set: Error getting context element location: "+ mXPath, e);
      }

      boolean lLocalised = SCOPE_LOCALISED.equals(mScope);

      Track.info("ContextSet", "Setting :{" + lName + "} to node with ID " + lDOM.getFoxId() + " at path " + lDOM.absolute() + (lLocalised ? " (local)" : " (global)"));

      if(lLocalised) {
        lContextUElem.setUElem(lName, ContextualityLevel.LOCALISED, lDOM);
      }
      else {
        lContextUElem.setUElemGlobal(lName, ContextualityLevel.STATE, lDOM);
      }
    }

    return XDoControlFlowContinue.instance();
  }

  public boolean isCallTransition() {
   return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ContextSetCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("context-set");
    }
  }
}
