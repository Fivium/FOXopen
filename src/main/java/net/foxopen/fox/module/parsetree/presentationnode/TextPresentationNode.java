package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedTextPresentationNode;

/**
 * This Presentation Node stores the simple text value from a DOM element from a presentation block in a module
 */
public class TextPresentationNode extends PresentationNode {
  private final String mContent;

  public TextPresentationNode(DOM pCurrentNode) {
    mContent = pCurrentNode.value();

    // This type of node has no children to process
  }

  /**
   * Get the text content from the original module DOM node
   * @return Text content
   */
  public String getText() {
    return mContent;
  }

  public String toString() {
    return "Text (" + mContent + ")";
  }

  public EvaluatedTextPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedTextPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
