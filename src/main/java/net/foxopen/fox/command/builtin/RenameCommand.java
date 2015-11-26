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
 * Simple command that renames DOM Nodes
 */
public class RenameCommand
extends BuiltInCommand {

  private String mMatch;
  private String mRenameTo;
  private DOM mCommandElement;

  /**
  * Constructs the command from the XML element specified.
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
      throw new ExInternal("Failed to evaluate rename-to attribute of fm:rename command", e);
    }

    if(lEvaluatedString.equals("")) {
      throw new ExInternal("XPATH error: rename command results in rename-to=\"\"", mCommandElement);
    }

    DOMList lRenameTarget;

    try {
      lRenameTarget = lContextUElem.extendedXPathUL(mMatch, ContextUElem.ATTACH);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate match attribute of fm:rename command", e);
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
