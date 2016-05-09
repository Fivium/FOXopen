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
import net.foxopen.fox.thread.FieldFocusResult;
import net.foxopen.fox.thread.FocusResult;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

/**
 * Command that sets the focus element or desired scroll position on the
 * parent XThread for use when generating the output HTML page, i.e.
 * will set user focus on an HTML page element.
 */
public class FocusCommand extends BuiltInCommand {

  private String mNodeXPath;
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

    String lYOffset = "0";
    try {
      if (!XFUtil.isNull(mScrollYOffsetStringOrXPath)) {
        lYOffset = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mScrollYOffsetStringOrXPath);
      }
    }
    catch (ExActionFailed ex) {
      // Non-critical
    }

    FocusResult lFocusResult = new FieldFocusResult(selectedNode.getRef(), lYOffset);
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
