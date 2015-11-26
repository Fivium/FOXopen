package net.foxopen.fox.command.builtin;


import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of a FOX <code>try</code> command
 * comparable to that of the Java language itself.
 *
 * <p>Works with the <code>catch</code>,<code>finally</code> and
 * <code>throws</code> commands.
 *
 */
public class TryCommand
extends BuiltInCommand {

  /** The sequence of commands to try. */
  private final XDoCommandList mCommandsToTry;
  /** A list of catch commands to be run (potentially) in order of appearance. */
  private final List<CatchBlock> mCatchBlocks;
  /** Any finally command - always run last after any catch commands. Can be null. */
  private final XDoCommandList mFinallyCommandList;

   /**
   * Constructs a try command from the XML element specified.
   *
   * @param module the fox module where the command resides.
   * @param commandElement the element from which the command will
   *        be constructed.
   */
  private TryCommand(Mod module, DOM commandElement)
  throws ExDoSyntax {
    super(commandElement);
    DOMList lChildElements = commandElement.getChildElements();
    if(lChildElements.getLength() == 0 || !lChildElements.item(0).getName().endsWith("do")) {
      throw new ExInternal("Error parsing \"try\" command in module \"" +module.getName()+ "\" - expected a \"do\" command as the first element!");
    }
    mCommandsToTry = new XDoCommandList(module, lChildElements.item(0));
    mCommandsToTry.validate(module);

    DOMList lCatchElements = commandElement.getULByLocalName("catch");
    mCatchBlocks = new ArrayList<CatchBlock>(lCatchElements.getLength());
    for(DOM lCatchElem: lCatchElements) {
      mCatchBlocks.add( new CatchBlock(module, lCatchElem));
    }

    DOMList lFinallyElements = commandElement.getULByLocalName("finally");
    if(lFinallyElements.getLength() > 1 || (lFinallyElements.getLength() == 1 && !lChildElements.item(lChildElements.getLength()-1).getName().endsWith("finally"))) {
      throw new ExInternal("Error parsing \"try\" command in module \""+module.getName()+ "\" - only one \"finally\" command can be specified as part of a \"try...catch...finally\""+
                           " sequence. If used, the \"finally\" must be the last command of the \"try\" block.");
    }

    if(lFinallyElements.getLength() == 1) {
      mFinallyCommandList = XDoCommandList.parseNestedDoOrChildElements(module, lFinallyElements.item(0));
    }
    else {
      mFinallyCommandList = null;
    }
  }

  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    XDoRunner lActionRunner = pRequestContext.createCommandRunner(false);

    XDoControlFlow lTryCatchResult = lActionRunner.runCommands(pRequestContext, mCommandsToTry);

    String lThrownCode = lActionRunner.getThrownCodeOrNull();

    //A code was caught, check for a matching catch statement
    if(lThrownCode != null){

      pRequestContext.addSysDOMInfo("exceptions/current-code", lThrownCode);
      pRequestContext.addSysDOMInfo("exceptions/current-message", lActionRunner.getThrownMessageOrNull());

      lActionRunner.reset();

      for (CatchBlock lCatchBlock : mCatchBlocks) {
        if (lCatchBlock.isCodeCaught(lThrownCode) || lCatchBlock.isCodeCaught("OTHERS") ) {
          lTryCatchResult = lActionRunner.runCommands(pRequestContext, lCatchBlock.mCommandList);
          //Only process the first matching catch if there are many
          break;
        }
      }
    }

    //Legacy behaviour: CSTs skipped the finally run
    XDoControlFlow lFinallyResult = XDoControlFlowContinue.instance();
    if(mFinallyCommandList != null && !lTryCatchResult.isCallStackTransformation()) {
      lActionRunner.reset();
      lFinallyResult = lActionRunner.runCommands(pRequestContext, mFinallyCommandList);
    }

    //Finally result should take precedence if it causes a control flow disruption
    if(!lFinallyResult.canContinue()){
      return lFinallyResult;
    }
    else {
      return lTryCatchResult;
    }
  }

  @Override
  public void validate(Mod pModule) {
    mCommandsToTry.validate(pModule);

    for (CatchBlock lCatchBlock : mCatchBlocks) {
      lCatchBlock.mCommandList.validate(pModule);
    }

    if(mFinallyCommandList != null) {
      mFinallyCommandList.validate(pModule);
    }
  }

  private static class CatchBlock {
    /** The sequence of commands to run after catch. */
    private final XDoCommandList mCommandList;

    /** A set of codes that are caught by this catch clause. */
    private Set<String> mCodesCaught = new HashSet<>();

     /**
     * Constructs a try command from the XML element specified.
     *
     * @param module the fox module where the command resides.
     * @param commandElement the element from which the command will
     *        be constructed.
     */
    private CatchBlock(Mod module, DOM commandElement)
    throws ExDoSyntax {
      mCommandList = XDoCommandList.parseNestedDoOrChildElements(module, commandElement);

      String codes = commandElement.getAttr("codes");
      if (codes == null) {
        throw new ExInternal("Error parsing \"catch\" command in module \""+module.getName()+ "\" - catch must have a \"codes\" attribute which speicifies the code(s) that are caught.");
      }
      mCodesCaught.addAll(Arrays.asList(codes.split(" \t")));
    }

    /**
    * Determines whether the specified error code is caught by this <code>catch</code>
    * clause.
    *
    * @param code the code to test against this catch clause
    * @return true if the specified code is caught by this catch, false otherwise
    */
    public boolean isCodeCaught(String code) {
      return mCodesCaught.contains(code);
    }
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new TryCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("try");
    }
  }
}
