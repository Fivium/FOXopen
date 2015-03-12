package net.foxopen.fox.download;

import net.foxopen.fox.XFUtil;

public enum DownloadMode {
  ATTACHMENT,
  INLINE;
  
  public String getHttpContentDispositionHeader() {
    return this.toString().toLowerCase();
  }
  
  public String getHttpParameterValue() {
    return this.toString().toLowerCase();
  }
  
  public static DownloadMode fromParameterString(String pParamString) {
    if(XFUtil.isNull(pParamString)) {
      return ATTACHMENT;
    }
    else {
      return valueOf(pParamString.toUpperCase());
    }
  }
}
