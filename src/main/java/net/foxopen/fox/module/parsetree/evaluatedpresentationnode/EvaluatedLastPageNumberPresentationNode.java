package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.LastPageNumberPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Evaluate an fm:current-page-number for serialising later
 */
public class EvaluatedLastPageNumberPresentationNode extends EvaluatedPageNumberPresentationNode {
  public EvaluatedLastPageNumberPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                 LastPageNumberPresentationNode pOriginalPresentationNode,
                                                 EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvaluatedParseTree, pEvalContext);
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.LAST_PAGE_NUMBER;
  }
}
