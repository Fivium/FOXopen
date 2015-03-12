package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.ContainerPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

import java.util.List;


/**
 * This EvaluatedPresentationNode is just a container to hold multiple actual EvaluatedPresentationNodes, for use in
 * nodes like fm:if and fm:case where there are multiple paths to choose from which contain multiple nodes and need
 * parsing and ideal evaluating to a common node
 */
public class EvaluatedContainerPresentationNode extends EvaluatedPresentationNode<PresentationNode> {

  public EvaluatedContainerPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, ContainerPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);
  }

  //TODO PN - needs to be nicer
  public EvaluatedContainerPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, PresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree,
                                            DOM pEvalContext, List<EvaluatedPresentationNode<? extends PresentationNode>> pChildren) {
    super(pParent, pOriginalPresentationNode, pEvalContext);
    addChildren(pChildren);
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.NODE_CONTAINER;
  }
}
