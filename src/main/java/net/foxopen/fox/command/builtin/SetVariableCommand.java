package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

public class SetVariableCommand
extends BuiltInCommand {

  private final String mVariableName;

  private final String mXPath;
  private final String mTextValue;

  private SetVariableCommand(DOM pParseUElem)
  throws ExInternal, ExDoSyntax {
    super(pParseUElem);
    mVariableName = pParseUElem.getAttr("name");

    mXPath = pParseUElem.getAttr("expr");
    mTextValue = pParseUElem.getAttr("textValue");

    if(XFUtil.isNull(mVariableName)) {
      throw new ExDoSyntax("name attribute must be specified");
    }

    if(!(XFUtil.isNull(mXPath) ^ XFUtil.isNull(mTextValue))) {
      throw new ExDoSyntax("expr and textValue attributes are mutually exclusive and one must be specified");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    if(!XFUtil.isNull(mXPath)) {
      //XPath specified, evaluate and set variable to result

      ContextUElem lContextUElem = pRequestContext.getContextUElem();
      XPathResult lXPathResult;
      try {
        lXPathResult = lContextUElem.extendedXPathResult(lContextUElem.attachDOM(), mXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate expr attribute for fm:set-variable", e);
      }

      pRequestContext.getXPathVariableManager().setVariableFromXPathResult(mVariableName, lXPathResult);
    }
    else {
      //Text value specified, no evaluation required
      pRequestContext.getXPathVariableManager().setVariable(mVariableName, mTextValue);
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
      return new SetVariableCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("set-variable");
    }
  }
}
