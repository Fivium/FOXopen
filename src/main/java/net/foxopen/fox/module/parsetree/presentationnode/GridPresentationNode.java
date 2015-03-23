package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedGridPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:grid elements in a module presentation block
 */
public class GridPresentationNode extends PresentationNode {
  private final String mFormColumns;
  private final String mStyles;
  private final String mClasses;

  public GridPresentationNode(DOM pCurrentNode) {
    mFormColumns = pCurrentNode.getAttr("formColumns");
    mStyles = pCurrentNode.getAttr("style");
    mClasses = pCurrentNode.getAttr("class");

    // Process children
    ParseTree.parseDOMChildren(this, pCurrentNode, false);
  }

  public String toString() {
    return "Grid ("+mFormColumns+" columns)";
  }

  public EvaluatedGridPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedGridPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
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
