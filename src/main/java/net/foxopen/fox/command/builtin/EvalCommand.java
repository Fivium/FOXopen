package net.foxopen.fox.command.builtin;

import java.util.Collection;
import java.util.Collections;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.CommandProvider;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;


public class EvalCommand
extends BuiltInCommand {

  private final String mMatch;
  private final String mExpr;
  private final DOM mEvalDefinitionDOM;

  /**
  * Constructs the command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  public EvalCommand(DOM pCommandElement)
  throws ExDoSyntax {
    super(pCommandElement);
    mMatch = pCommandElement.getAttrOrNull("match");
    mExpr = pCommandElement.getAttrOrNull("expr");
    mEvalDefinitionDOM = pCommandElement.createDocument();

    if(!(XFUtil.exists(mMatch) ^ XFUtil.exists(mExpr))) {
      throw new ExDoSyntax("eval command must have either a match or expr clause", pCommandElement);
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    DOMList lCommandDOMList;
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    try {
      if (mMatch != null) {
        lCommandDOMList = lContextUElem.extendedXPathUL(mMatch, ContextUElem.ATTACH);
      }
      else if (mExpr != null) {
        // e.g. <fm:eval expr="concat('<fm:assign to=',./NAME,' from='/ROOT/TEST/'>')" />
        String lCommandText = lContextUElem.extendedXPathString(lContextUElem.attachDOM(), mExpr);
        lCommandDOMList = DOM.createDocumentFromXMLString("<do>"+lCommandText+"</do>").getUL("*");
      }
      else {
        throw new ExInternal("fm:eval error");
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("XPath error in fm:eval", e);
    }

    try {
      //TODO PN XTHREAD deal with nested do's
      if(lCommandDOMList.size() > 0 && lCommandDOMList.get(0).getLocalName().equals("do")){
        lCommandDOMList = lCommandDOMList.get(0).getChildElements();
      }

      // Convert DOMList to List of Commands
      Command[] lCommandArray = new Command[lCommandDOMList.getLength()];
      Mod lMod = pRequestContext.getCurrentModule();
      DOM lCommandDOM;
      int i=0;
      while((lCommandDOM = lCommandDOMList.popHead()) != null) {
         // Parse command returning tokenised object representation
         lCommandArray[i++] = CommandProvider.getInstance().getCommand(lMod, lCommandDOM);
      }

      // Build and validate XDo
      XDoCommandList lEvaluatedCommands = new XDoCommandList(mEvalDefinitionDOM, lCommandArray);
      lEvaluatedCommands.validate(lMod);

      return pRequestContext.createCommandRunner(false).runCommands(pRequestContext, lEvaluatedCommands);
    }
    catch (ExDoSyntax e) {
      throw new ExInternal("Evaluated command syntax", e);
    }
  }

  public boolean isCallTransition() {
   return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new EvalCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("eval");
    }
  }
}
