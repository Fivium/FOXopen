package net.foxopen.fox.thread.stack;


import com.google.common.collect.Iterators;
import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.ActionDefinition;
import net.foxopen.fox.module.AutoActionType;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.State;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.DOMHandlerProvider;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.ThreadEventListener;
import net.foxopen.fox.thread.ThreadEventType;
import net.foxopen.fox.thread.persistence.ListeningPersistable;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;
import net.foxopen.fox.thread.persistence.data.ModuleCallPersistedData;
import net.foxopen.fox.thread.persistence.xstream.XStreamManager;
import net.foxopen.fox.thread.stack.ModuleStateChangeListener.EventType;
import net.foxopen.fox.thread.stack.callback.CallbackHandler;
import net.foxopen.fox.thread.stack.transform.StateStackTransformation;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackProperty;
import net.foxopen.fox.track.TrackTimer;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Co-ordinates module calls.
 */
public class ModuleCallStack
implements ListeningPersistable, Iterable<ModuleCall>, ThreadEventListener {

  private final Deque<ModuleCall> mStack;
  private final Set<ModuleStateChangeListener> mStateChangeListeners = new HashSet<>();

  private Deque<ModuleCall> mStackAtPersistenceCycleStart;

  /** Tracks whether a newly-created ModuleCallStack has been mounted at least once. */
  private boolean mHasBeenMounted = false;

  public void deserialise(RequestContext pRequestContext, PersistenceContext pPersistenceContext, String pThreadId, DOMHandlerProvider pDOMHandlerProvider) {

    //Validate
    if(mStack.size() != 0) {
      throw new IllegalStateException("Cannot deserialise into a stack which is not empty");
    }

    List<ModuleCallPersistedData> lModCallDataList = pPersistenceContext.getDeserialiser().getModuleCallPersistedData(pThreadId);

    for(ModuleCallPersistedData lModCallData : lModCallDataList) {
      ModuleCall lDeserialisedCall = ModuleCall.deserialise(pPersistenceContext, lModCallData, this, pDOMHandlerProvider);
      mStack.addFirst(lDeserialisedCall);
    }

    //We have added new module calls so the state of this object has changed - notify listeners
    notifyStateChangeListeners(pRequestContext, EventType.MODULE);
  }

  public ModuleCallStack() {
    mStack = new ArrayDeque<>();
  }

  public void rampUp(ActionRequestContext pRequestContext){
    Track.pushInfo("ModuleCallStackRampUp", "Mounting callstack", TrackTimer.THREAD_RAMP_UP);
    try {
      //Retain the top UCon on the stack while for opening DOM handlers as several will need a connection
      pRequestContext.getContextUCon().startRetainingUCon();
      if(!getTopModuleCall().isMounted()){
        getTopModuleCall().mount(pRequestContext);
      }

      mHasBeenMounted = true;
    }
    finally {
      Track.pop("ModuleCallStackRampUp", TrackTimer.THREAD_RAMP_UP);
      pRequestContext.getContextUCon().stopRetainingUCon();
    }

    //Keep these lines out of THREAD_RAMP_UP timer block - they may caused MODULE_LOAD timer to increment, so timers will overlap
    Track.setProperty(TrackProperty.MODULE_START_NAME, getTopModuleCall().getModule().getName());
    Track.setProperty(TrackProperty.STATE_START_NAME, getTopModuleCall().getTopState().getName());
  }

  /**
   * Ensures the top module call of this stack has been ramped up (and down). Calling this guarantees that the module's
   * initialisation subroutine will have fired, meaning context labels are valid and all its DOMs are available in memory (but
   * may be read only). This method has no effect if the top module call has already been mounted since this ModuleCallStack
   * was created.
   * @param pRequestContext
   */
  public void validateTopModuleCallHasBeenRamped(ActionRequestContext pRequestContext) {
    if(!mHasBeenMounted) {
      Track.pushInfo("RampUnrampTopModuleCall", "Ensuring top module call DOMs are loaded");
      try {
        rampUp(pRequestContext);
        rampDown(pRequestContext);
      }
      finally {
        Track.pop("RampUnrampTopModuleCall");
      }
    }
  }

  public void doInitialCall(ActionRequestContext pRequestContext, ModuleCall.Builder lModuleCallBuilder){

    if(mStack.size() != 0){
      throw new IllegalStateException("ModuleCallStack must be empty to perform initial call; already contains " + mStack.size() + " calls.");
    }

    //Record the initial call stack state
    Track.setProperty(TrackProperty.MODULE_START_NAME, lModuleCallBuilder.getEntryTheme().getModule().getName());
    Track.setProperty(TrackProperty.STATE_START_NAME, lModuleCallBuilder.getEntryTheme().getStateName());

    push(pRequestContext, lModuleCallBuilder);

    mHasBeenMounted = true;
  }

  //TODO access
  public void push(ActionRequestContext pRequestContext, ModuleCall.Builder pModuleCallBuilder){

    //Register this now so the new call is the first item to be persisted
    pRequestContext.getPersistenceContext().requiresPersisting(this, PersistenceMethod.UPDATE);

    //Build the new module call
    ModuleCall lNewModuleCall = pModuleCallBuilder.build(this, pRequestContext.getDOMHandlerProvider(), pRequestContext.getPersistenceContext());

    //Unmount the call currently on top if it's mounted
    if(mStack.size() > 0){
      getTopModuleCall().unmountIfMounted(pRequestContext);
    }

    //Push the new module call onto the stack
    //This has to be first otherwise the mounted request context is not in the right state
    mStack.addFirst(lNewModuleCall);

    //Run before-entry block and register SL DOM handlers - must be done when the module is actually on the stack so "getTopModule" calls return the correct module
    lNewModuleCall.initialise(pRequestContext);

    //Tell interested objects about the new module call
    notifyStateChangeListeners(pRequestContext, EventType.MODULE);

    //Open DOM handlers
    lNewModuleCall.mount(pRequestContext);

    //Run entry theme
    XDoRunner lEntryThemeRunner = pRequestContext.createCommandRunner(true);
    lNewModuleCall.processEntryTheme(pRequestContext, lEntryThemeRunner);

    //This may run a subsequent CST which would cause this module to become unmounted!
    lEntryThemeRunner.processCompletion(pRequestContext, this);

    //Pushing/entry theme process is complete, save the DOMs
    //Note: module call may have been unmounted by call stack transformations run above
    rampDown(pRequestContext);
  }

  /**
   * Pops the top ModuleCall from the top of this stack. If additional ModuleCalls remain on the stack afterwards, they may
   * be mounted by this method and may additionally have callback actions run against them.
   * @param pRequestContext Current RequestContext.
   * @param pRunCallbacks If true, this will run auto-state-final actions on the current top module and any callback actions
   * on the new top module. Callback action processing includes copying the :{return} DOM to the :{result} DOM etc. The new
   * top ModuleCall will be mounted if this parameter is true regardless of the value of the pMountData parameter.
   * @param pMountData If true, the new top module will be mounted at the end of the pop operation.
   * @param pAllowCallbackTransformations If true, further callstack transformations caused by running callback actions will
   * be allowed. If false, an error will be thrown if callback actions attempt to perform callstack transformations.
   * @param pAllowStateFinalInterrupts If true, action ignore/breaks and callstack transformations thrown in an auto-state-final
   * will interrupt the pop process and take precedence.
   */
  public void pop(ActionRequestContext pRequestContext, boolean pRunCallbacks, boolean pMountData, boolean pAllowCallbackTransformations, boolean pAllowStateFinalInterrupts){

    ModuleCall lRemovedModuleCall = getTopModuleCall();

    //Mark the callstack as requiring an update - this will process module calls/deletes
    pRequestContext.getPersistenceContext().requiresPersisting(this, PersistenceMethod.UPDATE);
    //Mark state call stack as requiring delete - in case of insert followed by immediate delete (i.e. push/pop in same churn)
    pRequestContext.getPersistenceContext().requiresPersisting(lRemovedModuleCall.getStateCallStack(), PersistenceMethod.DELETE);

    //Run any auto state final actions - legacy behaviour was only to run these when running callbacks
    if(pRunCallbacks) {
      //Ensure the call we're about to run actions on is mounted
      if(!lRemovedModuleCall.isMounted()){
        lRemovedModuleCall.mount(pRequestContext);
      }

      //Note that breaks/ignores in state finals do not interrupt the pop UNLESS they came from a state-pop (flag on CST)
      XDoRunner lStateFinalRunner;
      if(pAllowStateFinalInterrupts){
        lStateFinalRunner = pRequestContext.createCommandRunner(false);
        lStateFinalRunner.treatIgnoresAsBreaks();
      }
      else {
        lStateFinalRunner = pRequestContext.createIsolatedCommandRunner(true);
      }

      //Run any auto state finals
      lRemovedModuleCall.runFinalActions(pRequestContext, lStateFinalRunner);

      //Record now if the auto state final caused an interrupt (if we're allowing them)
      boolean lWasInterrupted = pAllowStateFinalInterrupts && !lStateFinalRunner.executionAllowed();

      //Do the standard post-action processing (including running any further CSTs if they're allowed)
      lStateFinalRunner.processCompletion(pRequestContext, this);

      //If the auto state final threw an interrupt, cancel the pop operation
      if(lWasInterrupted){
        return;
      }
    }

    //Pop from the top of the stack
    mStack.removeFirst();
    notifyStateChangeListeners(pRequestContext, EventType.MODULE);

    //Any errors which occur from here must manually abort the popped module call, because Thread abort only aborts the top call
    try {
      //If the stack has calls remaining on it, we may need to run callbacks
      if(mStack.size() > 0) {

        ModuleCall lNewTopModuleCall = getTopModuleCall();

        if(pMountData || pRunCallbacks) {
          //Mount DOMs and refresh the ContextUElem for the newly entered module
          lNewTopModuleCall.mount(pRequestContext);
        }

        if(pRunCallbacks){
          runCallbacks(pRequestContext, lRemovedModuleCall, lNewTopModuleCall, pAllowCallbackTransformations);
        }
      }

      //Now we've potentially read the return DOM/callback handlers etc we can unmount the popped call
      if(lRemovedModuleCall.isMounted()){
        lRemovedModuleCall.unmountModuleCall(pRequestContext);    //Ok here or needs moving up?
      }
    }
    catch (Throwable th) {
      //Catch all to ensure DOM handlers on the just-popped call are aborted in the event of an error
      lRemovedModuleCall.getContextUElem().abortDOMHandlers();
      throw new ExInternal("Error caught handling callback", th);
    }
  }

  private void runCallbacks(ActionRequestContext pRequestContext, ModuleCall pRemovedModuleCall, ModuleCall pNewTopModuleCall, boolean pAllowCallbackTransformations) {
    //Establish how to run the callbacks
    XDoRunner lCallbackRunner;
    if(pAllowCallbackTransformations) {
      lCallbackRunner = pRequestContext.createCommandRunner(true);
    }
    else {
      //If this pop is part of a pop-then-push style transformation, do not allow CSTs during the pop as they will interfere
      //with the 'primary' transformation. Legacy behaviour was to silently ignore these but now an error is raised.
      lCallbackRunner = pRequestContext.createIsolatedCommandRunner(true);
    }

    //Copy the return DOM to the result DOM
    //Legacy: this was only done if callbacks were to be run (makes sense)
    DOM lReturnDOM = pRemovedModuleCall.getContextUElem().getUElem(ContextLabel.RETURN);
    DOM lResultDOM = pNewTopModuleCall.getContextUElem().getUElem(ContextLabel.RESULT);
    //Clean out the result DOM before populating it
    lResultDOM.removeAllChildren();
    lReturnDOM.copyContentsTo(lResultDOM);

    State lNewState = pNewTopModuleCall.getTopState();

    // Run auto-callback-init actions
    for (ActionDefinition lAction : lNewState.getAutoActions(AutoActionType.CALLBACK_INIT)) {
      try {
        lCallbackRunner.runCommands(pRequestContext, lAction.checkPreconditionsAndGetCommandList(pRequestContext));
      }
      catch (Throwable th) {
        throw new ExInternal("Error running auto callback " + lAction.getActionName(), th);
      }
    }

    if(lCallbackRunner.executionAllowed()){
      //Run callback handlers (callback actions / return target expressions) if allowed by runner (i.e. no action break/ignore)
      for(CallbackHandler lCallbackHandler : pRemovedModuleCall.getCallbackHandlerList()){
        lCallbackHandler.handleCallback(pRemovedModuleCall, lCallbackRunner, pRequestContext);
      }
    }

    // Run auto-callback-final actions
    for (ActionDefinition lAction : lNewState.getAutoActions(AutoActionType.CALLBACK_FINAL)) {
      try {
        lCallbackRunner.runCommands(pRequestContext, lAction.checkPreconditionsAndGetCommandList(pRequestContext));
      }
      catch (Throwable th) {
        throw new ExInternal("Error running auto callback " + lAction.getActionName(), th);
      }
    }

    lCallbackRunner.processCompletion(pRequestContext, this);
  }

  public ModuleCall getTopModuleCall(){
    return mStack.getFirst();
  }

  public Mod getPreviousModuleOrNull(){

    Mod lPreviousMod = null;

    //Pop the top call off the stack, read the value from the call underneath and replace the top call
    if(mStack.size() > 1){
      ModuleCall lTopCall = mStack.pop();
      lPreviousMod = mStack.peek().getModule();
      mStack.push(lTopCall);
    }

    return lPreviousMod;
  }


  public int getStackSize(){
    return mStack.size();
  }

  public boolean isEmpty(){
    return mStack.size() == 0;
  }

  /**
   * Top to bottom.
   * @return
   */
  public Iterator<ModuleCall> iterator(){
    return Iterators.unmodifiableIterator(mStack.iterator());
  }

  /**
   * Get the index of the given module call within this stack. 0 represents the bottom of the stack.
   * @param pModuleCall
   * @return ModuleCall's stack index.
   */
  public int moduleCallIndex(ModuleCall pModuleCall){

    int lIndex = 0;
    Iterator<ModuleCall> lIterator = mStack.descendingIterator();
    while (lIterator.hasNext()) {
      if(lIterator.next().getCallId().equals(pModuleCall.getCallId())) {
        return lIndex;
      }
      else {
        lIndex++;
      }
    }

    //Not found in the whole stack
    throw new ExInternal("ModuleCall with ID " + pModuleCall.getCallId() + " not found in this ModuleCallStack");
  }

  public XDoControlFlow handleStateStackTransformation(ActionRequestContext pRequestContext, StateStackTransformation pTransformation) {
    //Propagate result from state transformation
    return pTransformation.transform(pRequestContext, getTopModuleCall().getStateCallStack());
  }


  public void registerStateChangeListener(ModuleStateChangeListener pStateChangeListener){
    mStateChangeListeners.add(pStateChangeListener);
  }

  public void removeStateChangeListener(ModuleStateChangeListener pStateChangeListener){
    mStateChangeListeners.remove(pStateChangeListener);
  }

  void notifyStateChangeListeners(RequestContext pRequestContext, EventType pEventType){
    Track.debug("ModuleCallStack", "Notifying state change listeners of " + pEventType + " event");
    for(ModuleStateChangeListener lListener : mStateChangeListeners) {
      lListener.handleStateChange(pRequestContext, pEventType, this);
    }
  }

  public void rampDown(ActionRequestContext pRequestContext){

    Track.pushInfo("ModuleCallStackRampDown", "Unmounting callstack", TrackTimer.THREAD_RAMP_DOWN);
    try {
      if(getStackSize() > 0){
        getTopModuleCall().unmountIfMounted(pRequestContext);

        Track.setProperty(TrackProperty.MODULE_END_NAME, getTopModuleCall().getModule().getName());
        Track.setProperty(TrackProperty.STATE_END_NAME, getTopModuleCall().getTopState().getName());
      }
    }
    finally {
      Track.pop("ModuleCallStackRampDown", TrackTimer.THREAD_RAMP_DOWN);
    }
  }

  public String debugOutput(){

    String lDebugInfo = "";
    int i = 0;

    for(ModuleCall lCall : mStack){

      lDebugInfo += "<h2>Call " + (++i) + " - " + lCall.getCallId() + " </h2><ul>";

      lDebugInfo += "<li>App: " + lCall.getApp().getMnemonicName() + "</li>";
      lDebugInfo += "<li>Module: " + lCall.getModule().getName() + "</li>";
      lDebugInfo += "<li>State: " + lCall.getTopState().getName() + "</li>";
      lDebugInfo += "<li>Theme: " + lCall.getEntryTheme().getName() + "</li>";
      lDebugInfo += "<li>Local cached mapsets: " + lCall.getMapSetManager().debugOutput() + "</li>";
      lDebugInfo += "<li>Facets: <ul>" + lCall.debugFacetProviders() + "</ul></li>";
      lDebugInfo += "<li>Security scope: <pre>" + XStreamManager.serialiseObjectToXMLString(lCall.getSecurityScope()).replaceAll("<", "&lt;").replaceAll(">", "&gt;") + "</pre></li>";
      lDebugInfo += "<li>XPath variables: <pre>" + XStreamManager.serialiseObjectToXMLString(lCall.getXPathVariableManager()).replaceAll("<", "&lt;").replaceAll(">", "&gt;") + "</pre></li>";
      lDebugInfo += "<li>State calls:<br/> " + lCall.getStateCallStack().debugOutput() + "</li>";

      lDebugInfo += "</ul>";
    }

    return lDebugInfo;
  }

  @Override
  public void startPersistenceCycle() {
    mStackAtPersistenceCycleStart = new ArrayDeque<>(mStack);
  }

  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    throw new UnsupportedOperationException();
    //return Collections.emptySet();
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {

    Collection<PersistenceResult> lDone = new HashSet<>();

    //Deletions
    for(ModuleCall lModuleCall : mStackAtPersistenceCycleStart) {
      if(!mStack.contains(lModuleCall)) {
        Collection<PersistenceResult> lImplicatedPersistables = lModuleCall.delete(pPersistenceContext);
        lDone.addAll(lImplicatedPersistables);
      }
    }

    //Additions
    for(ModuleCall lModuleCall : mStack) {
      if(!mStackAtPersistenceCycleStart.contains(lModuleCall)) {
        Collection<PersistenceResult> lImplicatedPersistables = lModuleCall.create(pPersistenceContext);
        lDone.addAll(lImplicatedPersistables);
      }
    }

    return lDone;
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.MODULE_CALL_STACK;
  }

  @Override
  public void handleThreadEvent(ActionRequestContext pRequestContext, ThreadEventType pEventType) {
    if(getStackSize() > 0 && pEventType == ThreadEventType.FINISH_REQUEST_PROCESSING) {
      getTopModuleCall().getMapSetManager().handleRequestCompletion();
    }
  }
}
