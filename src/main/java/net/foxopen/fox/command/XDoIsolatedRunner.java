package net.foxopen.fox.command;

import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowCST;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

/**
 * An XDoRunner implementation which does not allow call stack transformations. Can be set to optionally throw a hard error
 * if a CST is attempted.
 */
public class XDoIsolatedRunner
extends XDoRunner {

  private boolean mErrorOnTransform;

  /**
   * Creates a new XDoIsolatedRunner.
   * @param pErrorOnTransform If true, a hard error will be thrown in the case of a CST. If false an alert is logged.
   */
  public XDoIsolatedRunner(boolean pErrorOnTransform) {
    super();
    mErrorOnTransform = pErrorOnTransform;
  }

  /**
   * Runs commands as in {@link XDoRunner#runCommands}, ignoring or erroring on callstack transformations as appropriate.
   * Use this method when the command runner is running multiple sets of command lists. Otherwise, use {@link #runCommandsAndComplete}.
   * @param pRequestContext Current RequestContext.
   * @param pXDoCommandList Commands to run.
   * @return Result of running commands.
   */
  public XDoControlFlow runCommands(ActionRequestContext pRequestContext, XDoCommandList pXDoCommandList) {
    XDoControlFlow lControlFlow = super.runCommands(pRequestContext, pXDoCommandList);
    if(lControlFlow instanceof XDoControlFlowCST){
      if(mErrorOnTransform){
        throw new ExInternal("Call stack transformation not allowed here");
      }
      else {
        Track.alert("IgnoredCallStackTransformation", "Call stack transformation was suppressed - not allowed in this context");
        mControlFlow = XDoControlFlowContinue.instance();
      }
    }
    return lControlFlow;
  }

  /**
   * One hit method for running commands and performing any completion actions (i.e. converting an uncaught thrown code
   * into a Java exception). Use this method when the command runner only needs to run a single command list.
   * @param pRequestContext Current RequestContext.
   * @param pXDoCommandList Commands to run.
   * @return Result of running commands.
   */
  public XDoControlFlow runCommandsAndComplete(ActionRequestContext pRequestContext, XDoCommandList pXDoCommandList) {
    XDoControlFlow lCommandResult = runCommands(pRequestContext, pXDoCommandList);
    processCompletion(pRequestContext, null);
    return lCommandResult;
  }
}
