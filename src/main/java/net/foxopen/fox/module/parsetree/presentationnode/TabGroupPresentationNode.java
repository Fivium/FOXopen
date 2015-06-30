package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedTabGroupPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedTabGroupPresentationNode.EvaluatedTabPromptPresentationNode;
import net.foxopen.fox.module.tabs.TabGroup;
import net.foxopen.fox.module.tabs.TabGroupProvider;
import net.foxopen.fox.module.tabs.TabInfoProvider;

import java.util.List;

public class TabGroupPresentationNode
extends PresentationNode {

  /** tabStyle value for contained tabs */
  public static final String TAB_STYLE_CONTAINED = "contained";

  /** tabContainerStyle value for contained tabs */
  public static final String TAB_CONTAINER_STYLE_CONTAINED = "contained";

  private final String mTabGroupName;
  private final String mAttachXPath;

  private final String mTabStyle;
  private final String mTabContainerStyle;
  private final String mClassXPath;
  private final String mMultilineThresholdXPath;
  private final String mTabSize;

  public final String mClientSideXPath;

  private final List<TabInfoProvider> mTabInfoProviderList;

  public TabGroupPresentationNode(DOM pCurrentNode) {
    mTabGroupName = pCurrentNode.getAttr("name");
    mAttachXPath = XFUtil.nvl(pCurrentNode.getAttr("attach"), ".");

    mTabStyle = pCurrentNode.getAttr("tabStyle");
    mTabContainerStyle = pCurrentNode.getAttr("tabContainerStyle");
    mMultilineThresholdXPath = pCurrentNode.getAttr("multilineThreshold");
    mTabSize = pCurrentNode.getAttr("size");
    mClassXPath = pCurrentNode.getAttr("class");

    mClientSideXPath = pCurrentNode.getAttr("clientSide");

    try {
      mTabInfoProviderList = TabInfoProvider.fromDOM(pCurrentNode);
    }
    catch (ExModule e) {
      throw new ExInternal("Failed to construct " + this.toString(), e);
    }
  }


  @Override
  public String toString() {
    return "TabGroup (name="+mTabGroupName+", attach=" + mAttachXPath + ")";
  }

  @Override
  public EvaluatedTabGroupPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    DOM lMatchNode;
    try {
      lMatchNode = pEvaluatedParseTree.getContextUElem().extendedXPath1E(pEvalContext, mAttachXPath);
    }
    catch (ExCardinality | ExActionFailed e) {
      throw new ExInternal("Failed to resolve a single match node for " + this.toString(), e);
    }

    TabGroupProvider lTabGroupProvider = pEvaluatedParseTree.getModuleFacetProvider(TabGroupProvider.class);

    TabGroup lTabGroup = lTabGroupProvider.getOrCreateTabGroup(mTabGroupName, lMatchNode, mTabInfoProviderList, pEvaluatedParseTree.getContextUElem());

    return new EvaluatedTabGroupPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext, lTabGroup);
  }

  public String getTabStyle() {
    return mTabStyle;
  }

  public String getTabContainerStyle() {
    return mTabContainerStyle;
  }

  public String getTabSize() {
    return mTabSize;
  }

  public String getClientSideXPath() {
    return mClientSideXPath;
  }

  public String getClassXPath() {
    return mClassXPath;
  }

  public String getMultilineThresholdXPath() {
    return mMultilineThresholdXPath;
  }

  /**
   * For use by fm:tab-prompt-link. This is a subclass of this class because it is only relevant within a TabGroupPresentationNode.
   */
  public static class TabPromptPresentationNode
  extends PresentationNode {

    private final String mPromptTextXPath;

    public TabPromptPresentationNode(DOM pSourceDOM) {
      mPromptTextXPath = pSourceDOM.getAttrOrNull("text");
      if(XFUtil.isNull(mPromptTextXPath)) {
        throw new ExInternal("Tab prompt missing mandatory text attribute");
      }
    }

    public TabPromptPresentationNode(String pPromptTextXPath) {
      super();
      mPromptTextXPath = pPromptTextXPath;
    }

    @Override
    public String toString() {
      return "TabPrompt (text=" + mPromptTextXPath + ")";
    }

    @Override
    public EvaluatedTabPromptPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
      String lEvaluatedPrompt;
      try {
        lEvaluatedPrompt = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(pEvalContext, mPromptTextXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluated XPath for tab prompt text", e);
      }

      return new EvaluatedTabPromptPresentationNode(pParent, this, pEvalContext, lEvaluatedPrompt);
    }
  }
}
