package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.Pager;
import net.foxopen.fox.dom.paging.style.PageControlStyle;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

public class EvaluatedPagerControlPresentationNode extends EvaluatedPresentationNode<PresentationNode> {

  private final Pager mPager;
  private final PageControlStyle mPageControlStyle;

  public EvaluatedPagerControlPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParentNode, PresentationNode pOriginalNode, EvaluatedParseTree pEvalParseTree, DOM pEvalContext, Pager pPager) {
    super(pParentNode, pOriginalNode, pEvalContext);
    mPager = pPager;
    mPageControlStyle = pPager.getPageControlStyle(pEvalParseTree.getRequestContext());
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.PAGER_CONTROL;
  }

  public Pager getPager() {
    return mPager;
  }

  public PageControlStyle getPageControlStyle() {
    return mPageControlStyle;
  }
}
