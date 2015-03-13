package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.ExternalURLPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

public class EvaluatedExternalURLPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private final String mHRef;
  private final String mLinkText;
  private final String mType;
  private final String mTitle;

  public EvaluatedExternalURLPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, ExternalURLPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    try {
      mHRef = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), pOriginalPresentationNode.getHRef());
      mLinkText = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), pOriginalPresentationNode.getLinkText());
    }
    catch(ExActionFailed e) {
      throw e.toUnexpected("Failed running XPaths in external-url");
    }

    mType = pOriginalPresentationNode.getType();
    //TODO attribute to set link title
    mTitle = "";

    if (!XFUtil.exists(mHRef)||!XFUtil.exists(mLinkText)) {
      throw new ExInternal("Can't have an external url without a href or link text");
    }
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  public String getHRef() {
    return mHRef;
  }

  public String getLinkText() {
    return mLinkText;
  }

  public String getType() {
    if (!XFUtil.exists(mType)) {
      return "fullwin";
    }
    else {
      return mType;
    }
  }

  public String getTitle() {
    return mTitle;
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.EXTERNAL_URL;
  }
}
