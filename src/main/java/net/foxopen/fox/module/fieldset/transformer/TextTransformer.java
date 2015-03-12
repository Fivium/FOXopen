package net.foxopen.fox.module.fieldset.transformer;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;

public class TextTransformer
extends DefaultTransformer {

  private final CaseOption mCaseOption;

  TextTransformer(CaseOption pCaseOption, String pInputMaskName) {
    super(pInputMaskName);
    mCaseOption = pCaseOption;
  }

  public String applyOutboundTransform(DOM pSourceElement) {
    return applyCase(super.applyOutboundTransform(pSourceElement));
  }

  public String applyInboundTransform(String pPostedValue) {
    return applyCase(super.applyInboundTransform(pPostedValue));
  }

  private String applyCase(String pValue) {
    if(mCaseOption == null) {
      return pValue;
    }
    else {
      switch(mCaseOption) {
        case UPPER:
          return pValue.toUpperCase();
        case LOWER:
          return pValue.toLowerCase();
        case INITCAP:
          return XFUtil.initCap(pValue);
        default:
          return pValue; //Compiler
      }
    }
  }
}
