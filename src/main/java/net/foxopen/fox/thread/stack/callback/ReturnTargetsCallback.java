package net.foxopen.fox.thread.stack.callback;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCall;

public class ReturnTargetsCallback
implements CallbackHandler {

  private final String mReturnTargetsExpression;

  public ReturnTargetsCallback(String pReturnTargetsExpression) {
    mReturnTargetsExpression = pReturnTargetsExpression;
  }

  @Override
  public void handleCallback(ModuleCall pExitedModuleCall, XDoRunner pXDoRunner, ActionRequestContext pRequestContext) {

    DOMList lTargetsDOMList;
    try {
      lTargetsDOMList = pRequestContext.getContextUElem().getCreateXPathUL(mReturnTargetsExpression);

    }
    catch(ExActionFailed x) {
      throw new ExInternal("Bad return target XPATH on exit-module (specified on original module-call): "+mReturnTargetsExpression, x);
    }

    DOM lReturnDOM = pExitedModuleCall.getContextUElem().getUElem(ContextLabel.RETURN);

    for(DOM lTargetDOM : lTargetsDOMList) {
      lReturnDOM.copyContentsTo(lTargetDOM);
    }
  }
}
