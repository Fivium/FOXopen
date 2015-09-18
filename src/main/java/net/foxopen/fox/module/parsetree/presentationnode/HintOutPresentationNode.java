package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHintOutPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;


public class HintOutPresentationNode extends PresentationNode {
  private final String mHintTitle;
  private final String mHintText;
  private final String mHintBufferName;
  private final String mHintBufferAttachXPath;
  private final String mHintURL;

  public HintOutPresentationNode(DOM pCurrentNode) {
    mHintTitle = pCurrentNode.getAttr("prompt");
    mHintText = pCurrentNode.getAttr("text");
    mHintBufferName = pCurrentNode.getAttr(NodeAttribute.HINT_BUFFER.getExternalString());
    mHintBufferAttachXPath = pCurrentNode.getAttr(NodeAttribute.HINT_BUFFER_ATTACH_DOM.getExternalString());
    mHintURL = pCurrentNode.getAttr("hint-url");

    // This type of node has no children to process
  }

  public String getHintTitle() {
    return mHintTitle;
  }

  public String getHintText() {
    return mHintText;
  }

  public String getHintBufferName() {
    return mHintBufferName;
  }

  public String getHintBufferAttachXPath() {
    return mHintBufferAttachXPath;
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
