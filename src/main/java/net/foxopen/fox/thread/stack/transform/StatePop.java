package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowCST;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.StateCall;
import net.foxopen.fox.thread.stack.StateCallStack;

public class StatePop
extends StateStackTransformation {

  private final boolean mStrict;

  StatePop(boolean pStrict, String pAttachXPath) {
    super(pAttachXPath);
    mStrict = pStrict;
  }

  @Override
  public XDoControlFlow transform(ActionRequestContext pRequestContext, StateCallStack pStateCallStack) {

    //Is this the last state in the stack?
    if(pStateCallStack.getStackSize() == 1){
      if (!mStrict){
        //Return an exit module now - this will cause the state to be popped and auto state finals to fire
        //NOTE: in legacy behaviour, action ignore/breaks in auto state finals DID stop the transition if they were caused by a state-pop but not if they
        //were caused by an exit-module. This is preserved by calling setAllowCancellationInAutoStateFinal but may not be desirable as it is inconsistent.
        ExitModule lExitCST = CallStackTransformation.createExitCallStackTransformation(CallStackTransformation.Type.EXIT_THIS_PRESERVE_CALLBACKS, null);
        lExitCST.allowCancellationInAutoStateFinal();

        return new XDoControlFlowCST(lExitCST);
      }
      else {
        //TODO ExDeveloperError?
        throw new ExInternal("state-pop with strict flag would have caused state stack to be empty. Call with strict flag set to false to invoke a module exit.");
      }
    }
    else {
      //Run state final actions
      StateCall lCall = pStateCallStack.getTopStateCall();
      XDoControlFlow lControlFlowResult = lCall.runFinalActions(pRequestContext, pRequestContext.createCommandRunner(false));

      ContextUElem lContextUElem = pRequestContext.getContextUElem();

      //If no interrupts/CSTs came from running the final actions, reset the attach point and pop the call
      if(lControlFlowResult.canContinue()){
        //Evaluate new attach point here in case it depends on the popped state's attach point
        DOM lNewAttachPoint = evaluateAttachXPath(lContextUElem);

        //Pop the call
        pStateCallStack.statePop(pRequestContext);

        //Attach to the requested attach point
        if(lNewAttachPoint != null){
          pStateCallStack.attachTo(lContextUElem, lNewAttachPoint);
        }

      }

      return lControlFlowResult;
    }
  }

  @Override
  public String getDescription() {
    return "state-pop";
  }

}
