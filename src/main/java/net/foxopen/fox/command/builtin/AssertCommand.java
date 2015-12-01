package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowBreak;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExAssertion;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.assertion.AssertionResult;

import java.util.Collection;
import java.util.Collections;


public class AssertCommand
extends BuiltInCommand {

  protected final String mTestXPath;
  protected final String mMessage;

  private AssertCommand(DOM commandElement) {
    super(commandElement);
    mTestXPath = commandElement.getAttr("test");
    mMessage = commandElement.getAttr("message");
  }

  protected AssertCommand(DOM commandElement, String pTestXPath) {
    super(commandElement);
    mTestXPath = pTestXPath;
    mMessage = commandElement.getAttr("message");
  }

  /**
   * Shared handling of an AssertionResult for use between the different assertion commands. If the request is in assertion
   * mode, assertion failures are handled as breaks so the action is exited gracefully. In normal mode, an assertion failure
   * immediately throws an exception. If the assertion passed, the action is allowed to continue.
   * @param pRequestContext Current RequestContext.
   * @param pAssertionResult Result to handle.
   * @return Correct XDoControlFlow based on the current assertion mode and assertion result.
   */
  static XDoControlFlow handleAssertionResult(ActionRequestContext pRequestContext, AssertionResult pAssertionResult) {

    pRequestContext.addXDoResult(pAssertionResult);

    if (pRequestContext.isAssertionMode()) {
      if (!pAssertionResult.assertionPassed()) {
        return new XDoControlFlowBreak("AssertionFailed");
      }
    }
    else if (!pAssertionResult.assertionPassed()) {
      throw ExAssertion.createFromAssertionResult(pAssertionResult);
    }

    return XDoControlFlowContinue.instance();
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    boolean lPassed;
    try {
      lPassed = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mTestXPath);
    }
    catch(ExActionFailed e) {
      throw new ExInternal("Failed to run XPath for assert command", e);
    }

    AssertionResult lAssertionResult = new AssertionResult(mTestXPath, mMessage, lPassed);

    //Behaviour after an assertion failure depends on the request's assertion mode
    return handleAssertionResult(pRequestContext, lAssertionResult);
  }

  @Override
  public void validate(Mod pModule) {
    if (!XFUtil.exists(mTestXPath)) {
      throw new ExInternal("Markup error: assert command missing test attribute");
    }
  }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new AssertCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("assert");
    }
  }
}
