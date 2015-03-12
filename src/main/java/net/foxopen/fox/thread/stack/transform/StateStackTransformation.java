package net.foxopen.fox.thread.stack.transform;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.StateCallStack;

public abstract class StateStackTransformation {

  private final String mAttachXPath;

  public static StateStackTransformation createPushTransformation(String pToState, String pAttachXPath){
    return new StatePush(pToState, pAttachXPath);
  }

  public static StateStackTransformation createPopTransformation(boolean pStrict, String pAttachXPath){
    return new StatePop(pStrict, pAttachXPath);
  }

  public static StateStackTransformation createReplaceTransformation(String pToState, boolean pReplaceAll, String pAttachXPath){
    return new StateReplace(pToState, pReplaceAll, pAttachXPath);
  }

  protected StateStackTransformation(String pAttachXPath) {
    mAttachXPath = pAttachXPath;
  }

  protected DOM evaluateAttachXPath(ContextUElem pContextUElem){

    if(XFUtil.isNull(mAttachXPath)){
      return null;
    }

    DOM lAttachPoint;
    try {
      lAttachPoint = pContextUElem.extendedXPath1E(mAttachXPath);
    }
    catch (ExActionFailed | ExTooMany | ExTooFew e) {
      throw new ExInternal("Error getting state attach pont", e);
    }

    return lAttachPoint;
  }

  public abstract String getDescription();

  public abstract XDoControlFlow transform(ActionRequestContext pRequestContext, StateCallStack pStateCallStack);

}
