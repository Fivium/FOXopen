package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedMenuOutPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;


public class MenuOutPresentationNode extends GenericAttributesPresentationNode {
  private final String mMode;

  public MenuOutPresentationNode(DOM pCurrentNode) {
    super(pCurrentNode);

    mMode = pCurrentNode.getAttr("mode");

    // This type of node has no children to process
  }

  public String getMode() {
    return mMode;
  }

  public String toString() {
    return "MenuOutNode ("+mMode+")";
  }

  public EvaluatedMenuOutPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedMenuOutPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
