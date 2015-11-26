package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

public class ContextClearCommand
extends BuiltInCommand {

  private final String mScope;
  private final String mName;

  /** Constructs the command from the XML element specified */
  private ContextClearCommand(DOM pCmdDOM)
  throws ExInternal, ExDoSyntax {
    super(pCmdDOM);
    mScope = pCmdDOM.getAttr("scope");
    mName = pCmdDOM.getAttr("name");
    if(XFUtil.isNull(mScope) || XFUtil.isNull(mName)) {
      throw new ExDoSyntax("context-clear: missing attrs: scope or name");
    }
    else if(!mScope.equals(ContextSetCommand.SCOPE_STATE) && !mScope.equals(ContextSetCommand.SCOPE_LOCALISED)) {
      throw new ExDoSyntax("context-clear: scope must be 'state' or 'localised'");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    // Get context name - convert when in string(...) format
    String lName;
    try {
      lName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mName);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("context-clear: Error getting context name: "+mName, e);
    }
    if(XFUtil.isNull(lName)) {
      throw new ExInternal("context-clear: name attr returned null: "+mName);
    }

    boolean lLocalisedClear = ContextSetCommand.SCOPE_LOCALISED.equals(mScope);

    if(lLocalisedClear && !lContextUElem.isLocalised()) {
      throw new ExInternal("Cannot do a localised context-clear from a context which is not localised");
    }

    if(lLocalisedClear) {
      //Remove the label from the current localised level of the ContextUElem
      lContextUElem.removeUElem(lName);
    }
    else {
      //Remove the label from ALL levels of the ContextUElem
      lContextUElem.removeUElemGlobal(lName);
    }

    return XDoControlFlowContinue.instance();
  }

   public boolean isCallTransition() {
     return false;
   }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ContextClearCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("context-clear");
    }
  }
}
