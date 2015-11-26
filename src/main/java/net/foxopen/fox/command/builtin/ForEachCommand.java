package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.ForEachIterator;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;


/**
 * Implementation of a FOX <code>for-each</code> command
 * comparable to that of the Java language <code>for(;;)</code> statement.
 *
 * <p>Iterates over boolean/node-list condition. Care must be
 * taken when the condition is a node-list as the condition is
 * always <i>true</i> for non-empty lists.
 *
 */
public class ForEachCommand
extends BuiltInCommand {

  /** The sequence of commands to try. */
  private final XDoCommandList mNestedCommands;

  /** The xpath expression over which the iteration will occur. */
  private final String mXPathExpression;

  private final String mItemContextName;

  private final String mStatusContextName;

  private final Double mNumRangeFrom;
  private final Double mNumRangeTo;
  private final Double mNumRangeStep;

   /**
   * Constructs a For Each command from the XML element specified.
   *
   * @param module the fox module where the command resides.
   * @param commandElement the element from which the command will
   *        be constructed.
   */
   private ForEachCommand(Mod module, DOM commandElement)
   throws ExDoSyntax {
    super(commandElement);

    mNestedCommands = XDoCommandList.parseNestedDoOrChildElements(module, commandElement);

    mXPathExpression = getAttribute("xpath");
    mItemContextName = getAttribute("itemContextName", "loopitem");
    mStatusContextName = getAttribute("statusContextName", "loopstatus");

    String lNumRangeFromAttr = getAttribute("num-range-from");
    if(lNumRangeFromAttr != null) {
      mNumRangeFrom = Double.parseDouble(lNumRangeFromAttr);
    }
    else {
      mNumRangeFrom = null;
    }

    String lNumRangeToAttr = getAttribute("num-range-to");
    if(lNumRangeToAttr != null) {
      mNumRangeTo = Double.parseDouble(lNumRangeToAttr);
    }
    else {
      mNumRangeTo = null;
    }

    String lNumRangeStepAttr = getAttribute("num-range-step");
    if(lNumRangeStepAttr != null) {
      mNumRangeStep = Double.parseDouble(lNumRangeStepAttr);
    }
    else {
      mNumRangeStep = null;
    }
  }

  public XDoControlFlow run(final ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    //Retrieve items to be looped over from XPath
    DOMList lIterationDOMList;
    if (!XFUtil.isNull(mXPathExpression)) {
      try {
        lIterationDOMList = lContextUElem.extendedXPathUL(mXPathExpression, ContextUElem.ATTACH);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate XPath " + mXPathExpression + " for for-each expression", e);
      }
    }
    else {
      lIterationDOMList = new DOMList();
    }

    //Set up an executor to run the loop
    class LoopCommandExecutor
    implements ForEachIterator.IterationExecutable {

      //Default to continue in case the loop has 0 iterations
      XDoControlFlow mLastResult = XDoControlFlowContinue.instance();
      final XDoRunner mRunner = pRequestContext.createCommandRunner(false);

      @Override
      public boolean execute(DOM pOptionalCurrentItem, ForEachIterator.Status pIteratorStatus) {
        mLastResult = mRunner.runCommands(pRequestContext, mNestedCommands);
        //Only allow loop to continue if the executed command allows a continue
        return mLastResult.canContinue();
      }
    }

    LoopCommandExecutor lExecutor = new LoopCommandExecutor();

    //Construct and run iterator
    ForEachIterator lIterator = new ForEachIterator(!XFUtil.isNull(mXPathExpression), mItemContextName, mStatusContextName, mNumRangeFrom, mNumRangeTo, mNumRangeStep);
    lIterator.doForEach(lContextUElem, lIterationDOMList, lExecutor);

    //Return the last result the executor has (might be an interrupt if the loop stopped prematurely)
    return lExecutor.mLastResult;
  }


   /**
   * Validates the command syntax.
   *
   * @param module the module where the component resides
   * @throws ExInternal if the component syntax is invalid.
   */
   public void validate(Mod module) {
     mNestedCommands.validate(module);
   }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ForEachCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("for-each");
    }
  }
}
