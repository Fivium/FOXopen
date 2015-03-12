package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.CommentPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

public class EvaluatedCommentPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private String mComment;

  public EvaluatedCommentPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, CommentPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);
    mComment = pOriginalPresentationNode.getText();
  }

  @Override
  public String getText() {
    return mComment;
  }

  @Override
  public boolean isEscapingRequired() {
    return false;
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.COMMENT;
  }
}
