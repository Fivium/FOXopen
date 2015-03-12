package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.TextPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Stores the content of a regular text node from a DOM object via the
 */
public class EvaluatedTextPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private final String mTextContent;

  public EvaluatedTextPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, TextPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    mTextContent = pOriginalPresentationNode.getText();
  }

  /**
   * Return the simple text content from the Text Presentation Node
   * @return Text content
   */
  @Override
  public String getText() {
    return mTextContent;
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
    return ComponentBuilderType.TEXT;
  }
}
