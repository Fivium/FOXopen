package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.Pager;
import net.foxopen.fox.dom.paging.PagerProvider;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPagerControlPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

public class PagerControlPresentationNode
extends PresentationNode {

  private final String mInvokeName;
  private final String mMatchXPath;

  public PagerControlPresentationNode(DOM pCurrentNode) {
    mInvokeName = pCurrentNode.getAttr("invoke-name");
    mMatchXPath = pCurrentNode.getAttr("match");
  }

  public PagerControlPresentationNode(String pInvokeName) {
    mInvokeName = pInvokeName;
    mMatchXPath = null;
  }

  public String toString() {
    return "PageControlsOut (invoke="+mInvokeName+", match=" + mMatchXPath + ")";
  }

  @Override
  public EvaluatedPagerControlPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    String lMatchId = Pager.getMatchIdOrNull(pEvaluatedParseTree.getContextUElem(), mMatchXPath, pEvalContext);

    Pager lPager = pEvaluatedParseTree.getModuleFacetProvider(PagerProvider.class).getPagerOrNull(mInvokeName, lMatchId);
    if(lPager != null) {
      return new EvaluatedPagerControlPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext, lPager);
    }
    else {
      Track.info("PagerNotFound", "page-controls-out: No pager found for invoke name " + mInvokeName + ", match id " + lMatchId, TrackFlag.PARSE_TREE_WARNING);
      return null;
    }
  }
}
