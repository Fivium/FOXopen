package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.StateCall;
import net.foxopen.fox.thread.stack.StateCallStack;

class StateReplace
extends StateStackTransformation {

  private final String mStateName;
  private final boolean mReplaceAll;

  StateReplace(String pStateName, boolean pReplaceAll, String pAttachXPath) {
    super(pAttachXPath);
    mStateName = pStateName;
    mReplaceAll = pReplaceAll;
  }

  @Override
  public XDoControlFlow transform(ActionRequestContext pRequestContext, StateCallStack pStateCallStack) {

    XDoRunner lActionRunner = pRequestContext.createCommandRunner(true);

    StateCall lTopCall = pStateCallStack.getTopStateCall();
    XDoControlFlow lControlFlowResult = lTopCall.runFinalActions(pRequestContext, lActionRunner);

    //Note how control flow result could stop push

    //If no interrupts/CSTs came from running the final actions, reset the attach point and pop the call
    if(lControlFlowResult.canContinue()){

      ContextUElem lContextUElem = pRequestContext.getContextUElem();

      //Attach to the requested attach point
      DOM lNewAttachPoint = evaluateAttachXPath(lContextUElem);
      if(lNewAttachPoint == null){
        lNewAttachPoint = lContextUElem.attachDOM();
      }

      //Pop top/all depending on parameter
      do {
        pStateCallStack.statePop(pRequestContext);
      }
      while (mReplaceAll && pStateCallStack.getStackSize() > 0);

      //Push new state
      StateCall lPushedCall = pStateCallStack.statePush(pRequestContext, mStateName, lNewAttachPoint);

      //Assign contexts from the previous top call to the newly pushed call (legacy behaviour)
      lPushedCall.assignContextualLabels(lTopCall, lContextUElem);

      //Set the attach point again (otherwise we lose it in the above call)
      lContextUElem.setUElem(ContextLabel.ATTACH, lNewAttachPoint);

      //Run the new state's init actions
      lControlFlowResult = lPushedCall.runInitActions(pRequestContext, lActionRunner);
    }

    return lControlFlowResult;
  }

  @Override
  public String getDescription() {
    return "state-replace to " + mStateName;
  }
}
