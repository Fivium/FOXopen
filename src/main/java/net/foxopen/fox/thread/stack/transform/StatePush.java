package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.StateCall;
import net.foxopen.fox.thread.stack.StateCallStack;

class StatePush
extends StateStackTransformation {

  private final String mStateName;

  StatePush(String pStateName, String pAttachXPath) {
    super(pAttachXPath);
    mStateName = pStateName;
  }

  @Override
  public XDoControlFlow transform(ActionRequestContext pRequestContext, StateCallStack pStateCallStack) {

    DOM lNewAttachPoint = evaluateAttachXPath(pRequestContext.getContextUElem());
    if(lNewAttachPoint == null){
      //Preserve existing attach point if the attach attribute is not defined
      lNewAttachPoint = pRequestContext.getContextUElem().attachDOM();
    }

    //New state call will get old state's contextual labels from the ContextUElem when the call is unmounted. They could
    //also be passed in explicitly here if necessary.

    StateCall lCall = pStateCallStack.statePush(pRequestContext, mStateName, lNewAttachPoint);
    XDoControlFlow lControlFlowResult = lCall.runInitActions(pRequestContext, pRequestContext.createCommandRunner(false));

    return lControlFlowResult;
  }

  @Override
  public String getDescription() {
    return "state-push to " + mStateName;
  }
}
