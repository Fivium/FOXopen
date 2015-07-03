package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ActionCallCommand
extends BuiltInCommand {

  private final String mActionName;
  private final List<SetContextMarkup> mContextsToSet;
  private final List<SetVariableMarkup> mVariablesToSet;

  // Parse command returning tokenised representation
  private ActionCallCommand(DOM pParseUElem)
  throws ExInternal, ExDoSyntax {
    super(pParseUElem);
    mActionName = pParseUElem.getAttr("action");

    //Parse all fm:context definitions (for implicit context localises)
    mContextsToSet = Collections.unmodifiableList(
      pParseUElem.getUL("fm:context")
        .stream()
        .map(d -> new SetContextMarkup(d.getAttr("name"), d.getAttr("xpath")))
        .collect(Collectors.toList())
    );

    //Parse all fm:variable definitions for local variables
    mVariablesToSet = Collections.unmodifiableList(
      pParseUElem.getUL("fm:variable")
        .stream()
        .map(d -> new SetVariableMarkup(d.getAttr("name"), d.getAttr("textValue"), d.getAttr("expr")))
        .collect(Collectors.toList())
    );
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

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    //Only bother localising if we have contexts to set
    if(mContextsToSet.size() > 0) {
      lContextUElem.localise("fm:call/" + mActionName);
    }

    try {
      //Set all required contexts
      mContextsToSet.forEach(e -> e.setContext(lContextUElem));

      //Evaluate all local variable definitions into a map of variable names to values/XPathResults, then localise the variable manager
      Map<String, Object> lVariables = mVariablesToSet.stream().collect(Collectors.toMap(e -> e.mName, e -> e.evaluate(lContextUElem)));

      pRequestContext.getXPathVariableManager().localise("fm:call/" + mActionName, lVariables);
      try {
        //Resolve and run target action
        XDoCommandList lActionToRun = pRequestContext.resolveActionName(mActionName);
        return pRequestContext.createCommandRunner(false).runCommands(pRequestContext, lActionToRun);
      }
      finally {
        pRequestContext.getXPathVariableManager().delocalise("fm:call/" + mActionName);
      }
    }
    finally {
      if(mContextsToSet.size() > 0) {
        lContextUElem.delocalise("fm:call/" + mActionName);
      }
    }
  }

  /**
   * Parsed fm:context element.
   */
  private static class SetContextMarkup {
    private final String mName;
    private final String mXPath;

    private SetContextMarkup(String pName, String pXPath) {
      mName = pName;
      mXPath = pXPath;

      if(XFUtil.isNull(mName) || XFUtil.isNull(mXPath)) {
        throw new ExInternal("fm:context - both name and xpath must be specified");
      }
    }

    void setContext(ContextUElem pContextUElem) {
      DOM lDOM;
      try {
        lDOM = pContextUElem.extendedXPath1E(pContextUElem.attachDOM(), mXPath);
      }
      catch (ExTooMany | ExActionFailed | ExTooFew e) {
        throw new ExInternal("Failed to set context label " + mName, e);
      }
      pContextUElem.setUElem(mName, ContextualityLevel.LOCALISED, lDOM);
    }
  }

  /**
   * Parsed fm:variable element, which may be an expression or fixed text value (mutually exclusive).
   */
  private static class SetVariableMarkup {
    private final String mName;
    private final String mTextValue;
    private final String mExpr;

    private SetVariableMarkup(String pName, String pTextValue, String pExpr) {
      mName = pName;
      mTextValue = pTextValue;
      mExpr = pExpr;

      if(XFUtil.isNull(mName)) {
        throw new ExInternal("fm:variable - name must be specified");
      }
      else if(!(XFUtil.isNull(mTextValue) ^ XFUtil.isNull(mExpr))) {
        throw new ExInternal("fm:variable - exactly one of textValue or expr must be specified");
      }
    }

    Object evaluate(ContextUElem pContextUElem) {

      if(!XFUtil.isNull(mExpr)) {
        try {
          return pContextUElem.extendedXPathResult(pContextUElem.attachDOM(), mExpr);
        }
        catch (ExActionFailed e) {
          throw new ExInternal("Failed to evaluate expression attribute for variable " + mName, e);
        }
      }
      else {
        return mTextValue;
      }
    }
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
