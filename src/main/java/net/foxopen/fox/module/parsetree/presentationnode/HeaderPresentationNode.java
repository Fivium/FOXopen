package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHeaderPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:header in a buffer
 */
public class HeaderPresentationNode extends HeaderFooterPresentationNode {
  public HeaderPresentationNode(DOM pCurrentNode) {
    super(pCurrentNode);
  }

  public String toString() {
    return "Header";
  }

  @Override
  public EvaluatedHeaderPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                  EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedHeaderPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
