package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedInfoBoxPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:info-box elements in a module presentation block
 */
public class InfoBoxPresentationNode extends PresentationNode {
  private final String mInfoBoxType;
  private final String mStyles;
  private final String mClasses;

  private InfoBoxTitleContainerPresentationNode mTitleContainer;
  private ContainerPresentationNode mContentContainer;

  public InfoBoxPresentationNode(DOM pCurrentNode) {
    mInfoBoxType = XFUtil.nvl(pCurrentNode.getAttr("type"), "info");
    mStyles = pCurrentNode.getAttr("style");
    mClasses = pCurrentNode.getAttr("class");

    // Parse children locally
    for (DOM lInfoBoxChild : pCurrentNode.getChildNodes()) {
      // Process Child Node
      if (!lInfoBoxChild.isElement()) {
        continue;
      }
      String lNodeName = lInfoBoxChild.getName();

      if ("fm:title".equals(lNodeName)) {
        if (mTitleContainer != null) {
          throw new ExInternal("More than one Title node under an fm:info-box");
        }
        mTitleContainer = new InfoBoxTitleContainerPresentationNode(lInfoBoxChild);
        mTitleContainer.setDebugInfo("InfoBox Title");
      }
      else if ("fm:content".equals(lNodeName)) {
        if (mContentContainer != null) {
          throw new ExInternal("More than one Content node under an fm:info-box");
        }
        mContentContainer = new ContainerPresentationNode(lInfoBoxChild);
      }
      else {
        throw new ExInternal("Unexpected node found under an fm:info-box: " + lNodeName);
      }
    }

    if (mContentContainer == null) {
      throw new ExInternal("An fm:info-box should have at least one fm:content node inside it");
    }
  }

  public String toString() {
    return "InfoBox ("+mInfoBoxType+")";
  }

  @Override
  public EvaluatedInfoBoxPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedInfoBoxPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }

  public String getInfoBoxType() {
    return mInfoBoxType;
  }

  public String getStyles() {
    return mStyles;
  }

  public String getClasses() {
    return mClasses;
  }

  public InfoBoxTitleContainerPresentationNode getTitleContainer() {
    return mTitleContainer;
  }

  public ContainerPresentationNode getContentContainer() {
    return mContentContainer;
  }

  static public class InfoBoxTitleContainerPresentationNode extends ContainerPresentationNode {
    private final String mLevel;

    public InfoBoxTitleContainerPresentationNode(DOM pCurrentNode) {
      super(pCurrentNode);
      mLevel = XFUtil.nvl(pCurrentNode.getAttr("level"), "4");
    }

    public String getLevel() {
      return mLevel;
    }
  }
}
