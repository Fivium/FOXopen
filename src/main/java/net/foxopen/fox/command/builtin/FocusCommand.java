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
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.FocusResult;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

/**
 * Command that sets the focus element or desired scroll position on the
 * parent XThread for use when generating the output HTML page, i.e.
 * will set user focus on an HTML page element.
 *
 * @author Gary Watson
 */
public class FocusCommand extends BuiltInCommand {

  private String mNodeXPath;
  private String mStartPosStringOrXPath;
  private String mEndPosStringOrXPath;
  private String mScrollYOffsetStringOrXPath;

  /**
   * {@inheritDoc}
   */
  private FocusCommand(DOM commandElement)
  throws ExInternal {
    super(commandElement);
    parseCommand(commandElement);
  }

  /**
   * Parses the command structure. Relies on the XML Schema to
   * ensure the command adheres to the required format.
   * @param pMod the owning module
   * @param commandElement the element from which the command will be constructed.
   * @throws ExInternal
   */
  private void parseCommand(DOM commandElement)
  throws ExInternal {
    mNodeXPath = commandElement.getAttr("xpath");
    mStartPosStringOrXPath = commandElement.getAttrOrNull("selectionStart");
    mEndPosStringOrXPath = commandElement.getAttrOrNull("selectionEnd");
    mScrollYOffsetStringOrXPath = commandElement.getAttrOrNull("scrollYOffset");
  }

  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    DOM selectedNode = null;
    try {
      try {
        selectedNode = lContextUElem.extendedXPath1E(mNodeXPath);
      }
      catch (ExCardinality e){
        throw new ExActionFailed("BADPATH", "Cardinality exception when resolving focus target");
      }
    }
    catch (ExActionFailed af) {
      try {
        //TODO what is this trying to achieve???
        DOMList selectedNodes = lContextUElem.extendedXPathUL(lContextUElem.attachDOM(), mNodeXPath);
        if (selectedNodes.getLength() == 1) {
          selectedNode = selectedNodes.item(0);
        }
      }
      catch(ExActionFailed e) {
        // Non-critical
      }
    }

    if(selectedNode == null){
      //throw error? didn't before
      Track.alert("FocusCommand", "Focus command failed to match a target for path " + mNodeXPath);
      return XDoControlFlowContinue.instance();
    }

    String lStartPos  = null, lEndPos  = null, lYOffset = "0";
    try {
      if (!XFUtil.isNull(mStartPosStringOrXPath)) {
        lStartPos = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mStartPosStringOrXPath);
      }
      if (!XFUtil.isNull(mEndPosStringOrXPath)) {
        lEndPos =  lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mEndPosStringOrXPath);
      }
      if (!XFUtil.isNull(mScrollYOffsetStringOrXPath)) {
        lYOffset = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mScrollYOffsetStringOrXPath);
      }
    }
    catch (ExActionFailed ex) {
      // Non-critical
    }

    FocusResult lFocusResult = new FocusResult(selectedNode.getRef(), lStartPos, lEndPos, lYOffset);
    pRequestContext.addXDoResult(lFocusResult);

    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new FocusCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("focus");
    }
  }
}
