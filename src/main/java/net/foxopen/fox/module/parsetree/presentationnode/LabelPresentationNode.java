package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedLabelPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:label elements in a module presentation block
 */
public class LabelPresentationNode extends PresentationNode {
  /** The for-target attribute can contain an XPath to 1 element which the label should target */
  private final String mForTarget;

  public LabelPresentationNode(DOM pCurrentNode) {
    mForTarget = pCurrentNode.getAttr("for-target");

    // Process children
    ParseTree.parseDOMChildren(this, pCurrentNode, false);
  }

  public String toString() {
    return "Label (" + mForTarget + ")";
  }

  @Override
  public EvaluatedLabelPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedLabelPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }

  public String getForTarget() {
    return mForTarget;
  }
}