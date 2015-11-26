package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.servlets.FoxMainServlet;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.Collection;
import java.util.Collections;


public class TransactionCommand
extends BuiltInCommand {

  private static enum OperationType {
    JOIN,
    COMMIT,
    ROLLBACK,
    AUTONOMOUS,
    SPLIT;
  }

  private final OperationType mOperationType;
  private final TransactionOperation mTransactionOperation;

  /*
   * Nested classes required due to current reflection-based constructor invocation which means the fm:transaction
   * tag can only map to one class.
   */

  private static interface TransactionOperation {
    public XDoControlFlow run(ActionRequestContext pRequestContext);
  }

  /** JOIN, AUTONOMOUS and SPLIT commands */
  private abstract class SwitchOperation
  implements TransactionOperation {

    protected final String mTransactionName;
    protected final XDoCommandList mNestedXDoCommandList;

    private SwitchOperation(Mod pMod, DOM pCmdDOM) throws ExDoSyntax {
      // Determine transaction name (if specified)
      String lTransactionName = getAttribute("transaction");

      //Legacy default for JOIN (no longer used)
      if(mOperationType == TransactionCommand.OperationType.JOIN) {
        lTransactionName = XFUtil.nvl(lTransactionName, "MAIN");
      }

      mTransactionName = lTransactionName;

      if(XFUtil.isNull(mTransactionName)) {
        throw new ExDoSyntax("Transaction name must be specified for transaction " + mOperationType + " operation");
      }

      // Parse nested do block (if specified)
      DOM lNestedDoBlockDOM;
      try {
        lNestedDoBlockDOM = pCmdDOM.get1EByLocalName("do");
      }
      catch(ExCardinality x) {
        throw new ExDoSyntax("Error locating do block within transaction block", x);
      }

      mNestedXDoCommandList = new XDoCommandList(pMod, lNestedDoBlockDOM);
    }

    protected XDoControlFlow runNestedCommands(ActionRequestContext pRequestContext) {
      return pRequestContext.createCommandRunner(false).runCommands(pRequestContext, mNestedXDoCommandList);
    }
  }

  private class JoinOperation
  extends SwitchOperation {

    private JoinOperation(Mod pMod, DOM pCmdDOM) throws ExDoSyntax {
      super(pMod, pCmdDOM);
    }

    @Override
    public XDoControlFlow run(ActionRequestContext pRequestContext) {
      Track.info("TransactionJoin", "Posting ROOT DOM (no new connection)");
      pRequestContext.postDOM(ContextLabel.ROOT.asString());
      return runNestedCommands(pRequestContext);
    }
  }

  private class AutonomousOperation
  extends SwitchOperation {

    private AutonomousOperation(Mod pMod, DOM pCmdDOM) throws ExDoSyntax {
      super(pMod, pCmdDOM);
    }

    @Override
    public XDoControlFlow run(ActionRequestContext pRequestContext) {
      Track.pushInfo("TransactionAutonomous", "Start autonomous transaction " + mTransactionName);
      try {
        pRequestContext.getContextUCon().pushAutonomousConnection(mTransactionName);
        try {
          return runNestedCommands(pRequestContext);
        }
        catch(Throwable th) {
          pRequestContext.getContextUCon().rollbackCurrentConnection();
          throw th;
        }
        finally {
          pRequestContext.getContextUCon().popConnection(mTransactionName);
        }
      }
      finally {
        Track.pop("TransactionAutonomous");
      }
    }
  }

  private class SplitOperation
  extends SwitchOperation {

    private SplitOperation(Mod pMod, DOM pCmdDOM) throws ExDoSyntax {
      super(pMod, pCmdDOM);
    }

    @Override
    public XDoControlFlow run(ActionRequestContext pRequestContext) {
      Track.pushInfo("TransactionSplit", "Splitting to transaction " + mTransactionName);
      try {
        pRequestContext.getContextUCon().pushRetainedConnection(mTransactionName);
        try {
          return runNestedCommands(pRequestContext);
        }
        finally {
          pRequestContext.getContextUCon().popConnection(mTransactionName);
        }
      }
      finally {
        Track.pop("TransactionSplit");
      }
    }
  }

  /** COMMIT and ROLLBACK commands */
  private class ControlOperation
  implements TransactionOperation {

    private ControlOperation() throws ExDoSyntax {
      if(!XFUtil.isNull(getAttribute("transaction"))) {
        throw new ExDoSyntax("Cannot specify a transaction name on an fm:transaction" + mOperationType);
      }
    }

    @Override
    public XDoControlFlow run(ActionRequestContext pRequestContext) {
      ContextUCon lContextUCon = pRequestContext.getContextUCon();

      if(lContextUCon.isTransactionControlAllowed()) {
        if(mOperationType == TransactionCommand.OperationType.COMMIT) {
          lContextUCon.commitCurrentConnection();
        }
        else if(mOperationType == TransactionCommand.OperationType.ROLLBACK) {
          lContextUCon.rollbackCurrentConnection();
        }
      }
      else {
        String lCurConName = lContextUCon.getCurrentConnectionName();
        if(FoxMainServlet.MAIN_CONNECTION_NAME.equals(lCurConName)) {
          //TODO this should always throw an error - don't for now as lots of old module markup may have commits/rollbacks in legacy code
          Track.alert("TransactionCommand", mOperationType + " not permitted on current connection " + lCurConName +
          " - hard error skipped for backwards compatibility", TrackFlag.ACTION_PROCESSING);
        }
        else {
          throw new ExInternal(mOperationType + " not permitted on current connection " + lCurConName);
        }
      }

      return XDoControlFlowContinue.instance();
    }
  }

  public TransactionCommand(Mod pMod, DOM pCmdDOM)
  throws ExInternal, ExDoSyntax {

    super(pCmdDOM);

    // Determine transaction type
    String lTypeAttr = getAttribute("operation", "UNDEFINED").toUpperCase();
    mOperationType = OperationType.valueOf(lTypeAttr);
    if(mOperationType == null) {
      throw new ExDoSyntax("Unrecognised transaction operation type: "+lTypeAttr);
    }

    switch(mOperationType) {
      case JOIN:
        Track.info("TransactionCommand", "Transaction JOIN deprecated - use fm:post-dom", TrackFlag.BAD_MARKUP);
        mTransactionOperation = new JoinOperation(pMod, pCmdDOM);
        break;
      case SPLIT:
        mTransactionOperation = new SplitOperation(pMod, pCmdDOM);
        break;
      case AUTONOMOUS:
        mTransactionOperation = new AutonomousOperation(pMod, pCmdDOM);
        break;
      case COMMIT:
      case ROLLBACK:
        mTransactionOperation = new ControlOperation();
        break;
      default:
        throw new ExDoSyntax("Unrecognised transaction operation type: "+mOperationType); //Shouldn't happen
    }
  }

  public void validate(Mod pMod) {
    if(mTransactionOperation instanceof SwitchOperation) {
      ((SwitchOperation) mTransactionOperation).mNestedXDoCommandList.validate(pMod);
    }
  }

   public boolean isCallTransition() {
     return false;
   }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    return mTransactionOperation.run(pRequestContext);
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new TransactionCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("transaction");
    }
  }
}
