package net.foxopen.fox.module;

import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;

public class OutputDescription {
  private final StringAttributeResult mDescriptionText;
  private final EvaluatedPresentationNode<? extends PresentationNode> mDescriptionBufferContent;

  public OutputDescription(StringAttributeResult pDescriptionText, EvaluatedPresentationNode<? extends PresentationNode> pDescriptionBufferContent) {
    mDescriptionText = pDescriptionText;
    mDescriptionBufferContent = pDescriptionBufferContent;
  }

  public StringAttributeResult getDescriptionText() {
    return mDescriptionText;
  }

  public EvaluatedPresentationNode<? extends PresentationNode> getDescriptionBufferContent() {
    return mDescriptionBufferContent;
  }
}
