package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.fieldset.TabGroupHiddenField;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.TabGroupPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.module.tabs.EvaluatedTabInfo;
import net.foxopen.fox.module.tabs.TabGroup;

public class EvaluatedTabGroupPresentationNode
extends EvaluatedPresentationNode<TabGroupPresentationNode> {

  private final TabGroup mTabGroup;
  private final Map<String, EvaluatedPresentationNode> mEvaluatedPromptNodes = new HashMap<>();
  private final Map<String, EvaluatedPresentationNode> mEvaluatedContentNodes = new HashMap<>();

  private final boolean mMultiline;
  private final String mCSSClass;

  private final boolean mClientSide;
  private final TabGroupHiddenField mHiddenField;

  /** Contextual field, set during the constructor tab loop, so child TabPrompt nodes can see which tab is currently being processed. */
  private EvaluatedTabInfo mCurrentTabInfo;

  public EvaluatedTabGroupPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParentNode, TabGroupPresentationNode pOriginalNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext, TabGroup pTabGroup) {
    super(pParentNode, pOriginalNode, pEvalContext);
    mTabGroup = pTabGroup;

    ContextUElem lContextUElem = pEvaluatedParseTree.getContextUElem();

    //Client side boolean
    boolean lClientSide = false;
    if(!XFUtil.isNull(pOriginalNode.getClientSideXPath())) {
      try {
        lClientSide = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), pOriginalNode.getClientSideXPath());
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate tab clientSide XPath boolean", e);
      }
    }
    mClientSide = lClientSide;

    //CSS class
    if(!XFUtil.isNull(pOriginalNode.getClassXPath())) {
      try {
        mCSSClass = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), pOriginalNode.getClassXPath());
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate tab class XPath", e);
      }
    }
    else {
      mCSSClass = "";
    }

    //Multiline boolean
    if(!XFUtil.isNull(pOriginalNode.getMultilineThresholdXPath())) {
      try {
        //TODO PN extendedIntegerOrXPathInteger??
        int lMultilineThreshold = Integer.parseInt(lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), pOriginalNode.getMultilineThresholdXPath()));
        mMultiline = pTabGroup.getTabInfoList().size() >= lMultilineThreshold;
      }
      catch (ExActionFailed | NumberFormatException e) {
        throw new ExInternal("Failed to evaluate tab multiline threshold attribute", e);
      }
    }
    else {
      mMultiline = false;
    }

    for(EvaluatedTabInfo lEvalTabInfo : pTabGroup.getTabInfoList()) {
      //Set the contextual tab info for TabPrompt nodes in the prompt presentation node
      mCurrentTabInfo = lEvalTabInfo;

      lContextUElem.localise("TabGroup");
      try {
        lEvalTabInfo.setTabContextLabel(lContextUElem);

        EvaluatedPresentationNode lEvaluatedPromptNode = pEvaluatedParseTree.evaluateNode(this, lEvalTabInfo.getPromptPresentationNode(), pEvalContext);
        mEvaluatedPromptNodes.put(lEvalTabInfo.getTabKey(), lEvaluatedPromptNode);

        //Only evaluate the content node for the current tab (or if this will be a client-side tab group)
        if(pTabGroup.isTabSelected(lEvalTabInfo) || mClientSide) {
          EvaluatedPresentationNode lEvaluatedContentNode = pEvaluatedParseTree.evaluateNode(this, lEvalTabInfo.getContentPresentationNode(), pEvalContext);
          mEvaluatedContentNodes.put(lEvalTabInfo.getTabKey(), lEvaluatedContentNode);
        }
      }
      finally {
        lContextUElem.delocalise("TabGroup");
      }
    }
    //Null out conextual tab info so consumers don't get confused by its presence
    mCurrentTabInfo = null;


    if(lClientSide) {
      //Register the tab group hidden field on the fieldset
      mHiddenField = new TabGroupHiddenField(pTabGroup.getTabGroupKey(), pTabGroup.getSelectedTabKey());
      mHiddenField.addToFieldSet(pEvaluatedParseTree.getFieldSet());
    }
    else {
      mHiddenField = null;
    }
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.TAB_GROUP;
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  public TabGroupHiddenField getHiddenField() {
    return mHiddenField;
  }

  public String getTabSize() {
    return getOriginalNode().getTabSize();
  }

  public String getTabStyle() {
    return getOriginalNode().getTabStyle();
  }

  public TabGroup getTabGroup() {
    return mTabGroup;
  }

  public boolean isClientSide() {
    return mClientSide;
  }

  public boolean isMultiline() {
    return mMultiline;
  }

  public String getCSSClass() {
    return mCSSClass;
  }

  public EvaluatedPresentationNode getEvaluatedPromptNode(String pTabKey) {
    EvaluatedPresentationNode lEvaluatedNode = mEvaluatedPromptNodes.get(pTabKey);
    if(lEvaluatedNode == null) {
      throw new ExInternal("Prompt not evaluated for tab " + pTabKey);
    }
    return lEvaluatedNode;
  }

  public EvaluatedPresentationNode getEvaluatedContentNode(String pTabKey) {
    EvaluatedPresentationNode lEvaluatedNode = mEvaluatedContentNodes.get(pTabKey);
    if(lEvaluatedNode == null) {
      throw new ExInternal("Content not evaluated for tab " + pTabKey);
    }
    return lEvaluatedNode;
  }

  /**
   * For use by fm:tab-prompt-link. This is a subclass of this class because it is only relevant within a TabGroupPresentationNode.
   */
  public static class EvaluatedTabPromptPresentationNode
  extends EvaluatedPresentationNode<PresentationNode> {

    private final EvaluatedTabInfo mEvaluatedTabInfo;
    private final EvaluatedTabGroupPresentationNode mParentTabGroupEPN;
    private final String mPromptText;

    public EvaluatedTabPromptPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParentNode, PresentationNode pOriginalNode, DOM pEvalContext, String pPromptText) {
      super(pParentNode, pOriginalNode, pEvalContext);
      mParentTabGroupEPN = getClosestAncestor(EvaluatedTabGroupPresentationNode.class);
      mEvaluatedTabInfo = mParentTabGroupEPN.mCurrentTabInfo;
      mPromptText = pPromptText;
    }

    @Override
    public ComponentBuilderType getPageComponentType() {
      return ComponentBuilderType.TAB_PROMPT;
    }

    @Override
    public String toString() {
      return getOriginalNode().toString();
    }

    public TabGroup getTabGroup() {
      return mParentTabGroupEPN.getTabGroup();
    }

    public boolean isClientSideTab() {
      return mParentTabGroupEPN.mClientSide;
    }

    public EvaluatedTabInfo getEvaluatedTabInfo() {
      return mEvaluatedTabInfo;
    }

    public String getPromptText() {
      return mPromptText;
    }
  }
}
