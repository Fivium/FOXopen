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

public class MoveCommand extends BuiltInCommand {

  private final String mFromXPath;
  private final String mToXPath;


  public MoveCommand(DOM pDOM)
  throws ExInternal {
    super(pDOM);
    mFromXPath = pDOM.getAttr("from");
    mToXPath = pDOM.getAttr("to");
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    DOMList sourceNodes;
    try {
      sourceNodes = lContextUElem.extendedXPathUL(mFromXPath, ContextUElem.ATTACH);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate 'from' XPath of fm:move command", e);
    }

    DOMList targetNodes;
    try {
      targetNodes = lContextUElem.extendedXPathUL(mToXPath, ContextUElem.ATTACH);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate 'to' XPath of fm:move command", e);
    }

    for (int s=0; s < sourceNodes.getLength(); s++) {
      DOM source = sourceNodes.item(s);
      for (int t=0; t < targetNodes.getLength(); t++) {
        DOM target = targetNodes.item(t);
        source.moveToParent(target);
      }
    }

    pRequestContext.addSysDOMInfo("last-command/movecount", Integer.toString(sourceNodes.getLength()));

    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new MoveCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("move");
    }
  }
}
