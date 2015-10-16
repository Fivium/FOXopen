package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedCurrentPageNumberPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:current-page-number
 */
public class CurrentPageNumberPresentationNode extends PageNumberPresentationNode {
  public CurrentPageNumberPresentationNode(DOM pCurrentNode) {
    super(pCurrentNode);
  }

  public String toString() {
    return "CurrentPageNumber";
  }

  @Override
  public EvaluatedCurrentPageNumberPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                             EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedCurrentPageNumberPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
