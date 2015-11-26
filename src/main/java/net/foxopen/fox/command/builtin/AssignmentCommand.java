package net.foxopen.fox.command.builtin;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Implementation of a FOX command that assigns a value to a
 * target nodelist.
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
     + (commandElement.hasAttr("target") ? 1 : 0 );
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
     else if (commandElement.hasAttr("target")) {
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
                            "expected a \"mConstantValue\" or \"expr\" attribute specifying the value to assign!");
     }
     else if (mConstantValue != null && mXPathValue != null)
     {
       throw new ExInternal("Error parsing \""+getName()+"\" command - "+
                            "expected a \"mConstantValue\" or \"expr\" attribute, but not both!");
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
