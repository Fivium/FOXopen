package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.CurrentPageNumberPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Evaluate an fm:current-page-number for serialising later
 */
public class EvaluatedCurrentPageNumberPresentationNode extends EvaluatedPageNumberPresentationNode {
  public EvaluatedCurrentPageNumberPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                    CurrentPageNumberPresentationNode pOriginalPresentationNode,
                                                    EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvaluatedParseTree, pEvalContext);
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.CURRENT_PAGE_NUMBER;
  }
}
