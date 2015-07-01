package net.foxopen.fox.module.tabs;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.util.ForEachIterator;
import net.foxopen.fox.command.util.ForEachIterator.IterationExecutable;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides tabs based on a DOM loop. The :{tab} context can be used to refer to the loop item.
 */
public class ForEachDOMTabInfoProvider
extends TabInfoProvider {

  private final String mForEachXPath;

  public ForEachDOMTabInfoProvider(DOM pMarkupDOM, TabInfo pTabMarkup)
  throws ExModule {
    super(pTabMarkup);

    mForEachXPath = pMarkupDOM.getAttrOrNull("xpath");
    if(XFUtil.isNull(mForEachXPath)) {
      throw new ExModule("xpath attribute cannot be null for fm:tab-for-each-dom element");
    }
  }

  @Override
  public List<EvaluatedTabInfo> evaluate(final DOM pRelativeDOM, final ContextUElem pContextUElem, final boolean pEvaluateDefaultAttr) {
    final List<EvaluatedTabInfo> lTabInfoList = new ArrayList<>();

    final DOMList lXPathMatchedItems;
    try {
      lXPathMatchedItems = pContextUElem.extendedXPathUL(pRelativeDOM, mForEachXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to execute tab-for-each XPath", e);
    }

    ForEachIterator lIterator = new ForEachIterator(true, getTabInfo().mTabContextName, "loopStatus", null, null, null);
    lIterator.doForEach(pContextUElem, lXPathMatchedItems, new IterationExecutable() {
      public boolean execute(DOM pOptionalCurrentItem, ForEachIterator.Status pIteratorStatus) {
        //The tab DOM is also the relative DOM for this type of loop (i.e. :{action} for a tab change action should be the same as :{tab}
        lTabInfoList.add(getTabInfo().evaluate(pOptionalCurrentItem, pOptionalCurrentItem.getFoxId(), pOptionalCurrentItem, pContextUElem, pEvaluateDefaultAttr));
        return true;
      }
    });

    return lTabInfoList;
  }
}
