package net.foxopen.fox.command.builtin;

import java.util.Collection;
import java.util.Collections;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoResult;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;


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

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    boolean lResult;
    try {
      lResult = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mTestXPath);
    }
    catch(ExActionFailed e) {
      throw new ExInternal("Failed to run XPath for assert command", e);
    }

    pRequestContext.addXDoResult(new AssertionResult(mTestXPath, mMessage, lResult));

    return XDoControlFlowContinue.instance();
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

  //TODO implement trackable
  public static class AssertionResult
  implements XDoResult {
    protected final String mTestXPath;
    protected final String mMessage;
    protected final boolean mPassed;

    public AssertionResult(String pTestXPath, String pMessage, boolean pPassed) {
      mTestXPath = pTestXPath;
      mMessage = pMessage;
      mPassed = pPassed;
    }

    public boolean assertionPassed() {
      return mPassed;
    }

    public String getTestXPath() {
      return mTestXPath;
    }

    public String getFullMessage() {
      return (mPassed ? "PASSED" : "FAILED") + ": " + mMessage + " (test XPath: " + mTestXPath + ")";
    }
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
