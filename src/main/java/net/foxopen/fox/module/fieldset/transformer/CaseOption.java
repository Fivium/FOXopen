package net.foxopen.fox.module.fieldset.transformer;

import net.foxopen.fox.XFUtil;

public enum CaseOption {  
  UPPER,
  LOWER,
  INITCAP;  
  
  public static CaseOption fromExternalString(String pExternalString) {
    if(XFUtil.isNull(pExternalString)) {
      return null;
    }
    else {
      return valueOf(pExternalString.toUpperCase());
    }    
  }
}
