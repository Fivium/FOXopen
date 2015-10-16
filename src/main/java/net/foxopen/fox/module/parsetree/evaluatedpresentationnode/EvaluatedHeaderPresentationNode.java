package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.HeaderPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Evaluate an fm:header for serialising later
 */
public class EvaluatedHeaderPresentationNode extends EvaluatedHeaderFooterPresentationNode {
  public EvaluatedHeaderPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                         HeaderPresentationNode pOriginalPresentationNode,
                                         EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvaluatedParseTree, pEvalContext);
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.HEADER;
  }
}
