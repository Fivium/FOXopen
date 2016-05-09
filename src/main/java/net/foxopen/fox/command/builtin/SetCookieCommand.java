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

public class SetCookieCommand
extends BuiltInCommand {

  private final String mNameXPath;
  private final String mValueXPath;
  private final String mMaxAgeXPath;
  private final String mSetPathXPath;
  private final String mHttpOnlyXPath;

  private SetCookieCommand(DOM pParseUElem)
  throws ExInternal, ExDoSyntax {
    super(pParseUElem);
    mNameXPath = pParseUElem.getAttr("name");
    mValueXPath = pParseUElem.getAttr("value");
    mMaxAgeXPath = pParseUElem.getAttr("max-age");
    mSetPathXPath = pParseUElem.getAttr("set-path");
    mHttpOnlyXPath = pParseUElem.getAttr("http-only");

    if(XFUtil.isNull(mNameXPath)) {
      throw new ExDoSyntax("name attribute must be specified");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    String lName, lValue = "";
    boolean lPath = true, lHttpOnly = true;
    Integer lMaxAge = null;
    try {
      lName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mNameXPath);

      if(!XFUtil.isNull(mValueXPath)) {
        lValue = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mValueXPath);
      }

      if(!XFUtil.isNull(mMaxAgeXPath)) {
        lMaxAge = lContextUElem.extendedConstantOrXPathResult(lContextUElem.attachDOM(), mMaxAgeXPath).asNumber().intValue();
      }

      if(!XFUtil.isNull(mSetPathXPath)) {
        lPath = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mSetPathXPath);
      }

      if(!XFUtil.isNull(mHttpOnlyXPath)) {
        lHttpOnly = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mHttpOnlyXPath);
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate value attribute for fm:set-cookie", e);
    }

    // If the value is null/empty we should remove the cookie
    if (XFUtil.isNull(mValueXPath)) {
      pRequestContext.getFoxRequest().removeCookie(lName, lPath);
    }
    else if (lMaxAge != null) {
      // If max-age was set pass it through
      pRequestContext.getFoxRequest().addCookie(lName, lValue, lMaxAge, lPath, lHttpOnly);
    }
    else {
      // If no max-age was set we can't pass a null through so call a different overload
      pRequestContext.getFoxRequest().addCookie(lName, lValue, lPath, lHttpOnly);
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
      return new SetCookieCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("set-cookie");
    }
  }
}
