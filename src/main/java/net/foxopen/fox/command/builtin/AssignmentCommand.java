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
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;


/**
 * Implementation of a FOX command that assigns a value to a
 * target nodelist.
 *
 * @author Gary Watson
 */
public class AssignmentCommand
extends BuiltInCommand {

  private final String mTargetXPATH;
  private final String mConstantValue;
  private final String mXPathValue;
  private final boolean mCreateNodes;

   private AssignmentCommand(DOM commandElement) {
      super(commandElement);

     // Validate parameter usage
     int count =
       (commandElement.hasAttr("setTarget") ? 1 : 0 )
     + (commandElement.hasAttr("initTarget") ? 1 : 0 )
     + (commandElement.hasAttr("target") ? 1 : 0 );  // LEGACY TODO REMOVE LATER
     if(count != 1) {
       throw new ExInternal(getName()+" command: Expected one of: setTarget or initTarget");
     }

     if(commandElement.hasAttr("setTarget")) {
       mTargetXPATH  = getAttribute("setTarget", ".");
       mCreateNodes = false;
     }
     else if (commandElement.hasAttr("initTarget")) {
       mTargetXPATH  = getAttribute("initTarget", ".");
       mCreateNodes = true;
     }
     else if (commandElement.hasAttr("target")) {  // LEGACY REMOVE LATER
       mTargetXPATH  = getAttribute("target", ".");
       mCreateNodes = true;
     }
     else {
       throw new ExInternal(getName() + " command missing setTarget or initTarget attribute");
     }

     mConstantValue = getAttribute("textValue");
     mXPathValue    = getAttribute("expr");

     if (mConstantValue == null && mXPathValue == null)
     {
       throw new ExInternal("Error parsing \""+getName()+"\" command - "+
                            "expected a \"mConstantValue\" or \"expr\" arttribute specifting the value to assign!");
     }
     else if (mConstantValue != null && mXPathValue != null)
     {
       throw new ExInternal("Error parsing \""+getName()+"\" command - "+
                            "expected a \"mConstantValue\" or \"expr\" arttribute, but not both!");
     }
   }

   public boolean isCallTransition() {
     return false;
   }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    try {
      DOM targetNode;

      // Identify target node list
      DOMList targetNodes = lContextUElem.extendedXPathUL(mTargetXPATH, null);

      // When target node list is empty, exit or create one node as required
      if (targetNodes.getLength() == 0) {
        if(!mCreateNodes) {
          return XDoControlFlowContinue.instance();
        }
        try {
          targetNode = lContextUElem.getCreateXPath1E(mTargetXPATH, ContextUElem.ATTACH);
        }
        catch (ExTooMany e) {
          throw new ExActionFailed("BADPATH", "Cardinality exception when determining target node", e);
        }
        targetNodes.add(targetNode);
      }

      // Perform single value assignment to targets
      if(mConstantValue != null || mXPathValue.indexOf(":{assignee}") == -1) {

        String textVal;
        if(mConstantValue != null){
          textVal = mConstantValue;
        }
        else if("".equals(mXPathValue)){
          textVal = ""; //Stops XPath evaluator being called in the case of an empty String
        }
        else {
          textVal = lContextUElem.extendedXPathString(lContextUElem.getUElem(ContextLabel.ATTACH), mXPathValue);
        }

        while((targetNode = targetNodes.popHead()) != null) {
          targetNode.setText(textVal);
        }
      }

      // Perform assignment where each assigned value is computed relative to assignee node
      else {
        lContextUElem.localise("fm:assign");
        try {
          List<String> lValueList = new ArrayList<>(targetNodes.getLength());  // XPATH performance workaround
          for(int n=0; n < targetNodes.getLength(); n++) {
            targetNode = targetNodes.item(n);
            lContextUElem.defineUElem(ContextLabel.ASSIGNEE, targetNode);
            lValueList.add(lContextUElem.extendedXPathString(lContextUElem.attachDOM(), mXPathValue));
          }
          for(int n=0; n < targetNodes.getLength(); n++) {
            targetNodes.item(n).setText(lValueList.get(n));
          }
        }
        finally {
          lContextUElem.delocalise("fm:assign");
        }
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed", e);
    }

    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new AssignmentCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("assign");
    }
  }
}
