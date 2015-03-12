package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHintOutPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;


public class HintOutPresentationNode extends PresentationNode {
  private final String mHintTitle;
  private final String mHintText;
  private final String mHintURL;

  public HintOutPresentationNode(DOM pCurrentNode) {
    mHintTitle = pCurrentNode.getAttr("prompt");
    mHintText = pCurrentNode.getAttr("text");
    mHintURL = pCurrentNode.getAttr("hint-url");

    // This type of node has no children to process
  }

  public String getHintTitle() {
    return mHintTitle;
  }

  public String getHintText() {
    return mHintText;
  }

  public String getHintURL() {
    return mHintURL;
  }

  public EvaluatedHintOutPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedHintOutPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }

  public String toString() {
    return "HintOutNode ("+mHintText+")";
  }
}
