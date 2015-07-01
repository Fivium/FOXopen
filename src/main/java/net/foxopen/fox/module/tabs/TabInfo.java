package net.foxopen.fox.module.tabs;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;

/**
 * Data class containing unevaluated attributes based on module tab markup. A TabInfoProvider will evaluate its TabInfo
 * one or more times to create EvaluatedTabInfos (i.e. the provider may be loop-based).
 */
class TabInfo {

  final String mTabKeyXPath;
  final String mTabEnabledXPath;
  final String mTabVisibleXPath;
  final String mTabContextName;
  final String mPreTabActionName;
  final String mPostTabActionName;
  final String mDisplayOrderXPath;
  final String mDefaultXPath;
  final PresentationNode mPromptPresentationNode;
  final PresentationNode mContentPresentationNode;

  TabInfo(String pTabKeyXPath, String pTabEnabledXPath, String pTabVisibleXPath, String pTabContextName, String pPreTabActionName, String pPostTabActionName,
          String pDisplayOrderXPath, String pDefaultXPath, PresentationNode pPromptPresentationNode, PresentationNode pContentPresentationNode) {
    mTabKeyXPath = pTabKeyXPath;
    mTabEnabledXPath = pTabEnabledXPath;
    mTabVisibleXPath = pTabVisibleXPath;
    mTabContextName = pTabContextName;
    mPreTabActionName = pPreTabActionName;
    mPostTabActionName = pPostTabActionName;
    mDisplayOrderXPath = pDisplayOrderXPath;
    mDefaultXPath = pDefaultXPath;
    mPromptPresentationNode = pPromptPresentationNode;
    mContentPresentationNode = pContentPresentationNode;
  }

  /**
   * Evaluates the XPaths on this TabInfo, relative to the given DOM, and returns the evaluated form.
   * @param pTabDOM The DOM of the tab from a for-each loop, or the tab group attach DOM, depending on the source of this
   * TabInfo.
   * @param pDefaultTabKey Default value to use as a tab key if no tabKey attribute is specified. Use null if no default is available.
   * @param pContextUElem For XPath evaluation.
   * @param pEvaluateDefaultAttr If true, the "default" attribute is evaluated. This only needs to be true for initial creation.
   * @return New EvaluatedTabInfo.
   */
  EvaluatedTabInfo evaluate(DOM pTabDOM, String pDefaultTabKey, DOM pRelativeDOM, ContextUElem pContextUElem, boolean pEvaluateDefaultAttr) {

    String lTabKey;

    if (!XFUtil.isNull(mTabKeyXPath)) {
      try {
        lTabKey = pContextUElem.extendedStringOrXPathString(pTabDOM, mTabKeyXPath);

        //Check XPath returned something useful
        if (XFUtil.isNull(lTabKey)) {
          throw new ExInternal("Tab-key evaluated to null for XPath " + mTabKeyXPath);
        }
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate XPath for tab-key", e);
      }
    }
    else if(pDefaultTabKey != null) {
      lTabKey = pDefaultTabKey;
    }
    else {
      throw new ExInternal("A tab-key must be specified for tabs without a relative tab DOM");
    }

    String lDisplayOrder = null;
    if (!XFUtil.isNull(mDisplayOrderXPath)) {
      try {
        lDisplayOrder = pContextUElem.extendedStringOrXPathString(pTabDOM, mDisplayOrderXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate XPath for tab display order", e);
      }
    }

    boolean lEnabled = true;
    if (!XFUtil.isNull(mTabEnabledXPath)) {
      try {
        lEnabled = pContextUElem.extendedXPathBoolean(pTabDOM, mTabEnabledXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate XPath for tab enabled attribute", e);
      }
    }

    boolean lVisible = true;
    if (!XFUtil.isNull(mTabVisibleXPath)) {
      try {
        lVisible = pContextUElem.extendedXPathBoolean(pTabDOM, mTabVisibleXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate XPath for tab visible attribute", e);
      }
    }

    boolean lDefault = false;
    if(pEvaluateDefaultAttr) {
      try {
        lDefault = pContextUElem.extendedXPathBoolean(pTabDOM, mDefaultXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate XPath for tab default attribute", e);
      }
    }

    return new EvaluatedTabInfo(this, lTabKey, lEnabled, lVisible, pTabDOM, pRelativeDOM, lDisplayOrder, lDefault);
  }
}
