package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedFooterPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:footer in a buffer
 */
public class FooterPresentationNode extends HeaderFooterPresentationNode {
  public FooterPresentationNode(DOM pCurrentNode) {
    super(pCurrentNode);
  }

  public String toString() {
    return "Footer";
  }

  @Override
  public EvaluatedFooterPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                  EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedFooterPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
