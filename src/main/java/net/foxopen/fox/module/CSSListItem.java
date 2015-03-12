package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;

public class CSSListItem {
  private final String mStyleSheetPath;
  private final String mType;
  private final String mBrowserCondition;
  private final Integer mOrder;

  public CSSListItem(String pStyleSheetPath, String pType, String pBrowserCondition, String pOrder) {
    mStyleSheetPath = pStyleSheetPath;
    mType = pType;
    mBrowserCondition = pBrowserCondition;
    if (!XFUtil.isNull(pOrder)) {
      mOrder = Integer.valueOf(pOrder);
    }
    else {
      mOrder = Integer.MAX_VALUE;
    }
  }

  public String getStyleSheetPath() {
    return mStyleSheetPath;
  }

  public String getType() {
    return mType;
  }

  public String getBrowserCondition() {
    return mBrowserCondition;
  }

  public Integer getOrder() {
    return mOrder;
  }
}
