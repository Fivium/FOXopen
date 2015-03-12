package net.foxopen.fox.module.evaluatedattributeresult;

import net.foxopen.fox.module.evaluatedattributeresult.EvaluatedAttributeResult;

public class BooleanAttributeResult implements EvaluatedAttributeResult {
  private final Boolean mBoolean;

  public BooleanAttributeResult(Boolean pBoolean) {
    mBoolean = pBoolean;
  }

  public Boolean getBoolean() {
    return mBoolean;
  }
}
