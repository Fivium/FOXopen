package net.foxopen.fox.xhtml;

import java.text.DecimalFormat;

@Deprecated
public class InputMaskCommas
extends InputMask {

  private final String mMaskPattern = "#,###";

  public InputMaskCommas() {
    mFieldSetMaskName = "commas";
  }

  public String applyMask(String pTextValue) {
    int lIndex = pTextValue.lastIndexOf(".");
    StringBuffer lMaskBuffer = new StringBuffer(mMaskPattern);
    if(lIndex!=-1) {
      lMaskBuffer.append(".");
      for(int i=0; i<pTextValue.length() - lIndex; i++) {
        lMaskBuffer.append("#");
      }
    }

    try {
      double lNumber = Double.parseDouble(pTextValue);
      DecimalFormat lFormatter = new DecimalFormat(lMaskBuffer.toString());
      return lFormatter.format(lNumber);
    } catch (NumberFormatException e) {
      return pTextValue;
    }
  }

  public String removeMask(String pMaskedValue) {
    return pMaskedValue.replaceAll(",","");
  }

}
