package net.foxopen.fox.dom.paging;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.track.Track;

/**
 * Pager for paging through a DOMList identified by an XPath, typically in a for-each or set-out command. The entire
 * DOM is stored in memory and this pager is used to save page state and trim the DOM so the correct view of it can
 * be displayed. No DOM changes should be made by this pager.<br/><br/>
 *
 * Unlike DatabasePagers which are constructed when a query is run during action processing, DOMPagers are typically created
 * ad-hoc during output generation. Therefore at action time the state of this pager will not necessarily reflect the
 * latest DOM contents (for instance, if additional paged elements have been added, this pager will not know about them
 * until HTML generation). Any code which relies on this pager being up to date will therefore receive incorrect results.
 * FOX developers can mitigate against this using the fm:refresh-pager command.
 */
public class DOMPager
extends Pager {

  public DOMPager(String pPagerKey, String pModuleCallId, EvaluatedPagerSetup pEvalPagerSetup) {
    super(pPagerKey, pModuleCallId, pEvalPagerSetup);
  }

  /**
   * Filters the contents of the given list into a new list, based on the pager's current page size and row count.
   * The original list is not modified.
   * @param pRequestContext
   * @param pFullDOMList
   * @return
   */
  public DOMList trimDOMListForCurrentPage(ActionRequestContext pRequestContext, DOMList pFullDOMList) {

    int lOldRowCount = getRowCount();
    int lNewRowCount = pFullDOMList.getLength();
    setRowCount(lNewRowCount);

    DOMList lTrimmedList = new DOMList(getPageSize());
    for(int i = getCurrentPageStartRowNum() - 1; i < Math.min(getCurrentPageEndRowNum(), pFullDOMList.size()); i++) {
      lTrimmedList.add(pFullDOMList.get(i));
    }

    if(lOldRowCount != lNewRowCount) {
      Track.info("PersistDOMPager", "Marking DOM pager as requiring update as row counts have changed");
      pRequestContext.getPersistenceContext().requiresPersisting(this, PersistenceMethod.UPDATE);
    }

    return lTrimmedList;
  }

  /**
   * Updates this pager so the correct row count is used. This may cause the page to change if the current page is over
   * the new maximum page boundary.
   * @param pRequestContext
   * @param pPagedDOMList Up-to-date DOMList which this pager is paging.
   */
  public void refreshPager(ActionRequestContext pRequestContext, DOMList pPagedDOMList) {
    setRowCount(pPagedDOMList.getLength());
    setCurrentPage(getClosestActualPageNum());
    pRequestContext.getPersistenceContext().requiresPersisting(this, PersistenceMethod.UPDATE);
  }

  @Override
  protected void goToPageInternal(ActionRequestContext pRequestContext, int pPageNum, DOM pMatchDOM) {
  }

  @Override
  protected void prePageDefaultAction(DOM pMatchDOM) {
  }

  @Override
  protected void postPageDefaultAction(DOM pMatchDOM) {
  }

  @Override
  protected void resetPagerInternal(ActionRequestContext pRequestContext, DOM pMatchDOM) {
  }

}
