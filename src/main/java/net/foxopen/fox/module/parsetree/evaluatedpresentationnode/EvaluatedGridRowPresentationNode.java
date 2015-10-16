package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.GridRowPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Evaluate an fm:row for serialising later
 */
public class EvaluatedGridRowPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private final String mStyles;
  private final String mClasses;

  /**
   * Evaluate a GridRowPresentationNode object by evaluating the attributes
   *
   * @param pParent
   * @param pOriginalPresentationNode
   * @param pEvalParseTree
   * @param pEvalContext
   */
  public EvaluatedGridRowPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, GridRowPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    String lStyles = pOriginalPresentationNode.getStyles();
    String lClasses = pOriginalPresentationNode.getClasses();

    try {
      if (!XFUtil.isNull(lStyles)) {
        lStyles = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lStyles);
      }

      if (!XFUtil.isNull(lClasses)) {
        lClasses = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lClasses);
      }
    }
    catch (ExActionFailed e) {
      throw e.toUnexpected();
    }

    mStyles = lStyles;
    mClasses = lClasses;
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.GRID_ROW;
  }

  public String getStyles() {
    return mStyles;
  }

  public String getClasses() {
    return mClasses;
  }
}
