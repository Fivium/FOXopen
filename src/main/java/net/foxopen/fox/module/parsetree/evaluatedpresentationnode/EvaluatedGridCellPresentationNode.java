package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.GridCellPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.FOXGridUtils;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Evaluate an fm:cell for serialising later
 */
public class EvaluatedGridCellPresentationNode extends EvaluatedPresentationNode<PresentationNode> {

  private final String mColumnSpan;
  private final String mStyles;
  private final String mClasses;

  /**
   * Evaluate a GridCellPresentationNode object by evaluating the attributes
   *
   * @param pParent
   * @param pOriginalPresentationNode
   * @param pEvalParseTree
   * @param pEvalContext
   */
  public EvaluatedGridCellPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, GridCellPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    String lColumnSpan = pOriginalPresentationNode.getColumnSpan();
    String lStyles = pOriginalPresentationNode.getStyles();
    String lClasses = pOriginalPresentationNode.getClasses();

    try {
      if (!XFUtil.isNull(lColumnSpan)) {
        lColumnSpan = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lColumnSpan);
      }
      else {
        lColumnSpan = "1";
      }

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

    if (XFUtil.isNull(lColumnSpan)) {
      throw new ExInternal("ColumnSpan attribute evaluated to null");
    }

    EvaluatedGridPresentationNode lCurrentGrid = getClosestAncestor(EvaluatedGridPresentationNode.class);
    if (lCurrentGrid == null) {
      throw new ExInternal("fm:cell found without a containing grid: " + toString());
    }
    mColumnSpan = FOXGridUtils.calculateColumnClassName(Integer.parseInt(lColumnSpan), Integer.parseInt(lCurrentGrid.getFormColumns()));

    mStyles = lStyles;
    mClasses = lClasses;
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.GRID_CELL;
  }

  public String getColumnSpan() {
    return mColumnSpan;
  }

  public String getStyles() {
    return mStyles;
  }

  public String getClasses() {
    return mClasses;
  }
}
