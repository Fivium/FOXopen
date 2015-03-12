package net.foxopen.fox.module.tabs;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.module.DisplayOrderSortable;
import net.foxopen.fox.module.fieldset.action.SwitchTabAction;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;

/**
 * Evaluated data about an individual tab in a TabGroup.
 */
public class EvaluatedTabInfo
implements DisplayOrderSortable {

  private final TabInfo mTabInfo;
  private final String mTabKey;
  private final boolean mEnabled;
  private final DOM mTabDOM; //DOM element the :{tab} context points to in the HTML loop (can be an unconnected element for number loops)
  private final DOM mRelativeDOM; //DOM element the :{action} context points to in a tab action (either tab DOM or tab group attach point)
  private final String mDisplayOrder;
  private final boolean mDefault;

  EvaluatedTabInfo(TabInfo pTabInfo, String pTabKey, boolean pEnabled, DOM pTabDOM, DOM pRelativeDOM, String pDisplayOrder, boolean pDefault) {
    mTabInfo = pTabInfo;
    mTabKey = pTabKey;
    mEnabled = pEnabled;
    mTabDOM = pTabDOM;
    mRelativeDOM = pRelativeDOM;
    mDisplayOrder = pDisplayOrder;
    mDefault = pDefault;
  }

  public String getTabKey() {
    return mTabKey;
  }

  public boolean isEnabled() {
    return mEnabled;
  }

  public boolean isDefault() {
    return mDefault;
  }

  public DOM getTabDOM() {
    return mTabDOM;
  }

  public PresentationNode getPromptPresentationNode() {
    return mTabInfo.mPromptPresentationNode;
  }

  public PresentationNode getContentPresentationNode() {
    return mTabInfo.mContentPresentationNode;
  }

  public SwitchTabAction createSelectTabAction(String pTabGroupKey) {
    return new SwitchTabAction(pTabGroupKey, mTabKey, mRelativeDOM.getFoxId(), mTabInfo.mPreTabActionName, mTabInfo.mPostTabActionName);
  }

  public void setTabContextLabel(ContextUElem pContextUElem) {
    pContextUElem.setUElem(mTabInfo.mTabContextName, ContextualityLevel.LOCALISED, mTabDOM);
  }

  @Override
  public String getDisplayBeforeAttribute() {
    return null;
  }

  @Override
  public String getDisplayAfterAttribute() {
    return null;
  }

  @Override
  public String getDisplayOrder() {
    return mDisplayOrder;
  }

  @Override
  public String getName() {
    return mTabKey;
  }
}
