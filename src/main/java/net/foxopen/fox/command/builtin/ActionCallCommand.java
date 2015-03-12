package net.foxopen.fox.command.builtin;

import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

public class ActionCallCommand
extends BuiltInCommand {

  private final String mActionName;

  // Parse command returning tokenised representation
  private ActionCallCommand(DOM pParseUElem)
  throws ExInternal, ExDoSyntax {
    super(pParseUElem);
    mActionName = pParseUElem.getAttr("action");
  }

  @Override
  public void validate(Mod pModule) {
    if(pModule.badActionName(mActionName)) {
      pModule.addBulkModuleErrorMessage("\nBad Action Name in <call> command: "+mActionName);
    }
  }

  public boolean isCallTransition() {
   return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    XDoCommandList lActionToRun = pRequestContext.resolveActionName(mActionName);
    return pRequestContext.createCommandRunner(false).runCommands(pRequestContext, lActionToRun);
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ActionCallCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("call");
    }
  }
}
