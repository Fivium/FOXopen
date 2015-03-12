package net.foxopen.fox.module.fieldset.transformer;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.util.StringFormatter;

public class DateTransformer
implements FieldTransformer {
    
  private final String mExternalFormatMask;
  private final String mInternalFormatMask;

  DateTransformer(String pExternalFormatMask, String pInternalFormatMask) {
    mExternalFormatMask = pExternalFormatMask;
    mInternalFormatMask = pInternalFormatMask;
  }

  @Override
  public String applyOutboundTransform(DOM pSourceElement) {  
    return StringFormatter.formatDateStringSafe(pSourceElement.value(), mInternalFormatMask, mExternalFormatMask);    
  }


  @Override
  public String applyInboundTransform(String pPostedValue) {
    String lDOMValue = pPostedValue.trim();
    return StringFormatter.formatDateStringSafe(lDOMValue, mExternalFormatMask, mInternalFormatMask);    
  }
}
