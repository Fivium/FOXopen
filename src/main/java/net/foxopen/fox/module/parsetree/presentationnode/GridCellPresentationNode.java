package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedGridCellPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:cell elements in a module presentation block (and nested under an fm:grid element)
 */
public class GridCellPresentationNode extends PresentationNode {
  private final String mColumnSpan;
  private final String mStyles;
  private final String mClasses;

  public GridCellPresentationNode(DOM pCurrentNode) {
    mColumnSpan = pCurrentNode.getAttr("columnSpan");
    mStyles = pCurrentNode.getAttr("style");
    mClasses = pCurrentNode.getAttr("class");

    // Process children
    ParseTree.parseDOMChildren(this, pCurrentNode);
  }

  public String toString() {
    return "Grid ("+mColumnSpan+" column span)";
  }

  public EvaluatedGridCellPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedGridCellPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
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
