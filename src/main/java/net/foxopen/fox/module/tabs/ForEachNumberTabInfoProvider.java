package net.foxopen.fox.module.tabs;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.util.ForEachIterator;
import net.foxopen.fox.command.util.ForEachIterator.IterationExecutable;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides tabs using a number based loop with "from" and "to" attributes.  The :{tab} context is an element containing
 * the current loop index.
 */
public class ForEachNumberTabInfoProvider
extends TabInfoProvider {

  private final String mFromXPath;
  private final String mToXPath;

  public ForEachNumberTabInfoProvider(DOM pMarkupDOM, TabInfo pTabMarkup)
  throws ExModule {
    super(pTabMarkup);

    mFromXPath = pMarkupDOM.getAttrOrNull("from");
    if(XFUtil.isNull(mFromXPath)) {
      throw new ExModule("'from' attribute cannot be null in fm:tab-for-each-number element");
    }

    mToXPath = pMarkupDOM.getAttrOrNull("to");
    if(XFUtil.isNull(mToXPath)) {
      throw new ExModule("'to' attribute cannot be null in fm:tab-for-each-number element");
    }
  }

  @Override
  public List<EvaluatedTabInfo> evaluate(final DOM pRelativeDOM, final ContextUElem pContextUElem, final boolean pEvaluateDefaultAttr) {
    final List<EvaluatedTabInfo> lTabInfoList = new ArrayList<>();

    double lFrom;
    try {
      lFrom = Double.parseDouble(pContextUElem.extendedStringOrXPathString(pRelativeDOM, mFromXPath));
    }
    catch (ExActionFailed | NumberFormatException e) {
      throw new ExInternal("Failed to evaluate 'from' attribute for fm:tab-for-each-number", e);
    }

    double lTo;
    try {
      lTo = Double.parseDouble(pContextUElem.extendedStringOrXPathString(pRelativeDOM, mToXPath));
    }
    catch (ExActionFailed | NumberFormatException e) {
      throw new ExInternal("Failed to evaluate 'to' attribute for fm:tab-for-each-number", e);
    }

    ForEachIterator lIterator = new ForEachIterator(false, null, "loopStatus", lFrom, lTo, 1d);
    lIterator.doForEach(pContextUElem, null, new IterationExecutable() {
      public boolean execute(DOM pOptionalCurrentItem, ForEachIterator.Status pIteratorStatus) {
        //Create a dummy "tab" element so :{tab}/text() is the loop index value for tab XPaths
        String lCurrentStep = String.valueOf((int) pIteratorStatus.getCurrentStep());
        DOM lLoopStepDOM = DOM.createDocument("tab").setText(lCurrentStep);
        //Note: ContextUElem is already localised by ForEachIterator
        pContextUElem.setUElem(getTabInfo().mTabContextName, ContextualityLevel.LOCALISED, lLoopStepDOM);

        //The :{tab} DOM should be the loop index when evaluating the tab contents, but tab actions should be relative to the tab group attach
        lTabInfoList.add(getTabInfo().evaluate(lLoopStepDOM, lCurrentStep, pRelativeDOM, pContextUElem, pEvaluateDefaultAttr));
        return true;
      }
    });

    return lTabInfoList;
  }
}
