package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.ContainerPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.InfoBoxPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.InfoBoxPresentationNode.InfoBoxTitleContainerPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;


/**
 * Evaluate an fm:info-box for serialising later
 */
public class EvaluatedInfoBoxPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private static final List<String> VALID_INFO_BOX_TYPES = new ArrayList<>(Arrays.asList("info", "success", "warning", "danger"));
  private final String mInfoBoxType;
  private final String mStyles;
  private final String mClasses;
  private final String mTitleLevel;

  private final EvaluatedPresentationNode mTitleContainer;
  private final EvaluatedPresentationNode mContentContainer;

  /**
   * Evaluate a InfoBoxPresentationNode object by evaluating the attributes
   *
   * @param pParent
   * @param pOriginalPresentationNode
   * @param pEvalParseTree
   * @param pEvalContext
   */
  public EvaluatedInfoBoxPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, InfoBoxPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    // Pull in an evaluate the title node
    InfoBoxTitleContainerPresentationNode lOriginalTitleNode = pOriginalPresentationNode.getTitleContainer();
    if (lOriginalTitleNode != null) {
      mTitleContainer = pEvaluatedParseTree.evaluateNode(this, lOriginalTitleNode, pEvalContext);
    }
    else {
      mTitleContainer = null;
    }

    // Evaluate xpaths on attributes
    String lInfoBoxType = pOriginalPresentationNode.getInfoBoxType();
    String lStyles = pOriginalPresentationNode.getStyles();
    String lClasses = pOriginalPresentationNode.getClasses();
    String lTitleLevel = null;

    try {
      if (!XFUtil.isNull(lInfoBoxType)) {
        lInfoBoxType = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lInfoBoxType);
      }

      if (!XFUtil.isNull(lStyles)) {
        lStyles = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lStyles);
      }

      if (!XFUtil.isNull(lClasses)) {
        lClasses = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lClasses);
      }

      if (lOriginalTitleNode != null && !XFUtil.isNull(lOriginalTitleNode.getLevel())) {
        lTitleLevel = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lOriginalTitleNode.getLevel());
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Error while parsing XPaths on fm:info-box attributes", e);
    }

    if (!VALID_INFO_BOX_TYPES.contains(lInfoBoxType)) {
      throw new ExInternal("Unknown info box type: " + lInfoBoxType);
    }

    mInfoBoxType = lInfoBoxType;
    mStyles = lStyles;
    mClasses = lClasses;
    mTitleLevel = lTitleLevel;

    ContainerPresentationNode lOriginalContentNode = pOriginalPresentationNode.getContentContainer();
    mContentContainer = pEvaluatedParseTree.evaluateNode(this, lOriginalContentNode, pEvalContext);
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.INFO_BOX;
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

  public EvaluatedPresentationNode getEvaluatedTitleContainer() {
    return mTitleContainer;
  }

  public EvaluatedPresentationNode getEvaluatedContentContainer() {
    return mContentContainer;
  }

  public String getTitleLevel() {
    return mTitleLevel;
  }
}
