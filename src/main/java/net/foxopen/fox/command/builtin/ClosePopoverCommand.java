package net.foxopen.fox.command.builtin;

import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.facet.ModalPopoverProvider;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

/**
 * Command for closing a modal popover created from a {@link ShowPopoverCommand}.
 */
public class ClosePopoverCommand
extends BuiltInCommand {

  private ClosePopoverCommand(DOM pCommandDOM) {
    super(pCommandDOM);
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    pRequestContext.getModuleFacetProvider(ModalPopoverProvider.class).closeCurrentPopover();

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
      return new ClosePopoverCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("close-popover");
    }
  }
}
