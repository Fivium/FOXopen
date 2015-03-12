package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedMailToPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;


public class MailToPresentationNode extends PresentationNode {
  private final String mEmailAddress;
  private final String mCarbonCopyCSVList;
  private final String mPrompt;
  private final String mSubject;
  private final String mHint;
  private final String mImageURL;

  public MailToPresentationNode(DOM pCurrentNode) {
    String lEmailAddress = pCurrentNode.getAttr("email");
    if (lEmailAddress.startsWith("mailto:")) {
      mEmailAddress = lEmailAddress.substring(7, lEmailAddress.length());
    }
    else {
      mEmailAddress = lEmailAddress;
    }
    mCarbonCopyCSVList = pCurrentNode.getAttr("cc");
    mPrompt = pCurrentNode.getAttr("prompt");
    mSubject = pCurrentNode.getAttr("subject");
    mHint = pCurrentNode.getAttr("hint");
    mImageURL = pCurrentNode.getAttr("imageUrl");

    // This type of node has no children to process
  }
  public String toString() {
    return "Mail-To ("+mPrompt+" [" + mEmailAddress + "])";
  }

  @Override
  public EvaluatedMailToPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedMailToPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }

  public String getEmailAddress() {
    return mEmailAddress;
  }

  public String getPrompt() {
    if (XFUtil.isNull(mPrompt)) {
      return mEmailAddress;
    }
    return mPrompt;
  }

  public String getSubject() {
    return mSubject;
  }

  public String getCarbonCopyCSVList() {
    return mCarbonCopyCSVList;
  }

  public String getHint() {
    return mHint;
  }

  public String getImageURL() {
    return mImageURL;
  }
}
