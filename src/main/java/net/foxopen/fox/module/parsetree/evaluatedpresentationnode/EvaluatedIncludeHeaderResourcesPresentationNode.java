package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.IncludeHeaderResourcesPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Evaluate an fm:include-header-resources for serialising later
 */
public class EvaluatedIncludeHeaderResourcesPresentationNode extends EvaluatedPresentationNode<PresentationNode> {

  /**
   * Evaluate a IncludeHeaderResourcesPresentationNode object by evaluating the attributes
   *
   * @param pParent
   * @param pOriginalPresentationNode
   * @param pEvalParseTree
   * @param pEvalContext
   */
  public EvaluatedIncludeHeaderResourcesPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, IncludeHeaderResourcesPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.HEADER_RESOURCES;
  }
}
