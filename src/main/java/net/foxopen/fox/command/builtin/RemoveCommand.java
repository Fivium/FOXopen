package net.foxopen.fox.command.builtin;

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

import java.util.Collection;
import java.util.Collections;

/**
 * Simple command that removes DOM Nodes
 */
public class RemoveCommand
extends BuiltInCommand {

  private String mMatch;

  /**
  * Constructs the command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  private RemoveCommand(DOM commandElement)
  throws ExInternal, ExDoSyntax {
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
  throws ExInternal, ExDoSyntax
  {
    if(!commandElement.hasAttr("match")) {
      throw new ExDoSyntax("fm:remove command with no match clause", commandElement);
    }
    mMatch = commandElement.getAttr("match");
    if(mMatch.equals("")) {
      throw new ExDoSyntax("fm:remove command with match=\"\" clause", commandElement);
    }
  }

  public boolean isCallTransition() {
   return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    DOMList lRemoveTarget;
    try {
      lRemoveTarget = pRequestContext.getContextUElem().extendedXPathUL(mMatch, ContextUElem.ATTACH);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to run XPath for fm:remove match", e);
    }

    lRemoveTarget.removeFromDOMTree();
    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new RemoveCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("remove");
    }
  }
}
