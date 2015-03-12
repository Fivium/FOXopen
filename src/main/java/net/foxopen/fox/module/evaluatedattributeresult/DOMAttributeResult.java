package net.foxopen.fox.module.evaluatedattributeresult;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.evaluatedattributeresult.EvaluatedAttributeResult;

public class DOMAttributeResult implements EvaluatedAttributeResult {
  private final DOM mDOM;

  public DOMAttributeResult(DOM pDOM) {
    mDOM = pDOM;
  }

  public DOM getDOM() {
    return mDOM;
  }
}
