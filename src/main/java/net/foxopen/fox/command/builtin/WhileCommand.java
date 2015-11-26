package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
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
 * Implementation of a FOX <code>while</code> command
 * comparable to that of the Java language itself.
 *
 * <p>Iterates over boolean/node-list condition.
 *
 */
public class WhileCommand
extends BuiltInCommand {

  /** The sequence of commands to try. */
  private XDoCommandList doCommand;

  /** The xpath expression over which the iteration will occur. */
  private String xPathExpr;

  private static final int ITERATION_LIMIT = 10000;

   /**
   * Constructs a while command from the XML element specified.
   *
   * @param pModule the fox module where the command resides.
   * @param commandElement the element from which the command will
   *        be constructed.
   */
   private WhileCommand(Mod pModule, DOM commandElement) throws ExDoSyntax {
     super(commandElement);

     DOMList childElements = commandElement.getChildElements();
     if ( childElements.getLength() != 1 ||
          !childElements.item(0).getName().endsWith("do"))
     {
       throw new ExInternal("Error parsing \"while\" command in module \""+pModule.getName()+
                            "\" - expected a \"do\" command as the first and only element!");
     }
     doCommand = new XDoCommandList(pModule, childElements.item(0));

     xPathExpr = getAttribute("xpath");
     if (xPathExpr == null)
     {
       throw new ExInternal("Error parsing \"while\" command in module \""+pModule.getName()+
                            "\" - expected an XPath expression attribute, xpath!");
     }
   }

  public boolean isCallTransition() {
   return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem contextUElem = pRequestContext.getContextUElem();
    XDoRunner lRunner = pRequestContext.createCommandRunner(false);
    XDoControlFlow lResult = XDoControlFlowContinue.instance();

    int i=0;
    try {
      while (contextUElem.extendedXPathBoolean(contextUElem.attachDOM(), xPathExpr) && lResult.canContinue()) {
        if(++i > ITERATION_LIMIT) {
          throw new ExInternal("While command exceeded limit of " + ITERATION_LIMIT + " iterations");
        }

        lResult = lRunner.runCommands(pRequestContext, doCommand);
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Error running test XPath for while command", e);
    }

    return lResult;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new WhileCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("while");
    }
  }
}
