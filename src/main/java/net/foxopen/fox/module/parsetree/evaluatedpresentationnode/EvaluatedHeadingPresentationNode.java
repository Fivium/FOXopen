package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.HeadingPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Evaluate an fm:heading for serialising later
 */
public class EvaluatedHeadingPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private final String mLevel;
  private final String mStyles;
  private final String mClasses;

  /**
   * Evaluate a HeadingPresentationNode object by evaluating the attributes
   *
   * @param pParent
   * @param pOriginalPresentationNode
   * @param pEvalParseTree
   * @param pEvalContext
   */
  public EvaluatedHeadingPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, HeadingPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    String lLevel = pOriginalPresentationNode.getLevel();
    String lStyles = pOriginalPresentationNode.getStyles();
    String lClasses = pOriginalPresentationNode.getClasses();

    if (XFUtil.isNull(lLevel)) {
      throw new ExInternal("You must specify a level on an fm:heading element");
    }

    try {
      lLevel = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lLevel);

      if (!XFUtil.isNull(lStyles)) {
        lStyles = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lStyles);
      }

      if (!XFUtil.isNull(lClasses)) {
        lClasses = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lClasses);
      }
    }
    catch (ExActionFailed e) {
      e.toUnexpected();
    }

    if (XFUtil.isNull(lLevel)) {
      throw new ExInternal("You must specify a level on an fm:heading element");
    }

    mLevel = lLevel;
    mStyles = lStyles;
    mClasses = lClasses;
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.HEADING;
  }

  public String getLevel() {
    return mLevel;
  }

  public String getStyles() {
    return mStyles;
  }

  public String getClasses() {
    return mClasses;
  }
}
