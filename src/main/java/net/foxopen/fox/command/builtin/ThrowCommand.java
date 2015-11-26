package net.foxopen.fox.command.builtin;


import java.util.Arrays;
import java.util.Collection;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowBreak;
import net.foxopen.fox.command.flow.XDoControlFlowError;
import net.foxopen.fox.command.flow.XDoControlFlowIgnore;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

public class ThrowCommand
extends BuiltInCommand {

  private static final String THROW_BREAK_COMMAND_NAME = "throw-break";
  private static final String THROW_IGNORE_COMMAND_NAME = "throw-ignore";

  /** The code of the throw command. */
  private String mCodeOrXPath;

  /** The message top be thrown. */
  private String mMessageOrXPath;

  /**
   * Constructs a throw command from the XML element specified.
   * @param pMod the fox module where the command resides.
   * @param pCommandElement the element from which the command will be constructed.
   * @throws ExInternal
   */
  private ThrowCommand(DOM pCommandElement, String pCode) throws ExInternal {
    super(pCommandElement);
    mCodeOrXPath = XFUtil.nvl(pCode, "NONE");
    mMessageOrXPath = getAttribute("message", "NO-MESSAGE");
  }

  public boolean isCallTransition() {
    return true;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    final String lCode;
    final String lMessage;
    try {
      lCode = pRequestContext.getContextUElem().extendedStringOrXPathString(pRequestContext.getContextUElem().attachDOM(), mCodeOrXPath);
      lMessage = pRequestContext.getContextUElem().extendedStringOrXPathString(pRequestContext.getContextUElem().attachDOM(), mMessageOrXPath);
    }
    // Rethrow if XPath evaluation fails
    catch (ExActionFailed ex) {
      throw new ExInternal("fm:throw was invoked with an invalid XPath for one of the parameters", ex);
    }

    if(XDoControlFlowIgnore.IGNORE_CODE.equals(lCode)){
      return new XDoControlFlowIgnore(lMessage);
    }
    else if(XDoControlFlowBreak.BREAK_CODE.equals(lCode)){
      return new XDoControlFlowBreak(lMessage);
    }
    else {
      return new XDoControlFlowError(lCode, lMessage, new ExActionFailed(lCode, lMessage));
    }
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {

      String lCode;
      if(THROW_BREAK_COMMAND_NAME.equals(pMarkupDOM.getLocalName())) {
        lCode = XDoControlFlowBreak.BREAK_CODE;
      }
      else if(THROW_IGNORE_COMMAND_NAME.equals(pMarkupDOM.getLocalName())) {
        lCode = XDoControlFlowIgnore.IGNORE_CODE;
      }
      else {
        lCode = pMarkupDOM.getAttr("code");
      }

      return new ThrowCommand(pMarkupDOM, lCode);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Arrays.asList("throw", THROW_BREAK_COMMAND_NAME, THROW_IGNORE_COMMAND_NAME);
    }
  }
}
