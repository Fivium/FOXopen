package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedExternalURLPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.components.html.ExternalURLComponentBuilder;


public class ExternalURLPresentationNode extends PresentationNode {
  private final String mHRef;
  private final String mLinkText;
  private final String mType;
  private final String mTitle;

  public ExternalURLPresentationNode(DOM pCurrentNode) {
    mHRef = pCurrentNode.getAttr("href");
    mLinkText = pCurrentNode.getAttr("text");
    mType = pCurrentNode.getAttr("type");
    mTitle = pCurrentNode.getAttr("title");

    // This type of node has no children to process
  }
  public String toString() {
    return "ExternalURL ("+mLinkText+" [" + mHRef + "])";
  }

  @Override
  public EvaluatedExternalURLPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedExternalURLPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }

  public String getHRef() {
    return mHRef;
  }

  public String getLinkText() {
    return mLinkText;
  }

  public String getType() {
    if (!XFUtil.exists(mType)) {
      return ExternalURLComponentBuilder.NON_JS_LINK_TYPE;
    }
    return mType;
  }

  public String getTitle() {
    return mTitle;
  }
}
