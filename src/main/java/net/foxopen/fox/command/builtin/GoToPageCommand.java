package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.Pager;
import net.foxopen.fox.dom.paging.PagerProvider;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

public class GoToPageCommand
extends BuiltInCommand {

  private final String mMatchExpr;
  private final String mInvokeName;
  private final String mPageNumberExpr;
  private final String mNewPageSizeExpr;
  private final String mPageOutOfBoundsActionName;
  /* Temporary workaround to allow developers to manually override the pager's row count if they have manipulated the pager cache - to be removed in a future release */
  private final String mUnsupportedNewRowCountExpression;

  private GoToPageCommand(DOM pParseUElem) {
    super(pParseUElem);
    mMatchExpr = pParseUElem.getAttrOrNull("match");
    mInvokeName = pParseUElem.getAttrOrNull("pagination-invoke-name");
    mPageNumberExpr = pParseUElem.getAttrOrNull("number");
    mNewPageSizeExpr = pParseUElem.getAttrOrNull("new-page-size");

    mUnsupportedNewRowCountExpression = pParseUElem.getAttrOrNull("new-row-count-unsupported");

    mPageOutOfBoundsActionName = pParseUElem.getAttrOrNull("out-of-bounds-action");

    if(XFUtil.isNull(mPageNumberExpr) && XFUtil.isNull(mNewPageSizeExpr) && XFUtil.isNull(mUnsupportedNewRowCountExpression)) {
      throw new ExInternal("go-to-page must specify at least one of 'number' or 'new-page-size' attributes");
    }
  }

  @Override
  public void validate(Mod pModule) {
    if (!XFUtil.isNull(mPageOutOfBoundsActionName)) {
      if (pModule.badActionName(mPageOutOfBoundsActionName)) {
        pModule.addBulkModuleErrorMessage("\nBad Action Name in <go-to-page> command: "+mPageOutOfBoundsActionName);
      }
    }
  }

  private static final int xpathInt(ContextUElem pContextUElem, String pXPath) {
    try {
      return pContextUElem.extendedConstantOrXPathResult(pContextUElem.attachDOM(), pXPath).asNumber().intValue();
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate go-to-page command numeric expression", e);
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    Pager lPager;
    if(!XFUtil.isNull(mMatchExpr)) {
      //If a match attribute was specified, this should be resolved to a node to determine the pager's match id
      DOM lMatchNode;
      try {
        lMatchNode = lContextUElem.extendedXPath1E(mMatchExpr);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate go-to-page command match expression", e);
      }
      catch (ExCardinality e) {
        throw new ExInternal("go-to-page command match expression failed to match exactly 1 element", e);
      }

      String lMatchFoxId = lMatchNode.getFoxId();
      if (XFUtil.isNull(lMatchFoxId)) {
        throw new ExInternal("Failed to determine foxid of match element.");
      }

      //Resolve database pager using match id
      lPager = pRequestContext.getModuleFacetProvider(PagerProvider.class).getPagerOrNull(mInvokeName, lMatchFoxId);
    }
    else {
      //Resolve DOM pager without a match id
      lPager = pRequestContext.getModuleFacetProvider(PagerProvider.class).getPagerOrNull(mInvokeName, null);
    }

    if (lPager != null) {
      //Allow developer to set the row count if they've manipulated the paged data in an unsupported way
      //TODO PN - remove this when no longer required
      if(!XFUtil.isNull(mUnsupportedNewRowCountExpression)) {
        lPager.setRowCount(xpathInt(lContextUElem, mUnsupportedNewRowCountExpression));
      }

      boolean lSetNewSize = !XFUtil.isNull(mNewPageSizeExpr);
      int lPageSize = lPager.getPageSize();
      if(lSetNewSize) {
        lPageSize = xpathInt(lContextUElem, mNewPageSizeExpr);
      }

      boolean lGoToPage = !XFUtil.isNull(mPageNumberExpr);
      int lDesiredPageNumber = lPager.getCurrentPage();
      if (lGoToPage) {
        lDesiredPageNumber = xpathInt(lContextUElem, mPageNumberExpr);
      }

      // If the user has explicitly asked to go to a new page, we must check the new page number is valid (this may also depend on the new page size)
      if (lGoToPage && !lPager.validatePageNumber(lDesiredPageNumber, lPageSize)) {
        if (!XFUtil.isNull(mPageOutOfBoundsActionName)) {
          //Run the out of bounds action if defined - no change will be made to the pager
          XDoCommandList lOutOfBoundsAction = pRequestContext.resolveActionName(mPageOutOfBoundsActionName);
          pRequestContext.createIsolatedCommandRunner(true).runCommandsAndComplete(pRequestContext, lOutOfBoundsAction);
        }
        else {
          throw new ExInternal("Page "+lDesiredPageNumber+" is outside of this pager's page boundaries (1 - "+lPager.getPageCount()+") for pager " + lPager.getPagerKey());
        }
      }
      else {
        // New page number is valid or user didn't ask to change page
        if(lSetNewSize) {
          //Change the pager size if user asked for it
          //This also does a go to page - only specify a page if the user asked, leave null for the default behaviour
          lPager.changePageSize(pRequestContext, lPageSize, lGoToPage ? lDesiredPageNumber : null);
        }
        else if(lGoToPage) {
          //We don't need to change the size so just go to a new page
          lPager.goToPage(pRequestContext, lDesiredPageNumber);
        }
      }
    }
    else {
      throw new ExInternal("Failed to obtain pager handle with invoke-name='" + mInvokeName + "' and match='" + mMatchExpr + "'");
    }

    return XDoControlFlowContinue.instance();
  }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new GoToPageCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("go-to-page");
    }
  }
}
