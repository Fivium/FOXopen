/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
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
