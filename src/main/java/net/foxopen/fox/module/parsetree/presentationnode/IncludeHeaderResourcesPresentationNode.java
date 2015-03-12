package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedIncludeHeaderResourcesPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:include-header-resources elements in a module presentation block
 */
public class IncludeHeaderResourcesPresentationNode extends PresentationNode {

  public IncludeHeaderResourcesPresentationNode(DOM pCurrentNode) {
    // No children to process
  }

  public String toString() {
    return "include-header-resources";
  }

  public EvaluatedIncludeHeaderResourcesPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedIncludeHeaderResourcesPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
