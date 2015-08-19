package net.foxopen.fox.dom.paging.style;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.Pager;
import net.foxopen.fox.dom.paging.TopNDatabasePager;
import net.foxopen.fox.ex.ExModule;

/**
 * Note: the behaviours specified in this class are prototypes and may need changing based on feedback.
 */
public class PageControlStyle {

  private static enum StyleType {
    SIMPLE,
    STANDARD,
    FULL,
    CREEPER;
  }

  private static class ButtonConfig {
    Boolean mEnabled;
    String mLabel;

    ButtonConfig(Boolean pEnabled, String pLabel) {
      mEnabled = pEnabled;
      mLabel = pLabel;
    }
  }

  private ButtonConfig mFirstPage = new ButtonConfig(true, "&laquo;");
  private ButtonConfig mPreviousPage = new ButtonConfig(true, "&lsaquo;");
  private ButtonConfig mNextPage = new ButtonConfig(true, "&rsaquo;");
  private ButtonConfig mLastPage = new ButtonConfig(true, "&raquo;");

  private boolean mShowPageNavigation = true;
  private boolean mShowPageCount = true;

  private int mPageDisplayScope = 0;

  private boolean mHideForSinglePage = true;

  public static PageControlStyle createFromDOM(DOM pDefinitionDOM) throws ExModule {
    //Establish a default

    String lStyleAttr = pDefinitionDOM.getAttr("style");
    if(XFUtil.isNull(lStyleAttr)) {
      throw new ExModule("Style attribute must be defined");
    }

    StyleType lStyleType = StyleType.valueOf(lStyleAttr.toUpperCase());

    PageControlStyle lStyleDefn = getDefault(lStyleType);

    overloadDefaultConfig(lStyleDefn.mFirstPage, pDefinitionDOM, "fm:first-page");
    overloadDefaultConfig(lStyleDefn.mPreviousPage, pDefinitionDOM, "fm:previous-page");
    overloadDefaultConfig(lStyleDefn.mNextPage, pDefinitionDOM, "fm:next-page");
    overloadDefaultConfig(lStyleDefn.mLastPage, pDefinitionDOM, "fm:last-page");

    String lPageCountEnabledAttr = optionalElementWithAttribute(pDefinitionDOM, "fm:page-count", "enabled");

    if(lPageCountEnabledAttr != null) {
      lStyleDefn.mShowPageCount = Boolean.valueOf(lPageCountEnabledAttr);
    }

    String lCreeperScopeAttr = optionalElementWithAttribute(pDefinitionDOM, "fm:creeper", "page-scope");
    if(lCreeperScopeAttr != null) {
      lStyleDefn.mPageDisplayScope = Integer.parseInt(lCreeperScopeAttr);
    }

    String lHideForSinglePageAttr =  optionalElementWithAttribute(pDefinitionDOM, "fm:single-page", "hide-controls");
    if(lHideForSinglePageAttr != null) {
      lStyleDefn.mHideForSinglePage = Boolean.valueOf(lHideForSinglePageAttr);
    }

    return lStyleDefn;
  }

  private static void overloadDefaultConfig(ButtonConfig pTarget, DOM pDefinitionDOM, String pElementName) {
    ButtonConfig lDefinition = parseButtonConfig(pDefinitionDOM.get1EOrNull(pElementName));
    pTarget.mEnabled = XFUtil.nvl(lDefinition.mEnabled, pTarget.mEnabled);
    pTarget.mLabel = XFUtil.nvl(lDefinition.mLabel, pTarget.mLabel);
  }

  private static ButtonConfig parseButtonConfig(DOM pDOM) {
    if(pDOM != null) {
      String lEnabledAttr = pDOM.getAttrOrNull("enabled");
      Boolean lEnabled = null;
      if(lEnabledAttr != null) {
        lEnabled = Boolean.valueOf(lEnabledAttr);
      }
      String lLabel = pDOM.getAttrOrNull("label");
      return new ButtonConfig(lEnabled, lLabel);
    }
    else {
      return new ButtonConfig(null, null);
    }
  }

  private static String optionalElementWithAttribute(DOM pDOM, String pElementName, String pAttributeName) {
    if(pDOM != null && pDOM.get1EOrNull(pElementName) != null) {
      return pDOM.get1EOrNull(pElementName).getAttrOrNull(pAttributeName);
    }
    else {
      return null;
    }
  }

  public static PageControlStyle getDefault(Pager pPager) {
    if(pPager instanceof TopNDatabasePager) {
      return getDefault(StyleType.CREEPER);
    }
    else {
      return getDefault(StyleType.STANDARD);
    }
  }

  private PageControlStyle() {
  }

  private static PageControlStyle getDefault(StyleType pStyleType) {

    PageControlStyle lNewStyle = new PageControlStyle();
    switch(pStyleType) {
      case SIMPLE:
        lNewStyle.mFirstPage.mEnabled = false;
        lNewStyle.mLastPage.mEnabled = false;
        lNewStyle.mShowPageNavigation = false;
        lNewStyle.mShowPageCount = false;
        break;
      case STANDARD:
        lNewStyle.mShowPageNavigation = false;
        break;
      case CREEPER:
        lNewStyle.mFirstPage.mEnabled = false;
        lNewStyle.mLastPage.mEnabled = false;
        lNewStyle.mPageDisplayScope = 2;
    }

    return lNewStyle;
  }

  public boolean isFirstPageEnabled() {
    return mFirstPage.mEnabled;
  }

  public boolean isPreviousPageEnabled() {
    return mPreviousPage.mEnabled;
  }

  public boolean isNextPageEnabled() {
    return mNextPage.mEnabled;
  }

  public boolean isLastPageEnabled() {
    return mLastPage.mEnabled;
  }

  public String getFirstPageLabel() {
    return mFirstPage.mLabel;
  }

  public String getPreviousPageLabel() {
    return mPreviousPage.mLabel;
  }

  public String getNextPageLabel() {
    return mNextPage.mLabel;
  }

  public String getLastPageLabel() {
    return mLastPage.mLabel;
  }

  public boolean isShowPageNavigation() {
    return mShowPageNavigation;
  }

  public boolean isShowPageCount() {
    return mShowPageCount;
  }

  public int getPageDisplayScope() {
    return mPageDisplayScope;
  }

  public boolean isHideForSinglePage() {
    return mHideForSinglePage;
  }
}
