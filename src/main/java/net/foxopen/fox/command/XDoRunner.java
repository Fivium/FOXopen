package net.foxopen.fox.command;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.command.builtin.AssertCommand;
import net.foxopen.fox.command.builtin.AssertCommand.AssertionResult;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowCST;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.flow.XDoControlFlowError;
import net.foxopen.fox.command.flow.XDoControlFlowIgnore;
import net.foxopen.fox.command.flow.XDoControlFlowThrownCode;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExAssertion;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.ExitResponse;
import net.foxopen.fox.thread.ModulePushExitResponse;
import net.foxopen.fox.thread.ResponseOverride;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.thread.stack.transform.CallStackTransformation;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.List;


/**
 * Encapsulates the behaviour for running a set of "action blocks", particularly the complexities of dealing with
 * ACTIONIGNORE/ACTIONBREAK codes being thrown in action markup. When running one or more action blocks, a new XDoRunner
 * should be constructed. Each action block should be offered to the XDoRunner in turn using {@link #runCommands} - depending
 * on the XDoRunner's state, they may not actually be run. After all action blocks have been offered the consumer should
 * invoke {@link #processCompletion}. This handles any callstack transformations, errors, etc which have being cause by
 * action execution.<br/><br>
 *
 * An XDoRunner should not run commands recursively. For commands which execute nested commands, a new XDoRunner should be
 * constructed at each level of recursion. The result from a child XDoRunner should be returned to its parent, thereby
 * chaining the lowest result up to the top level.<br/><br>
 *
 * An action block is defined as a top level type of action, for instance auto-action-inits, top level user action and
 * auto-action-finals are the three standard action blocks for a normal page churn.
 */
public class XDoRunner {

  protected XDoControlFlow mControlFlow;

  /** Provides a belt-and-braces check against this runner being called recursively */
  private boolean mRunning = false;

  private boolean mTreatIgnoreAsBreak = false;

  public XDoRunner() {
    mControlFlow = XDoControlFlowContinue.instance();
  }

  /**
   * Runs the given commands if this runner is in the correct state (i.e. it has not encountered an ACTIONBREAK, call stack
   * transformation, or thrown error).
   * @param pRequestContext Current request context.
   * @param pXDoCommandList Commands to run.
   * @return The current XDoControlFlow stored on this XDoRunner.
   */
  public XDoControlFlow runCommands(ActionRequestContext pRequestContext, XDoCommandList pXDoCommandList){

    if(mRunning){
      throw new ExInternal("Cannot run commands on an XDoRunner which is already running");
    }
    //Check that this runner is allowed to run commands
    if(mControlFlow.canContinue()){
      //Record that we are currently running
      mRunning = true;

      Track.pushInfo("RunCommandList", pXDoCommandList.getPurpose());
      try {
        COMMAND_LOOP:
        for(Command lCommand : pXDoCommandList){

          Track.pushInfo("fm:" + lCommand.getCommandName(), lCommand);
          try {
            mControlFlow = lCommand.run(pRequestContext);

            //Belt and braces to ensure commands are behaving correctly
            if(mControlFlow == null) {
              throw new ExInternal("SERIOUS ERROR: command implementation for " + lCommand.getCommandName() +  " must NOT return null");
            }

            //We might be tracking removed labels on dev mode - check and immediately error if the most recent command has caused a problem
            if(!FoxGlobals.getInstance().isProduction() && pRequestContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.TRACK_UNATTACHED_LABEL)) {
              String lTrackedLabelName = pRequestContext.getDevToolbarContext().getTrackedContextLabelOrNull();
              if(lTrackedLabelName != null) {
                DOM lTrackedDOM = pRequestContext.getContextUElem().getUElemOrNull(lTrackedLabelName);
                if(lTrackedDOM != null && !lTrackedDOM.isAttached()) {
                  throw new ExInternal("Tracked context label " + lTrackedLabelName + " has been removed from the DOM tree by command " + lCommand.getDebugInfo());
                }
              }
            }

            //Immediate stop running commands in this action block if something happened to stop them
            if(!mControlFlow.canContinue()){
              break COMMAND_LOOP;
            }
          }
          catch(Throwable th) {
            //Note: this will cause verbose error stacks on nested calls. TODO PN investigate a better way (i.e. checked exception with command stack stored within it)
            throw new ExInternal("Error caught running command " + lCommand.getDebugInfo(), th);
          }
          finally {
            Track.pop("fm:" + lCommand.getCommandName());
          }
        }
      }
      finally {
        //Mark runner as no longer running
        mRunning = false;
        Track.pop("RunCommandList");
      }

      //Commands have finished running

      if(mControlFlow instanceof XDoControlFlowIgnore && !mTreatIgnoreAsBreak){
        //IGNORE codes cancel the current action block but allow subsequent ones to run. If this was an IGNORE,
        //reset the current control flow value so the runner is allowed to continue.
        mControlFlow = XDoControlFlowContinue.instance();
      }
    }

    return mControlFlow;
  }

  /**
   * Invokes any required processing based on the current XDoControlFlow stored on this XDoRunner. This may involve performing
   * a callstack transformation or throwing a hard error if an uncaught error was thrown. Also performs any assertions generated
   * by the fm:assert command.
   * @param pRequestContext Current request context. XDoResults may be added to this if a thread exit occurred.
   * @param pModuleCallStack Optional ModuleCallStack to invoke any transformations on.
   */
  public void processCompletion(ActionRequestContext pRequestContext, ModuleCallStack pModuleCallStack){

    //Loop through assertions and throw an error if there was a failure
    //TODO improve assertion functionality i.e. have option to fail immediately
    List<AssertionResult> lAsserionResults = pRequestContext.getXDoResults(AssertCommand.AssertionResult.class);
    for(AssertionResult lAssertionResult : lAsserionResults) {
      if(!lAssertionResult.assertionPassed()) {
        throw ExAssertion.createFromAssertionResultList(lAsserionResults);
      }
      else {
        Track.info("AssertionPassed", lAssertionResult.getFullMessage(), TrackFlag.ASSERTION);
      }
    }

    if(mControlFlow instanceof XDoControlFlowError) {
      Track.info("RethrowError");
      ((XDoControlFlowError) mControlFlow).reThrow();
    }
    else if(mControlFlow instanceof XDoControlFlowCST) {
      //Sanity check
      if(pModuleCallStack == null) {
        throw new ExInternal("Module call stack argument cannot be null when processing a CST");
      }

      //Grab the environment DOM now in case we need it after the CST processing
      DOM lEnvDOM = pRequestContext.getContextUElem().getUElem(ContextLabel.ENV);

      CallStackTransformation lCallStackTransformation = ((XDoControlFlowCST) mControlFlow).getCallStackTransformation();

      Track.pushInfo("CallStackTransformation", lCallStackTransformation);
      try {
        lCallStackTransformation.transform(pRequestContext, pModuleCallStack);
      }
      finally {
        Track.pop("CallStackTransformation");
      }

      //Process a thread exit - set the response to that specified on the exit-module command, or get the default action from the thread
      //Only do this if a response override has not already been set
      if(pModuleCallStack.isEmpty() && pRequestContext.getXDoResults(ResponseOverride.class).size() == 0){
        handleThreadExit(pRequestContext, pModuleCallStack, lEnvDOM, lCallStackTransformation);
      }
    }
  }

  private void handleThreadExit(ActionRequestContext pRequestContext, ModuleCallStack pModuleCallStack, DOM pEnvDOM, CallStackTransformation pCallStackTransformation) {
    if(pCallStackTransformation.getExitResponseOverride() != null) {
      pRequestContext.addXDoResult(pCallStackTransformation.getExitResponseOverride());
      Track.info("ThreadExit", "Exiting thread with overriden response (from exit transformation)");
    }
    else {
      //Get an exit response from the thread and handle it appropriately
      ExitResponse lExitResponse = pRequestContext.getDefaultExitResponse();

      if(lExitResponse instanceof ModulePushExitResponse) {
        //Thread exit page is a module reference, so do a call of the module name/entry theme
        Track.info("ThreadExit", "Pushing module onto callstack to prevent thread exit");

        ModuleCall.Builder lModuleCallBuilder = ((ModulePushExitResponse) lExitResponse).createBuilder(pRequestContext);

        lModuleCallBuilder.setEnvironmentDOM(pEnvDOM);
        CallStackTransformation lCST = CallStackTransformation.createCallStackTransformation(CallStackTransformation.Type.MODAL, lModuleCallBuilder);

        Track.pushInfo("ThreadExitModulePush");
        try {
          lCST.transform(pRequestContext, pModuleCallStack);
        }
        finally {
          Track.pop("ThreadExitModulePush");
        }
      }
      else if(lExitResponse instanceof ResponseOverride) {
        //Thread exit page is a URI or other response type (i.e. a self-closing modeless page)
        Track.info("ThreadExit", "Exiting thread with default response (from thread)");
        pRequestContext.addXDoResult((ResponseOverride) lExitResponse);
      }
      else {
        throw new ExInternal("Don't know how to treat an ExitResponse of type " + lExitResponse.getClass().getName());
      }
    }
  }

  /**
   * "Injects" a control flow result from an external source into this runner. This cannot be done while the runner is
   * in the middle of running actions. This should be used to override behaviour in exceptional circumstances when the
   * runner is the only point of reference available. It is preferable to always return the XDoControlFlow directly to a
   * consumer if possible.
   * @param pControlFlow New control flow, which will override this runner's current one.
   */
  public void injectResult(XDoControlFlow pControlFlow) {
    if(mRunning) {
      throw new ExInternal("Cannot inject a control flow while running");
    }

    mControlFlow = pControlFlow;
  }

  /**
   * Resets the state of this XDoRunner to allow it to run further commands regardless of its current state.
   */
  public void reset(){
    mControlFlow = XDoControlFlowContinue.instance();
  }

  /**
   * Tests if this XDoRunner will allow further actions to be executed (i.e. it has not hit an ACTIONBREAK, error or CST).
   * @return True if this runner will execute subsequent commands.
   */
  public boolean executionAllowed() {
    return mControlFlow.canContinue();
  }

  /**
   * If invoked, this XDoRunner will treat ACTIONIGNOREs in the same way as ACTIONBREAKs - i.e. no more commands will be
   * run after hitting an ACTIONIGNORE.
   */
  public void treatIgnoresAsBreaks() {
    mTreatIgnoreAsBreak = true;
  }

  /**
   * If there is currently a thrown error stored on this XDoRunner, returns the code for that error. Otherwise returns null.
   * @return Error code or null.
   */
  public String getThrownCodeOrNull(){
    if(mControlFlow instanceof XDoControlFlowThrownCode){
      return ((XDoControlFlowThrownCode) mControlFlow).getCode();
    }
    else {
      return null;
    }
  }

  /**
   * If there is currently a thrown error stored on this XDoRunner, returns the message for that error. Otherwise returns null.
   * @return Error message or null.
   */
  public String getThrownMessageOrNull(){
    if(mControlFlow instanceof XDoControlFlowThrownCode){
      return ((XDoControlFlowThrownCode) mControlFlow).getMessage();
    }
    else {
      return null;
    }
  }
}
