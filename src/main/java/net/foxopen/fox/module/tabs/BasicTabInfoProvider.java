package net.foxopen.fox.module.tabs;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.DOM;

import java.util.Collections;
import java.util.List;

/**
 * Static tab info defined inline in the module.
 */
public class BasicTabInfoProvider
extends TabInfoProvider {

  BasicTabInfoProvider(TabInfo pTabMarkup) {
    super(pTabMarkup);
  }

  @Override
  public List<EvaluatedTabInfo> evaluate(DOM pRelativeDOM, ContextUElem pContextUElem, boolean pEvaluateDefaultAttr) {
    return Collections.singletonList(getTabInfo().evaluate(pRelativeDOM, false, pRelativeDOM, pContextUElem, pEvaluateDefaultAttr));
  }
}
