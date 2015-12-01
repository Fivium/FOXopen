package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.assertion.AssertionEqualsResult;

import java.util.Collection;
import java.util.Collections;

public class AssertEqualsCommand
extends AssertCommand {

  private final String mEqualsValue;
  private final String mEqualsExpression;

  private AssertEqualsCommand(DOM pDom) {
    super(pDom, pDom.getAttr("target"));
    mEqualsValue = pDom.getAttrOrNull("equals-value");
    mEqualsExpression = pDom.getAttr("equals-expression");
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    try {
      String lExpectedResult;
      if(mEqualsValue == null) {
        lExpectedResult = lContextUElem.extendedXPathString(lContextUElem.attachDOM(), mEqualsExpression);
      }
      else {
        lExpectedResult = mEqualsValue;
      }

      if(lExpectedResult == null) {
        throw new ExInternal("String to test cannot be null");
      }

      String lActualResult = lContextUElem.extendedXPathString(lContextUElem.attachDOM(), mTestXPath);

      boolean lPassed = lExpectedResult.equals(lActualResult);

      AssertionEqualsResult lAssertionResult = new AssertionEqualsResult(mTestXPath, mMessage, lExpectedResult, lActualResult, lPassed);

      //Behaviour after an assertion failure depends on the request's assertion mode
      return AssertCommand.handleAssertionResult(pRequestContext, lAssertionResult);
    }
    catch(ExActionFailed e) {
      throw new ExInternal("Failed to run XPath for assert command", e);
    }
  }


  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new AssertEqualsCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("assert-equals");
    }
  }
}
