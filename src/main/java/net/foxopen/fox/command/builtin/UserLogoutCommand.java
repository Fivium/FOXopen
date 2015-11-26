package net.foxopen.fox.command.builtin;

import net.foxopen.fox.auth.AuthUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;


public class UserLogoutCommand
extends BuiltInCommand {

   /**
   * Constructs the command from the XML element specified.
   *
   * @param commandElement the element from which the command will
   *        be constructed.
   */
  private UserLogoutCommand(DOM commandElement) {
    super(commandElement);
  }

  public boolean isCallTransition() {
   return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    pRequestContext.getAuthenticationContext().logout(pRequestContext, AuthUtil.getClientInfoNVP(pRequestContext.getFoxRequest()));

    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new UserLogoutCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("user-logout");
    }
  }
}
