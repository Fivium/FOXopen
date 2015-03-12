package net.foxopen.fox.thread.persistence.xstream;


import com.thoughtworks.xstream.converters.SingleValueConverter;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.StandardAuthenticationContext;

class StandardAuthenticationContextConverter
implements SingleValueConverter {

  @Override
  public boolean canConvert(Class pType) {
    return pType == StandardAuthenticationContext.class;
  }

  @Override
  public Object fromString(String pStr) {
    return new StandardAuthenticationContext("".equals(pStr) ? null : pStr);
  }

  @Override
  public String toString(Object pObj) {
    StandardAuthenticationContext lSAC = (StandardAuthenticationContext) pObj;
    return XFUtil.nvl(lSAC.getSessionId(), "");
  }
}

