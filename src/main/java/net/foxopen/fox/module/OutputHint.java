package net.foxopen.fox.module;

import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;

public class OutputHint {
  private final String mHintID;
  private final StringAttributeResult mTitle;
  private final StringAttributeResult mContent;
  private final EvaluatedPresentationNode<? extends PresentationNode> mHintBufferContent;
  private final OutputDescription mDescription;
  private final String mHintURL;

  public OutputHint(String pHintID, StringAttributeResult pTitle, StringAttributeResult pContent
    , EvaluatedPresentationNode<? extends PresentationNode> pHintBufferContent, OutputDescription pDescription
    , String pHintURL) {
    mHintID = pHintID;
    mTitle = pTitle;
    mContent = pContent;
    mHintBufferContent = pHintBufferContent;
    mDescription = pDescription;
    mHintURL = pHintURL;
  }

  public String getHintID() {
    return mHintID;
  }


  public String getHintContentID() {
    return mHintID + "-content";
  }

  public StringAttributeResult getTitle() {
    return mTitle;
  }

  public StringAttributeResult getContent() {
    return mContent;
  }

  public OutputDescription getDescription() {
    return mDescription;
  }

  public String getHintURL() {
    return mHintURL;
  }

  public EvaluatedPresentationNode<? extends PresentationNode> getHintBufferContent() {
    return mHintBufferContent;
  }
}
