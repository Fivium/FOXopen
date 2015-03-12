package net.foxopen.fox.module.evaluatedattributeresult;

import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.module.evaluatedattributeresult.EvaluatedAttributeResult;

public class DOMListAttributeResult implements EvaluatedAttributeResult {
  private final DOMList mDOMList;

  public DOMListAttributeResult(DOMList pDOMList) {
    mDOMList = pDOMList;
  }

  public DOMList getDOMList() {
    return mDOMList;
  }
}
