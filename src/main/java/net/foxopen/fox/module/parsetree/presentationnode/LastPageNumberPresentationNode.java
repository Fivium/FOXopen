package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedLastPageNumberPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:last-page-number
 */
public class LastPageNumberPresentationNode extends PageNumberPresentationNode {
  public LastPageNumberPresentationNode(DOM pCurrentNode) {
    super(pCurrentNode);
  }

  public String toString() {
    return "LastPageNumber";
  }

  @Override
  public EvaluatedLastPageNumberPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                          EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedLastPageNumberPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
