package net.foxopen.fox.module.fieldset.transformer;

import net.foxopen.fox.dom.DOM;

public class DecimalTransformer
extends DefaultTransformer {

  DecimalTransformer(String pInputMaskName) {
    super(pInputMaskName);
  }

  public String applyOutboundTransform(DOM pSourceElement) {
    String lValue = getDOMValue(pSourceElement);
    lValue = applyDecimalChanges(lValue);
    return applyInputMask(lValue);
  }

  public String applyInboundTransform(String pPostedValue) {
    String lValue = processPostedValue(pPostedValue);
    lValue = removeInputMask(lValue);
    return applyDecimalChanges(lValue);
  }
  
  private String applyDecimalChanges(String pValue) {   
    // Parsing and altering number
    try { 
      // If invalid number, jump to exception handler
      double lDummy = Double.parseDouble(pValue);
      
      // Really cheesy check to ensure that decimals < 1 start with a zero
      if (pValue.indexOf(".") == 0) {
        pValue = "0" + pValue;
      }
      
      return pValue;
    }
    catch (NumberFormatException ex) {
      // In the event of any error, return original value
      return pValue;
    }
  }
}
