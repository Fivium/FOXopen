package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.FooterPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Evaluate an fm:footer for serialising later
 */
public class EvaluatedFooterPresentationNode extends EvaluatedHeaderFooterPresentationNode {
  public EvaluatedFooterPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                         FooterPresentationNode pOriginalPresentationNode,
                                         EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvaluatedParseTree, pEvalContext);
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.FOOTER;
  }
}
