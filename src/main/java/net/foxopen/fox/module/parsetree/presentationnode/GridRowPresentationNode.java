package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedGridRowPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:row elements in a module presentation block (and nested under an fm:grid element)
 */
public class GridRowPresentationNode extends PresentationNode {
  private final String mStyles;
  private final String mClasses;

  public GridRowPresentationNode(DOM pCurrentNode) {
    mStyles = pCurrentNode.getAttr("style");
    mClasses = pCurrentNode.getAttr("class");

    // Process children
    ParseTree.parseDOMChildren(this, pCurrentNode);
  }

  public EvaluatedGridRowPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedGridRowPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }

  public String getStyles() {
    return mStyles;
  }

  public String getClasses() {
    return mClasses;
  }

  public String toString() {
    return "GridRow";
  }
}
