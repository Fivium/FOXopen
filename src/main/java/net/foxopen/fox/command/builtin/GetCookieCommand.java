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
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

public class GetCookieCommand
extends BuiltInCommand {

  private final String mNameXPath;
  private final String mResultXPath;

  private GetCookieCommand(DOM pParseUElem)
  throws ExInternal, ExDoSyntax {
    super(pParseUElem);
    mNameXPath = pParseUElem.getAttr("name");
    mResultXPath = pParseUElem.getAttr("result");

    if(XFUtil.isNull(mNameXPath) || XFUtil.isNull(mResultXPath)) {
      throw new ExDoSyntax("name and result attributes must be specified");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    try {
      String lName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mNameXPath);

      String lCookieValue = pRequestContext.getFoxRequest().getCookieValue(lName);

      // Get/create the result node to store the value in and then store it
      DOM lResultTargetNode = lContextUElem.getCreateXPath1E(mResultXPath);
      lResultTargetNode.setText(lCookieValue);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate value attribute for fm:get-cookie", e);
    }
    catch (ExTooMany e) {
      throw new ExInternal("Failed to evaluate value attribute for fm:get-cookie", e);
    }

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
      return new GetCookieCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("get-cookie");
    }
  }
}
