package net.foxopen.fox.module.evaluatedattributeresult;

import net.foxopen.fox.dom.xpath.XPathResult;

/**
 * Store an evaluated PresentationAttribute along with its escaping requirement
 */
public class PresentationStringAttributeResult implements StringAttributeResult {
  private final String mResult;
  private final boolean mEscapingRequired;

  public PresentationStringAttributeResult(XPathResult pXPathResult) {
    if (pXPathResult != null) {
      mResult = pXPathResult.asString();
      mEscapingRequired = pXPathResult.isEscapingRequired();
    }
    else {
      mResult = null;
      mEscapingRequired = false;
    }
  }

  @Override
  public String getString() {
    return mResult;
  }

  @Override
  public boolean isEscapingRequired() {
    return mEscapingRequired;
  }
}
