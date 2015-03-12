package net.foxopen.fox.thread.stack;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.ActionDefinition;
import net.foxopen.fox.module.AutoActionType;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.persistence.Persistable;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;
import net.foxopen.fox.thread.persistence.data.StateCallPersistedData;
import net.foxopen.fox.thread.persistence.xstream.XStreamManager;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * An entry on the state call stack. StateCalls maintain a list of "contextual labels", i.e. DOM labels which are contextual
 * to the state call. This usually includes the "attach" label as calling a state can result in the attach point changing.
 * The labels for the active state must be saved to the persistence context at the end of every page churn. A state also
 * maintains a scroll position so the user's browser is scrolled to the correct location when accessing the state.
 */
public class StateCall
implements Persistable {

  private final String mCallId;
  private final String mStateName;
  private final StateCallStack mCallStack;

  //Nullable - null = no labels set yet
  private Collection<ContextUElem.SerialisedLabel> mContextualLabels;

  private int mScrollPosition;

  static StateCall createNewStateCall(String pStateName, StateCallStack pStateCallStack) {
    String lNewCallId = XFUtil.unique();
    return new StateCall (lNewCallId, pStateName, 0, null, pStateCallStack);
  }

  static StateCall deserialise(PersistenceContext pPersistenceContext, StateCallPersistedData pStateCallData, StateCallStack pOwningCallStack) {
    return new StateCall (pStateCallData.getCallId(), pStateCallData.getStateName(), pStateCallData.getScrollPosition(), pStateCallData.getContextLabels(), pOwningCallStack);
  }

  private StateCall(String pCallId, String pStateName, int pScrollPosition, Collection<ContextUElem.SerialisedLabel> pContextualLabels, StateCallStack pStateCallStack) {
    mCallId = pCallId;
    mStateName = pStateName;
    mCallStack = pStateCallStack;
    mScrollPosition = pScrollPosition;
    mContextualLabels = pContextualLabels;
  }

  public String getStateName() {
    return mStateName;
  }

  void loadContextualLabels(ContextUElem pContextUElem){

    if(mContextualLabels != null) {
      Track.debug("StateCall", "Reassigning contexts for state " + mStateName);

      if(pContextUElem.isLocalised()) {
        throw new ExInternal("Cannot load context when localised to " + pContextUElem.getLocalisedPurpose());
      }

      //Clear all non-document labels from the ContextUElem - either they will be replaced with updated values, or are no longer valid
      pContextUElem.clearContextualLabels();

      //Load labels for this state into the ContextUElem
      pContextUElem.deserialiseContextualLabels(mContextualLabels);
    }
    else {
      Track.debug("StateCall", "No contexts to assign for state " + mStateName);
    }

  }

  void unmount(ActionRequestContext pRequestContext){
    //Save the latest contextual labels from the ContextUElem
    mContextualLabels = pRequestContext.getContextUElem().getSerialisedContextualLabels();

    //Always serialise the state (assume something has changed)
    pRequestContext.getPersistenceContext().requiresPersisting(this, PersistenceMethod.UPDATE);
  }

  /**
   * Allows contextual labels to be assigned to this StateCall from another StateCall. This should only be invoked
   * immediately after StateCall construction. Once the StateCall is in use its contextual labels should only be modified
   * by module developer actions.
   * @param pFromStateCall StateCall to copy contextual labels from.
   * @param pContextUElem Copied labels will be updated on this ContextUElem.
   */
  public void assignContextualLabels(StateCall pFromStateCall, ContextUElem pContextUElem) {
    if(mContextualLabels == null) {
      if(pFromStateCall.mContextualLabels != null) {
        mContextualLabels = new HashSet<>(pFromStateCall.mContextualLabels);
        loadContextualLabels(pContextUElem);
      }
    }
    else {
      throw new ExInternal("Cannot assign contextual labels when labels are already defined");
    }
  }

  public XDoControlFlow runInitActions(ActionRequestContext pRequestContext, XDoRunner pActionRunner) {
    return runAutoActions(pRequestContext, pActionRunner, AutoActionType.STATE_INIT);
  }

  public XDoControlFlow runFinalActions(ActionRequestContext pRequestContext, XDoRunner pActionRunner) {
    return runAutoActions(pRequestContext, pActionRunner, AutoActionType.STATE_FINAL);
  }

  private XDoControlFlow runAutoActions(ActionRequestContext pRequestContext, XDoRunner pActionRunner, AutoActionType pAutoActionType){

    Collection<ActionDefinition> lAutoActions = mCallStack.getModuleCall().getModule().getState(mStateName).getAutoActions(pAutoActionType);

    //Default to continue in case there are no auto actions
    XDoControlFlow lResultControlFlow = XDoControlFlowContinue.instance();
    for (ActionDefinition lAction : lAutoActions) {
      //Note: this runner will only actions run until a break is hit
      lResultControlFlow = pActionRunner.runCommands(pRequestContext, lAction.getXDoCommandList());
    }

    return lResultControlFlow;
  }

  public int getScrollPosition() {
    return mScrollPosition;
  }

  public void setScrollPosition(int pScrollPosition) {
    mScrollPosition = pScrollPosition;
  }

  String getCallId() {
    return mCallId;
  }

  String getContextualLabelDebugString() {
    return XStreamManager.serialiseObjectToXMLString(mContextualLabels);
  }

  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().createStateCall(mCallId, mCallStack.getModuleCall().getCallId(), mCallStack.stateCallIndex(this), mStateName, mScrollPosition, mContextualLabels);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.CREATE));
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().updateStateCall(mCallId, mScrollPosition, mContextualLabels);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.UPDATE));
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().deleteStateCall(mCallId);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.DELETE));
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.STATE_CALL;
  }
}
