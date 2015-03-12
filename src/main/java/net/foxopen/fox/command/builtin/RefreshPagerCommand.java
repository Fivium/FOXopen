package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.paging.DOMPager;
import net.foxopen.fox.dom.paging.Pager;
import net.foxopen.fox.dom.paging.PagerProvider;
import net.foxopen.fox.dom.paging.PagerSetup;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

/**
 * Command for programtically refreshing the status of a DOMPager. Typically a DOMPager's status is only change during
 * HTML generation. If the developer requires complex manipulation of the pager they can use this command to ensure it
 * is up to date before performing any dependent operations on it.
 */
public class RefreshPagerCommand
extends BuiltInCommand {

  private final PagerSetup mPagerSetup;
  private final String mMatchExpr;
  private final String mXPath;

  private RefreshPagerCommand(DOM pDOM) {
    super(pDOM);
    try {
      mPagerSetup = PagerSetup.fromDOMMarkupOrNull(pDOM, "pagination-definition", "page-size", "pagination-invoke-name");
    }
    catch (ExModule e) {
      throw new ExInternal("Failed to parse fm:reset-pager command", e);
    }
    mMatchExpr = pDOM.getAttrOrNull("match");
    mXPath = pDOM.getAttrOrNull("xpath");

    //Match should be specified for set-out pagers or xpath for for-each pagers - never both
    if(!XFUtil.isNull(mMatchExpr) && !XFUtil.isNull(mXPath)) {
      throw new ExInternal("match and xpath attributes are mutually exclusive");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    String lMatchId = Pager.getMatchIdOrNull(lContextUElem, mMatchExpr, lContextUElem.attachDOM());
    //Do a getOrCreate in case this is the first time in
    DOMPager lPager = pRequestContext.getModuleFacetProvider(PagerProvider.class).getOrCreateDOMPager(mPagerSetup.evalute(pRequestContext, lMatchId));

    //Resolve the DOM list, either by running the for-each XPath or getting the child elements of the set-out match node
    DOMList lPagerElements = new DOMList();
    try {
      if (!XFUtil.isNull(mMatchExpr)) {
        lPagerElements = lContextUElem.extendedXPath1E(mMatchExpr).getChildElements();
      }
      else {
        lPagerElements = lContextUElem.extendedXPathUL(lContextUElem.attachDOM(), mXPath);
      }
    }
    catch (ExTooFew e) {
      //Ignore - 0 is fine
    }
    catch (ExCardinality | ExActionFailed e) {
      throw new ExInternal("Failed to resolve pager elements", e);
    }

    lPager.refreshPager(pRequestContext, lPagerElements);

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
      return new RefreshPagerCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("refresh-pager");
    }
  }
}
