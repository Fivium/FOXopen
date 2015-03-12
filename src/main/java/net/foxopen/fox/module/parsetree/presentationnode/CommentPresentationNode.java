package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedCommentPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores any xml comments from the module presentation block
 */
public class CommentPresentationNode extends PresentationNode {
  String mContent;

  /**
   * Serialise the comment node passed in to a string which can be served out with getText()
   *
   * @param pCurrentNode XML comment node from the module
   */
  public CommentPresentationNode(DOM pCurrentNode) {
    mContent = pCurrentNode.outputNodeToString(false);

    // This type of node has no children to process
  }

  /**
   * Get the comment serialsed as text including the <!-- and -->
   * @return Comment element in plain text
   */
  public String getText() {
    return mContent;
  }

  public String toString() {
    return "Comment ("+mContent+")";
  }

  @Override
  public EvaluatedCommentPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedCommentPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
