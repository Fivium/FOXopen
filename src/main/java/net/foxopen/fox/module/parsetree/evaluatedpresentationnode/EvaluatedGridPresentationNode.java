package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.GridPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Evaluate an fm:grid for serialising later
 */
public class EvaluatedGridPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private final String mFormColumns;
  private final String mStyles;
  private final String mClasses;

  /**
   * Evaluate a GridPresentationNode object by evaluating the attributes
   *
   * @param pParent
   * @param pOriginalPresentationNode
   * @param pEvalParseTree
   * @param pEvalContext
   */
  public EvaluatedGridPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, GridPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    String lFormColumns = pOriginalPresentationNode.getFormColumns();
    String lStyles = pOriginalPresentationNode.getStyles();
    String lClasses = pOriginalPresentationNode.getClasses();

    try {
      if (!XFUtil.isNull(lFormColumns)) {
        lFormColumns = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lFormColumns);
      }
      else {
        lFormColumns = "12";
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

    if (XFUtil.isNull(lFormColumns)) {
      throw new ExInternal("FormColumns attribute evaluated to null");
    }

    mFormColumns = lFormColumns;
    mStyles = lStyles;
    mClasses = lClasses;
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.GRID;
  }

  public String getFormColumns() {
    return mFormColumns;
  }

  public String getStyles() {
    return mStyles;
  }

  public String getClasses() {
    return mClasses;
  }
}
