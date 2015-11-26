package net.foxopen.fox.command.builtin;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.transform.StateStackTransformation;

import java.util.Arrays;
import java.util.Collection;


/**
 * Implementation of a FOX command that performs push, pop and replace state manipulation.
 */
public final class StateCommand
extends BuiltInCommand {

  public static final String OPERATION_REPLACE = "replace";
  public static final String OPERATION_PUSH = "push";
  public static final String OPERATION_POP = "pop";
  public static final String OPERATION_STRICT_POP = "strict-pop";
  public final String mStateName;
  public final String mAttach;
  public final boolean mReplaceAll;

  public final String mOperationName;

  /**
  * Constructs a StateCommand command from the XML element specified.
  *
  * @param pCommandDOM the element from which the command will be constructed.
  */
  private StateCommand(Mod pMod, DOM pCommandDOM) {
    super(pCommandDOM);
    // Determine push, pop, replace
    String lName = pCommandDOM.getLocalName();
    if(lName.equals("state-push") || lName.equals("state-replace") || lName.equals("state-pop") || lName.equals("state-strict-pop")) {
      mOperationName = lName.substring(6);
    }
    else {
      mOperationName = pCommandDOM.getAttrOrNull("action");
    }

    if (!OPERATION_REPLACE.equals(mOperationName) && !OPERATION_PUSH.equals(mOperationName) && !OPERATION_POP.equals(mOperationName) && !OPERATION_STRICT_POP.equals(mOperationName)) {
      throw new ExInternal("A fm:state within module "+pMod.getName()+" has no push, pop or replace");
    }

    // Member variables
    mStateName = pCommandDOM.getAttrOrNull("name");
    mAttach = pCommandDOM.getAttrOrNull("attach");
    mReplaceAll = Boolean.valueOf(pCommandDOM.getAttrOrNull("all")); // null is allowed in Boolean.valueof()

    if((mOperationName.equals(OPERATION_PUSH) || mOperationName.equals(OPERATION_REPLACE)) && XFUtil.isNull(mStateName)) {
      throw new ExInternal("Invalid command syntax: state command does not specify a destination state name");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    StateStackTransformation lTransformation;

    switch (mOperationName) {
      case OPERATION_PUSH:
        lTransformation = StateStackTransformation.createPushTransformation(mStateName, mAttach);
        break;
      case OPERATION_POP:
      case OPERATION_STRICT_POP:
        lTransformation = StateStackTransformation.createPopTransformation(OPERATION_STRICT_POP.equals(mOperationName), mAttach);
        break;
      case OPERATION_REPLACE:
        lTransformation = StateStackTransformation.createReplaceTransformation(mStateName, mReplaceAll, mAttach);
        break;
      default:
        throw new ExInternal(mOperationName + " not implemented");
    }

    return pRequestContext.handleStateStackTransformation(lTransformation);
  }

  public boolean isCallTransition() {
    return OPERATION_POP.equals(mOperationName);
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new StateCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Arrays.asList("state-push", "state-replace", "state-pop", "state-strict-pop", "state");
    }
  }
}
