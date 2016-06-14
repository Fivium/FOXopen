package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.PagerSetup;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.PageControlsPosition;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedContainerPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedSetOutPresentationNode;

import java.util.List;


/**
 * This Presentation Node is for a fm:set-out element
 */
public class SetOutPresentationNode
extends GenericAttributesPresentationNode
implements SetOutInfoProvider {
  private final String mMatch;
  private final String mDatabasePaginationInvokeName;
  private final PagerSetup mDOMPagerSetup;

  private final PageControlsPosition mPageControlsPosition;

  public SetOutPresentationNode(DOM pCurrentNode) {
    super(pCurrentNode);

    mMatch = getAttrOrNull("match");

    //Note DOM and database pagination attributes are seperate and mutually exclusive.
    mDatabasePaginationInvokeName = getAttrOrNull("pagination-invoke-name");
    try {
      mDOMPagerSetup = PagerSetup.fromDOMMarkupOrNull(pCurrentNode, "pagination-definition", "page-size", "dom-pagination-invoke-name");
    }
    catch (ExModule e) {
      throw new ExInternal("Bad pagination markup in set-out command for match " + mMatch, e);
    }

    if(mDOMPagerSetup != null && !XFUtil.isNull(mDatabasePaginationInvokeName)) {
      throw new ExInternal("You cannot define both a pagination-invoke-name and a dom-pagination-invoke-name on the same set-out node");
    }

    String lPageControlsPositionAttr = getAttrOrNull("page-controls-position");
    if(!XFUtil.isNull(lPageControlsPositionAttr)) {
      mPageControlsPosition = PageControlsPosition.fromString(lPageControlsPositionAttr);
    }
    else {
      //Default page control position is "both"
      mPageControlsPosition = PageControlsPosition.BOTH;
    }

    // This type of node has no children to process
  }

  @Override
  public String toString() {
    return "SetOutNode ("+mMatch+")";
  }

  @Override
  public EvaluatedContainerPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    List<EvaluatedPresentationNode<? extends PresentationNode>> lEvaluatedNodes = EvaluatedSetOutPresentationNode.evaluate(pParent, this, pEvaluatedParseTree, pEvalContext);
    return new EvaluatedContainerPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext, lEvaluatedNodes);
  }

  @Override
  public String getMatch() {
    return mMatch;
  }

  @Override
  public String getDatabasePaginationInvokeName() {
    return mDatabasePaginationInvokeName;
  }

  @Override
  public PageControlsPosition getPageControlsPosition() {
    return mPageControlsPosition;
  }

  @Override
  public PagerSetup getDOMPagerSetup() {
    return mDOMPagerSetup;
  }
}
