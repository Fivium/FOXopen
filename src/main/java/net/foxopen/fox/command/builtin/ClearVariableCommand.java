package net.foxopen.fox.command.builtin;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

public class ClearVariableCommand
extends BuiltInCommand {

  private final String mVariableName;

  private ClearVariableCommand(DOM pParseUElem)
  throws ExInternal, ExDoSyntax {
    super(pParseUElem);
    mVariableName = pParseUElem.getAttr("name");

    if(XFUtil.isNull(mVariableName)) {
      throw new ExDoSyntax("name attribute must be specified");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    pRequestContext.getXPathVariableManager().clearVariable(mVariableName);
    return XDoControlFlowContinue.instance();
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ClearVariableCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("clear-variable");
    }
  }
}
