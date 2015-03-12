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
package net.foxopen.fox.command;


import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.Validatable;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.ArrayList;

/**
 * A list of commands to be execute with an {@link XDoRunner}.
 */
public class XDoCommandList
extends ArrayList<Command>
implements Validatable {

  private final String mPurpose;
  private boolean mValidated = false;

  /**
   * Parses a piece of markup which may either contain nested commands directly as child elements, or nested within an
   * fm:do element.
   * @param pModule
   * @param pMarkupContainer
   * @return
   * @throws ExDoSyntax
   */
  public static XDoCommandList parseNestedDoOrChildElements(Mod pModule, DOM pMarkupContainer)
  throws ExDoSyntax {
    DOM lMarkupDOM = pMarkupContainer;
    if(lMarkupDOM.get1EByLocalNameOrNull("do") != null){
      lMarkupDOM = lMarkupDOM.get1EByLocalNameOrNull("do");
    }

    return new XDoCommandList(pModule, lMarkupDOM);
  }

  public static XDoCommandList emptyCommandList() {
    return new XDoCommandList();
  }

  public XDoCommandList(Mod pMod, DOM edo)
  throws ExDoSyntax {
    this(pMod, edo, "(Unnamed Command List)");
  }

  public XDoCommandList(Mod pMod, DOM edo, String pPurpose)
  throws ExDoSyntax {
    mPurpose = pPurpose;
    buildCmdList(pMod, edo);
  }

  //For empty list construction
  private XDoCommandList() {
    mPurpose = "Empty XDo";
  }

  public XDoCommandList(DOM pEvalCmd, Command[] pCommandArray)
  throws ExDoSyntax {
    mPurpose = "fm:eval";
    //this(pEvalCmd, "fm:eval", false);
    //Convert passed array into List items
    for(Command lCmd : pCommandArray) {
      add(lCmd);
    }
  }

  private void buildCmdList(Mod pMod, DOM pDefinitionDOM) {
    // Get command list
    for(DOM lCommandDefinition : pDefinitionDOM.getChildElements()) {

      if("do".equals(lCommandDefinition.getLocalName())) {
        //Recurse into nested do blocks (legacy bad syntax)
        Track.alert("BadNestedDoBlock", "Nested do block located in action " + mPurpose + " - should be removed", TrackFlag.BAD_MARKUP);
        buildCmdList(pMod, lCommandDefinition);
      }
      else {
        // Parse command returning tokenised object representation
        Command lParsedCommand = CommandProvider.getInstance().getCommand(pMod, lCommandDefinition);
        // Add command object to execute list
        add(lParsedCommand);
      }
    }
  }

   /**
   * Validates that all commands are valid.
   *
   * @param pModule the module where the component resides
   * @throws ExInternal if the component syntax is invalid.
   */
  public void validate(Mod pModule) {
    // Skip duplicate validate calls
    if(mValidated) {
      return;
    }

    // Validate actions
    for (int i = 0; i < size(); i++) {
      Command lCommand = get(i);
      lCommand.validate(pModule);
      if(i != size()-1 && lCommand.isCallTransition()) {
        throw new ExInternal(
          "A Call Stack Transtion command (one of fm:call-module, fm:exit-module, fm:state-pop, fm:throw) is followed by an unreachable command.\n"
           + "CST Command: "+lCommand.toString()+"\n"
           + "Unreachable Command: " + get(i+1).toString() +"\n"
           + "Path: "+mPurpose+"\n"
        );
      }
    }

    // Flag validate run
    mValidated = true;
  }

  public String getPurpose() {
    return mPurpose;
  }
}
