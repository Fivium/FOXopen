package net.foxopen.fox.command.builtin;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.foxopen.fox.ContextLabel;
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


public class IfCommand
extends BuiltInCommand {

  /** The if part XPath expression. */
  private String mIfTestExpr;

  /** The command list for the then clause. */
  private XDoCommandList mThenCommands;

  /** A list of the else-if clauses. */
  private List<String> mElseIfTestExprs;
  private List<XDoCommandList> mElseIfClauses;

  /** The command list for the else clause. */
  private XDoCommandList mElseClause;

  /**
  * Constructs an if command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  private IfCommand(Mod module, DOM commandElement) {
    super(commandElement);

    try {
      // Parse the If part
      mIfTestExpr = commandElement.getAttr("test");
      if (mIfTestExpr == null) {
        throw new ExInternal("Error parsing \"if\" command in module \""+module.getName()+
                             "\" - expected a \"test\" attribute that defines the test!", commandElement);
      }

      DOMList thenElems = commandElement.getULByLocalName("then");
      if (thenElems.getLength() != 1) {
        throw new ExInternal("Error parsing \"if\" command in module \""+module.getName()+
                             "\" - expected one \"then\" clause as the first element of the \"if\" clause!", commandElement);
      }
      mThenCommands = XDoCommandList.parseNestedDoOrChildElements(module, thenElems.item(0));

      // Parse the else-if clauses
      DOMList elseIfElems = commandElement.getULByLocalName("else-if");
      mElseIfClauses = new ArrayList(elseIfElems.getLength());
      mElseIfTestExprs = new ArrayList(elseIfElems.getLength());

      for (int n=0; n < elseIfElems.getLength(); n++) {
        DOM elseIfElem = elseIfElems.item(n);
        mElseIfTestExprs.add(elseIfElem.getAttr("test"));

        XDoCommandList elseIfClause = XDoCommandList.parseNestedDoOrChildElements(module, elseIfElem);
        mElseIfClauses.add(elseIfClause);
      }

      // Parse any else part
      DOMList elseElems = commandElement.getULByLocalName("else");
      if (elseElems.getLength() == 1) {
        mElseClause = XDoCommandList.parseNestedDoOrChildElements(module, elseElems.item(0));
      }
      else if (elseElems.getLength() > 1) {
        throw new ExInternal("Error parsing \"if\" command in module \""+module.getName()+
                             "\" - expected only one \"else\" clause. Found "+elseElems.getLength()+" else clauses!", commandElement);
      }
    }
    catch (Exception ex) {
      throw new ExInternal("Unexpected error caught parsing \""+getName()+"\" command - see nested exception for details.", commandElement, ex);
    }
  }

   /**
   * Validates that the if statement adheres to the if...then...else-if...else
   * structure in that order.
   *
   * @param pModule the module where the component resides
   * @param commandElement the XML element that comprises the command
   * @throws ExInternal if the component syntax is invalid.
   */
  @Override
  public void validate(Mod pModule) {
    mThenCommands.validate(pModule);

    for (XDoCommandList lElseIf : mElseIfClauses) {
      lElseIf.validate(pModule);
    }

    if(mElseClause != null) {
      mElseClause.validate(pModule);
    }
  }

  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    XDoRunner lXDoRunner = pRequestContext.createCommandRunner(false);

    // If...then part check
    boolean lTestResult;
    try {
      lTestResult = lContextUElem.extendedXPathBoolean(lContextUElem.getUElem(ContextLabel.ATTACH), mIfTestExpr);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("If command xpath",e);
    }

    if(lTestResult) {
      return lXDoRunner.runCommands(pRequestContext, mThenCommands);
    }

    // Else-If... part check
    for(int n=0; n < mElseIfClauses.size(); n++) {
      String lElseIfTest = mElseIfTestExprs.get(n);
      try {
        lTestResult = lContextUElem.extendedXPathBoolean(lContextUElem.getUElem(ContextLabel.ATTACH), lElseIfTest);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("ElseIf command xpath",e);
      }
      if (lTestResult) {
        return lXDoRunner.runCommands(pRequestContext, (mElseIfClauses.get(n)));
      }
    }

    // else part check
    if(mElseClause != null) {
      return lXDoRunner.runCommands(pRequestContext, mElseClause);
    }

    //In any other cases allow calling code to continue
    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new IfCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("if");
    }
  }
}
