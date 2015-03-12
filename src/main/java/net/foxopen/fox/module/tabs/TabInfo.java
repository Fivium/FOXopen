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
  final String mTabContextName;
  final String mPreTabActionName;
  final String mPostTabActionName;
  final String mDisplayOrderXPath;
  final String mDefaultXPath;
  final PresentationNode mPromptPresentationNode;
  final PresentationNode mContentPresentationNode;

  TabInfo(String pTabKeyXPath, String pTabEnabledXPath, String pTabContextName, String pPreTabActionName, String pPostTabActionName,
          String pDisplayOrderXPath, String pDefaultXPath, PresentationNode pPromptPresentationNode, PresentationNode pContentPresentationNode) {
    mTabKeyXPath = pTabKeyXPath;
    mTabEnabledXPath = pTabEnabledXPath;
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
   * @param pTabDOMIsContextual True if pTabDOM represents a contextual tab from a for-each loop (for example).
   * @param pContextUElem
   * @param pEvaluateDefaultAttr
   * @return
   */
  EvaluatedTabInfo evaluate(DOM pTabDOM, boolean pTabDOMIsContextual, DOM pRelativeDOM, ContextUElem pContextUElem, boolean pEvaluateDefaultAttr) {

    String lTabKey;
    if (pTabDOMIsContextual) {
      //If contextual then there is a "real" DOM for this tab which we can use the reference of
      //If not, then the tab DOM will be the tab group attach DOM which is no use for generating a tab key
      lTabKey = pTabDOM.getFoxId();
    }
    else if (!XFUtil.isNull(mTabKeyXPath)) {
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

    boolean lDefault = false;
    if(pEvaluateDefaultAttr) {
      try {
        lDefault = pContextUElem.extendedXPathBoolean(pTabDOM, mDefaultXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate XPath for tab default attribute", e);
      }
    }

    return new EvaluatedTabInfo(this, lTabKey, lEnabled, pTabDOM, pRelativeDOM, lDisplayOrder, lDefault);
  }
}
