package net.foxopen.fox.thread.stack;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.persistence.ListeningPersistable;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;
import net.foxopen.fox.thread.persistence.data.StateCallPersistedData;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


public class StateCallStack
implements ListeningPersistable {

  private final Deque<StateCall> mStack = new ArrayDeque<>();
  private final ModuleCall mModuleCall;

  private Deque<StateCall> mStackAtStart = new ArrayDeque<>();

  public static StateCallStack deserialise(PersistenceContext pPersistenceContext, ModuleCall pModuleCall) {
    List<StateCallPersistedData> lStateCalls = pPersistenceContext.getDeserialiser().getStateCallPersistedData(pModuleCall.getCallId());

    StateCallStack lNewStack = new StateCallStack(pPersistenceContext, pModuleCall);

    for(StateCallPersistedData lCallData : lStateCalls) {
      StateCall lNewStateCall = StateCall.deserialise(pPersistenceContext, lCallData, lNewStack);
      lNewStack.mStack.addFirst(lNewStateCall);
    }

    return lNewStack;
  }

  StateCallStack(PersistenceContext pPersistenceContext, ModuleCall pModuleCall) {
    mModuleCall = pModuleCall;
    pPersistenceContext.registerListeningPersistable(this);
  }

  /**
   *returns null
   * @return
   */
  public String getTopStateName(){
    StateCall lTopCall = mStack.peekFirst();
    if(lTopCall != null){
      return lTopCall.getStateName();
    }
    else {
      return null;
    }
  }

  public StateCall getTopStateCall(){
    return mStack.getFirst();
  }

  public StateCall statePush(ActionRequestContext pRequestContext, String pStateName, DOM pAttachPoint){

    if(getStackSize() > 0){
      //Unmount the current state call if needed
      getTopStateCall().unmount(pRequestContext);
    }

    StateCall lNewCall = StateCall.createNewStateCall(pStateName, this);

    pRequestContext.getContextUElem().setUElem(ContextLabel.ATTACH, pAttachPoint);

    mStack.addFirst(lNewCall);

    mModuleCall.fireStateChangeEvent(pRequestContext);

    pRequestContext.getPersistenceContext().requiresPersisting(this, PersistenceMethod.UPDATE);

    return lNewCall;
  }

  public StateCall statePop(ActionRequestContext pRequestContext){

    StateCall lCall = mStack.removeFirst();

    //Force labels to be serialised in case we need to read them off the state call later
    lCall.unmount(pRequestContext);

    mModuleCall.fireStateChangeEvent(pRequestContext);

    //Restore labels for new top call
    mModuleCall.loadContextualLabels();

    //The callstack has changed and will need to be serialised (popped state needs deleting)
    pRequestContext.getPersistenceContext().requiresPersisting(this, PersistenceMethod.UPDATE);

    //Return the popped call
    return lCall;
  }

  public int getStackSize(){
    return mStack.size();
  }

  //Exposed for init/final actions
  ModuleCall getModuleCall() {
    return mModuleCall;
  }

  void unmount (ActionRequestContext pRequestContext){
    if(getStackSize() > 0){
      getTopStateCall().unmount(pRequestContext);
    }
  }

  public void attachTo(ContextUElem pContextUElem, DOM pAttachPoint){
    pContextUElem.setUElem(ContextLabel.ATTACH, pAttachPoint);
  }

  public String debugOutput(){

    String lDebugInfo = "<ol>";
    int i = 0;

    for(StateCall lCall : mStack){

      //lDebugInfo += "<h3>Call " + (++i) + "</h2><ul>";
      lDebugInfo += "<li>" + lCall.getStateName() + "<br/>";
      lDebugInfo += "<ul>";
      lDebugInfo +=   "<li>Contexts:<br/><pre> " + lCall.getContextualLabelDebugString().replaceAll("<", "&lt;").replaceAll(">", "&gt;") + "</pre></li>";
      lDebugInfo += "</ul>";
      lDebugInfo += "</li>";
    }

    lDebugInfo += "</ol>";

    return lDebugInfo;
  }

  public int stateCallIndex(StateCall pStateCall){
    int lIndex = 0;
    Iterator<StateCall> lIterator = mStack.descendingIterator();
    while (lIterator.hasNext()) {
      if(lIterator.next().getCallId().equals(pStateCall.getCallId())) {
        return lIndex;
      }
      else {
        lIndex++;
      }
    }

    //Not found in the whole stack
    throw new ExInternal("StateCall with ID " + pStateCall.getCallId() + " not found in this ModuleCallStack");
  }

  public Collection<StateCall> getAllCalls() {
    return Collections.unmodifiableCollection(mStack);
  }

  @Override
  public void startPersistenceCycle() {
    mStackAtStart = new ArrayDeque<>(mStack);
  }

  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {

    Collection<PersistenceResult> lDone = new HashSet<>();

    //Deletions
    for(StateCall lStateCall : mStackAtStart) {
      if(!mStack.contains(lStateCall)) {
        Collection<PersistenceResult> lDeleteResult = lStateCall.delete(pPersistenceContext);
        lDone.addAll(lDeleteResult);
      }
    }

    //Additions
    for(StateCall lStateCall : mStack) {
      if(!mStackAtStart.contains(lStateCall)) {
        Collection<PersistenceResult> lCreateResult = lStateCall.create(pPersistenceContext);
        lDone.addAll(lCreateResult);
      }
    }

    return lDone;
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    //Delete the entire stack in one hit
    pPersistenceContext.getSerialiser().deleteStateCallStack(mModuleCall.getCallId());

    //Work out all deleted items
    Collection<PersistenceResult> lImplicated = new HashSet<>();
    Collection<StateCall> lAllCalls = new HashSet<>(mStack);
    lAllCalls.addAll(mStackAtStart);

    //Mark both individual calls and the stack as deleted (to prevent unneccessary updates on the deleted calls)
    for(StateCall lStateCall : lAllCalls) {
      lImplicated.add(new PersistenceResult(lStateCall, PersistenceMethod.DELETE));
    }

    lImplicated.add(new PersistenceResult(this, PersistenceMethod.DELETE));

    return lImplicated;
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.STATE_CALL_STACK;
  }
}
