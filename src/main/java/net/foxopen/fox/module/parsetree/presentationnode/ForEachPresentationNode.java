package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.util.ForEachIterator;
import net.foxopen.fox.command.util.ForEachIterator.IterationExecutable;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.paging.DOMPager;
import net.foxopen.fox.dom.paging.PagerProvider;
import net.foxopen.fox.dom.paging.PagerSetup;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.PageControlsPosition;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedContainerPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.track.Track;

/**
 * This Presentation Node is for a fm:for-each element, holding its sub-elements in a ContainerPresentationNode for
 * evaluation time where it can evaluate the container multiple times for each loop iteration with the correct status
 * and contexts set.
 */
public class ForEachPresentationNode extends PresentationNode {
  private static final String DEFAULT_NUM_RANGE_FROM = "0";
  private static final String DEFAULT_NUM_RANGE_STEP = "1";
  private static final String DEFAULT_ITEM_CONTEXT_NAME = "loopitem";
  private static final String DEFAULT_STATUS_CONTEXT_NAME = "loopstatus";

  private final ForEachContainerPresentationNode mForEachContainer;

  private final String mXPath;

  private final Double mRangeFrom;
  private final Double mRangeTo;
  private final Double mRangeStep;

  private final String mItemContextName;
  private final String mStatusContextName;

  private final PagerSetup mPagerSetup;
  private final PageControlsPosition mPageControlsPosition;

  public ForEachPresentationNode(DOM pCurrentNode) {
    mXPath = pCurrentNode.getAttr("xpath");

    try {
      mRangeFrom = Double.parseDouble(XFUtil.nvl(pCurrentNode.getAttr("num-range-from"), DEFAULT_NUM_RANGE_FROM));
    }
    catch (NumberFormatException e) {
      throw new ExInternal("Bad number specified for fm:for-each 'num-range-from' attribute", e);
    }

    try {
      String lRangeTo = pCurrentNode.getAttr("num-range-to");
      if(!XFUtil.isNull(lRangeTo)) {
        mRangeTo = Double.parseDouble(lRangeTo);
      }
      else {
        mRangeTo = null;
      }
    }
    catch (NumberFormatException e) {
      throw new ExInternal("Bad number specified for fm:for-each 'num-range-to' attribute", e);
    }

    try {
      mRangeStep = Double.parseDouble(XFUtil.nvl(pCurrentNode.getAttr("num-range-step"), DEFAULT_NUM_RANGE_STEP));
    }
    catch (NumberFormatException e) {
      throw new ExInternal("Bad number specified for fm:for-each 'num-range-step' attribute", e);
    }

    mItemContextName = XFUtil.nvl(pCurrentNode.getAttr("itemContextName"), DEFAULT_ITEM_CONTEXT_NAME);
    mStatusContextName = XFUtil.nvl(pCurrentNode.getAttr("statusContextName"), DEFAULT_STATUS_CONTEXT_NAME);

    try {
      mPagerSetup = PagerSetup.fromDOMMarkupOrNull(pCurrentNode, "pagination-definition", "page-size", "pagination-invoke-name");
    }
    catch (ExModule e) {
      throw new ExInternal("Bad markup on for-each for XPath " + mXPath, e);
    }

    String lPageControlsPositionAttr = pCurrentNode.getAttr("page-controls-position");
    if(!XFUtil.isNull(lPageControlsPositionAttr)) {
      mPageControlsPosition = PageControlsPosition.fromString(lPageControlsPositionAttr);
    }
    else {
      //Default page control position is "none" for a for-each node
      mPageControlsPosition = PageControlsPosition.NONE;
    }

    if(XFUtil.isNull(mXPath) && mRangeTo == null) {
      throw new ExInternal("Invalid syntax for for-each presentation node: at least one of xpath or num-range-to attributes must be specified");
    }
    else if(mPagerSetup != null  && XFUtil.isNull(mXPath)) {
      //We can only create pagers over DOMs, so ensure an XPath is specified
      throw new ExInternal("Invalid syntax for for-each presentation node: xpath needs to be specified if a pagination declaration is provided");
    }

    mForEachContainer = new ForEachContainerPresentationNode(pCurrentNode);
    mForEachContainer.setDebugInfo("ForEachNode");

    // This type of node has its children set aside, so does not need to call ParseTree.parseDOMChildren()
  }

  public String toString() {
    return "ForEachNode: (" + (XFUtil.isNull(mXPath) ? "" : "xpath = '" + mXPath + "'") + (XFUtil.isNull(mRangeTo) ? "" : "from " + mRangeFrom + " to " + mRangeTo + " (step: " + mRangeStep + ")") + ")";
  }

  /**
   * This does not call out to another object, instead it constructs a new container object and evaluates the sub-elements
   * of the original fm:for-each into it multiple times, evaluating them with the updated contexts as it goes.
   *
   * @inheritDoc
   */
  public EvaluatedPresentationNode<? extends PresentationNode> evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, final EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    Track.pushDebug("EvaluatedForEachPresentationNode", toString());
    try {
      // Construct a container node with the content of this node (effectively replacing the for-each in the evaluated tree)
      final EvaluatedContainerPresentationNode lForEachContainer = mForEachContainer.evaluate(pParent, pEvaluatedParseTree, pEvalContext);

      DOMPager lPager = null;
      if(mXPath != null && mPagerSetup != null) {
        lPager = pEvaluatedParseTree.getModuleFacetProvider(PagerProvider.class).getOrCreateDOMPager(mPagerSetup.evalute(pEvaluatedParseTree.getRequestContext(), null));
      }

      if(lPager != null && mPageControlsPosition.isAbove()) {
        //TODO PN - improve EPN creation logic
        PresentationNode lPagerPN = new PagerControlPresentationNode(mPagerSetup.getInvokeName());
        EvaluatedPresentationNode<? extends PresentationNode> lPagerEPN = pEvaluatedParseTree.evaluateNode(lForEachContainer, lPagerPN, pEvalContext);
        lForEachContainer.addChild(lPagerEPN);
      }

      ContextUElem lContextUElem = pEvaluatedParseTree.getContextUElem();

      DOMList lXPathMatchedItems = null;
      if (!XFUtil.isNull(mXPath)) {
        try {
          lXPathMatchedItems = lContextUElem.extendedXPathUL(pEvalContext, mXPath);
        }
        catch (ExActionFailed e) {
          throw new ExInternal("Bad XPath " + mXPath + " specified in for-each presentation node", e);
        }

        //Apply paging to results
        if(lPager != null) {
          lXPathMatchedItems = lPager.trimDOMListForCurrentPage(pEvaluatedParseTree.getRequestContext(), lXPathMatchedItems);
        }
      }

      ForEachIterator lIterator = new ForEachIterator(!XFUtil.isNull(mXPath), mItemContextName, mStatusContextName, mRangeFrom, mRangeTo, mRangeStep);
      lIterator.doForEach(lContextUElem, lXPathMatchedItems, new IterationExecutable() {
        public boolean execute(DOM pOptionalCurrentItem, ForEachIterator.Status pIteratorStatus) {
          lForEachContainer.evaluateChildren(pEvaluatedParseTree);
          return true;
        }
      });

      if(lPager != null && mPageControlsPosition.isBelow()) {
        //TODO PN - improve EPN creation logic
        PresentationNode lPagerPN = new PagerControlPresentationNode(mPagerSetup.getInvokeName());
        EvaluatedPresentationNode<? extends PresentationNode> lPagerEPN = pEvaluatedParseTree.evaluateNode(lForEachContainer, lPagerPN, pEvalContext);
        lForEachContainer.addChild(lPagerEPN);
      }

      return lForEachContainer;
    }
    finally {
      Track.pop("EvaluatedForEachPresentationNode");
    }
  }

  /**
   * Extension of ContainerPresentationNode to make it evaluate to a ForEachEvaluatedContainerPresentationNode
   */
  static private class ForEachContainerPresentationNode extends ContainerPresentationNode {
    public ForEachContainerPresentationNode(DOM pSourceDOM) {
      super(pSourceDOM);
    }

    @Override
    public EvaluatedContainerPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
      return new ForEachEvaluatedContainerPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
    }
  }

  /**
   * Extension of EvaluatedContainerPresentationNode to set canEvaluateChildren == false so that we can repeatedly
   * evaluate the same container with different attach points.
   */
  static private class ForEachEvaluatedContainerPresentationNode extends EvaluatedContainerPresentationNode {
    public ForEachEvaluatedContainerPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                     ContainerPresentationNode pOriginalPresentationNode,
                                                     EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
      super(pParent, pOriginalPresentationNode, pEvaluatedParseTree, pEvalContext);
    }

    @Override
    public boolean canEvaluateChildren() {
      return false;
    }
  }
}
