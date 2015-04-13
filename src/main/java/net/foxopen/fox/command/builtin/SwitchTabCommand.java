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
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.tabs.TabGroup;
import net.foxopen.fox.module.tabs.TabGroupProvider;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

public class SwitchTabCommand
extends BuiltInCommand {

  private final String mTabGroupName;
  private final String mTabGroupAttachXPath;

  private final String mTabKeyXPath;
  private final String mTabDOMXPath;

  private SwitchTabCommand(DOM pMarkupDOM) throws ExDoSyntax {
    super(pMarkupDOM);
    mTabGroupName = pMarkupDOM.getAttrOrNull("tabGroupName");
    mTabGroupAttachXPath = pMarkupDOM.getAttrOrNull("tabGroupAttach");

    mTabKeyXPath = pMarkupDOM.getAttrOrNull("tabKey");
    mTabDOMXPath = pMarkupDOM.getAttrOrNull("tabDOM");

    if(XFUtil.isNull(mTabGroupName)) {
      throw new ExDoSyntax("tabGroupName attribute must be specified");
    }

    if(XFUtil.isNull(mTabKeyXPath) == XFUtil.isNull(mTabDOMXPath)) {
      throw new ExDoSyntax("Exactly one of tabKey or tabDOM must be specified");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    DOM lTabGroupAttach;
    if(!XFUtil.isNull(mTabGroupAttachXPath)) {
      try {
        lTabGroupAttach = lContextUElem.extendedXPath1E(mTabGroupAttachXPath);
      }
      catch (ExTooFew | ExTooMany | ExActionFailed e) {
        throw new ExInternal("Failed to evaluate tabGroupAttach attribute", e);
      }
    }
    else {
      lTabGroupAttach = lContextUElem.attachDOM();
    }

    //Get OR create a tab group - we may need the create if this is the first attempt to access a tab group in a module call
    //(i.e. before it's been set out).
    TabGroupProvider lTabGroupProvider = pRequestContext.getModuleFacetProvider(TabGroupProvider.class);
    TabGroup lTabGroup = lTabGroupProvider.getOrCreateEmptyTabGroup(mTabGroupName, lTabGroupAttach);

    //Set the group to either the tab DOM or tab key specified
    if(!XFUtil.isNull(mTabDOMXPath)) {
      try {
        DOM lTabDOM = lContextUElem.extendedXPath1E(mTabDOMXPath);
        lTabGroup.selectTab(pRequestContext.getPersistenceContext(), lTabDOM);
      }
      catch (ExTooFew | ExTooMany | ExActionFailed e) {
        throw new ExInternal("Failed to evaluate tabDOM attribute", e);
      }
    }
    else {
      try {
        String lTabKey = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mTabKeyXPath);
        lTabGroup.selectTab(pRequestContext.getPersistenceContext(), lTabKey);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate tabKey attribute", e);
      }
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
      return new SwitchTabCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("switch-tab");
    }
  }
}
