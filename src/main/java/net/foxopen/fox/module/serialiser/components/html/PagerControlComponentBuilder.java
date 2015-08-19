package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.dom.paging.Pager;
import net.foxopen.fox.dom.paging.style.PageControlStyle;
import net.foxopen.fox.module.fieldset.action.InternalActionContext;
import net.foxopen.fox.module.fieldset.action.PageControlAction;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPagerControlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;


public class PagerControlComponentBuilder
extends ComponentBuilder<HTMLSerialiser, EvaluatedPagerControlPresentationNode> {

  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPagerControlPresentationNode> INSTANCE = new PagerControlComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPagerControlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private PagerControlComponentBuilder() {
  }

  private static final String linkString(HTMLSerialiser pSerialiser, InternalActionContext lActionContext, int pPageNo, String pPrompt) {
    return "<a href=\"javascript:" + pSerialiser.getInternalActionSubmitString(lActionContext, PageControlAction.getParamMapForPageNumber(pPageNo)) + "\">" + pPrompt + "</a>";
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPagerControlPresentationNode pEvalPagerControlNode) {

    Pager lPager = pEvalPagerControlNode.getPager();

    //No page controls for unintialised/empty pagers, or pagers with 1 page where single page hiding is enabled
    if(lPager.getRowCount() == 0 || (pEvalPagerControlNode.getPageControlStyle().isHideForSinglePage() && lPager.getPageCount() <= 1)) {
      return;
    }

    PageControlStyle lPageControlStyle = pEvalPagerControlNode.getPageControlStyle();

    //Register the go to page action with the fieldset
    PageControlAction lInternalAction = new PageControlAction(lPager.getPagerKey());
    InternalActionContext lActionContext = pSerialisationContext.getFieldSet().addInternalAction(lInternalAction);

    pSerialiser.append("<ul class=\"pager\">");

    if(lPageControlStyle.isFirstPageEnabled()) {
      if(lPager.getCurrentPage() > 1) {
        pSerialiser.append("<li class=\"page-skip first\">" + linkString(pSerialiser, lActionContext, 1, lPageControlStyle.getFirstPageLabel() ) + "</li>");
      }
      else {
        pSerialiser.append("<li class=\"page-skip first disabled\">" + lPageControlStyle.getFirstPageLabel() + "</li>");
      }
    }

    if(lPageControlStyle.isPreviousPageEnabled()) {
      if(lPager.getCurrentPage() > 1) {
        pSerialiser.append("<li class=\"page-skip previous\">" + linkString(pSerialiser, lActionContext, lPager.getCurrentPage() - 1, lPageControlStyle.getPreviousPageLabel()) + "</li>");
      }
      else {
        pSerialiser.append("<li class=\"page-skip previous disabled\">" + lPageControlStyle.getPreviousPageLabel() + "</li>");
      }
    }

    if(lPageControlStyle.isShowPageNavigation()) {

      int lPageFrom = 1;
      int lPageTo = lPager.getPageCount();
      if(lPageControlStyle.getPageDisplayScope() > 0) {
        lPageFrom = Math.max(1, lPager.getCurrentPage() - lPageControlStyle.getPageDisplayScope());
        lPageTo = Math.min(lPager.getPageCount(), lPager.getCurrentPage() + lPageControlStyle.getPageDisplayScope());
      }

      for(int lPageNo = lPageFrom; lPageNo <= lPageTo; lPageNo++) {
        String lPageText;
        if(lPageNo != lPager.getCurrentPage()) {
          lPageText = "<li class=\"page-number\">" + linkString(pSerialiser, lActionContext, lPageNo, Integer.toString(lPageNo)) + "</li>";
        }
        else {
          lPageText = "<li class=\"page-number current-page\">" + Integer.toString(lPageNo) + "</li>";
        }

        pSerialiser.append(lPageText);
      }
    }
    else {
      pSerialiser.append("<li class=\"current-page-text\">Page " + lPager.getCurrentPage());
      if(lPageControlStyle.isShowPageCount()) {
        pSerialiser.append("<span class\"page-count\"> of " + lPager.getPageCount() + "</span>");
      }
      pSerialiser.append("</li>");
    }

    if(lPageControlStyle.isNextPageEnabled()) {
      if(lPager.getCurrentPage() < lPager.getPageCount()) {
        pSerialiser.append("<li class=\"page-skip next\">" + linkString(pSerialiser, lActionContext, lPager.getCurrentPage() + 1, lPageControlStyle.getNextPageLabel() ) + "</li>");
      }
      else {
        pSerialiser.append("<li class=\"page-skip next disabled\">" + lPageControlStyle.getNextPageLabel() + "</li>");
      }
    }

    if(lPageControlStyle.isLastPageEnabled()) {
      if(lPager.getCurrentPage() < lPager.getPageCount()) {
        pSerialiser.append("<li class=\"page-skip last\">" + linkString(pSerialiser, lActionContext, lPager.getPageCount(), lPageControlStyle.getLastPageLabel() ) + "</li>");
      }
      else {
        pSerialiser.append("<li class=\"page-skip last disabled\">" + lPageControlStyle.getLastPageLabel() + "</li>");
      }
    }

    pSerialiser.append("</ul>");
  }
}
