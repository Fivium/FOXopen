package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.MailToPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

public class EvaluatedMailToPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private final String mEmailAddress;
  private final String mCarbonCopyCSVList;
  private final String mPrompt;
  private final String mSubject;
  private final String mHint;
  private final String mImageURL;

  public EvaluatedMailToPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, MailToPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    String lEmailAddress = pOriginalPresentationNode.getEmailAddress();
    String lCarbonCopyCSVList = pOriginalPresentationNode.getCarbonCopyCSVList();
    String lPrompt = pOriginalPresentationNode.getPrompt();
    String lSubject = pOriginalPresentationNode.getSubject();
    String lHint = pOriginalPresentationNode.getHint();
    String lImageURL = pOriginalPresentationNode.getImageURL();

    try {
      if (!XFUtil.isNull(lEmailAddress)) {
        lEmailAddress = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lEmailAddress);
      }

      if (!XFUtil.isNull(lCarbonCopyCSVList)) {
        lCarbonCopyCSVList = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lCarbonCopyCSVList);
      }

      if (!XFUtil.isNull(lPrompt)) {
        lPrompt = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lPrompt);
      }

      if (!XFUtil.isNull(lSubject)) {
        lSubject = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lSubject);
      }

      if (!XFUtil.isNull(lHint)) {
        lHint = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lHint);
      }

      if (!XFUtil.isNull(lImageURL)) {
        lImageURL = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lImageURL);
      }

      if (!XFUtil.exists(lEmailAddress)) {
        throw new ExInternal("Can't have a mail-to without an email address to send it to");
      }
    }
    catch (ExActionFailed e) {
      throw e.toUnexpected();
    }

    mEmailAddress = lEmailAddress;
    mCarbonCopyCSVList = lCarbonCopyCSVList;
    mPrompt = lPrompt;
    mSubject = lSubject;
    mHint = lHint;
    mImageURL = lImageURL;
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  public String getEmailAddress() {
    return mEmailAddress;
  }

  public String getPrompt() {
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

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.MAILTO;
  }
}
