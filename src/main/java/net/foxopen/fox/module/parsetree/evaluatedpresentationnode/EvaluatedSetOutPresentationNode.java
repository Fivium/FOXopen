package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.paging.DOMPager;
import net.foxopen.fox.dom.paging.Pager;
import net.foxopen.fox.dom.paging.PagerProvider;
import net.foxopen.fox.dom.paging.PagerSetup;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeFactory;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeEvaluationContext;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.module.datanode.NodeType;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.behaviour.DOMPagerBehaviour;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.SetOutInfoProvider;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Evaluated version of a set out node, mapped to a single match node. One set-out command may produce multiple evaluated
 * nodes if it matches multiple items.
 */
public class EvaluatedSetOutPresentationNode
extends GenericAttributesEvaluatedPresentationNode<GenericAttributesPresentationNode>
implements DOMPagerBehaviour {

  //May be null if visibility rules prevent the setout command seeing anything
  private final EvaluatedNodeInfo mChildEvaluatedNodeInfo;

  // Nullable
  private final DOMPager mDOMPager;

  public static <T extends GenericAttributesPresentationNode & SetOutInfoProvider> List<EvaluatedPresentationNode<? extends PresentationNode>> evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, T pOriginalPresentationNode,
                                                                                                           EvaluatedParseTree pEvalParseTree, DOM pEvalContext) {

    String lMatchXPath = pOriginalPresentationNode.getMatch();
    if(XFUtil.isNull(lMatchXPath)) {
      //Replicates legacy behaviour
      lMatchXPath = ".";
    }

    Track.pushDebug("EvaluatedSetOutPresentationNode", lMatchXPath);
    try {
      // Get a DOMList for the match attribute supplied
      DOMList lMatchedDataList;
      try {
        lMatchedDataList = pEvalParseTree.getContextUElem().extendedXPathUL(pEvalContext, lMatchXPath);
      }
      catch(ExActionFailed x) {
       throw new ExInternal("Bad set-out match expression: " + lMatchXPath, x);
      }

      // Show a dev toolbar warning when a set-out matches no nodes
      if (lMatchedDataList.size() < 1) {
        Track.debug("SetOutMatchedNothing", "fm:set-out found with no matching elements, " + pOriginalPresentationNode.toString(), TrackFlag.PARSE_TREE_WARNING);
        return Collections.emptyList();
      }

      List<EvaluatedPresentationNode<? extends PresentationNode>> lResultEvalNodes = new ArrayList<>(lMatchedDataList.size());

      // For each matched DOM, find out the Node Info, set the evaluate context and get an Evaluated Node Info
      for (DOM lMatchedDataItem : lMatchedDataList) {

        //Establish a pager type based on setout markup, and resolve to a pager if defined
        //Note: an fm:set-out node may have either a database pager or a DOM pager defined on it.
        Pager lPager;
        DOMPager lDOMPager = null; //For DOM pagers we need to pass the pager to the evaluated node
        PagerSetup lDOMPagerSetup = pOriginalPresentationNode.getDOMPagerSetup();
        String lDBPaginationInvokeName = pOriginalPresentationNode.getDatabasePaginationInvokeName();
        if(!XFUtil.isNull(lDBPaginationInvokeName)) {
          lPager = pEvalParseTree.getModuleFacetProvider(PagerProvider.class).getPagerOrNull(lDBPaginationInvokeName, lMatchedDataItem.getFoxId());
          if(lPager == null) {
            Track.info("PagerNotFound", "set-out: Pager not found matching invoke name " + lDBPaginationInvokeName + ", match id " + lMatchedDataItem.getFoxId(), TrackFlag.PARSE_TREE_WARNING);
          }
        }
        else if(lDOMPagerSetup != null) {
          lDOMPager = pEvalParseTree.getModuleFacetProvider(PagerProvider.class).getOrCreateDOMPager(lDOMPagerSetup.evalute(pEvalParseTree.getRequestContext(), lMatchedDataItem.getFoxId()));
          lPager = lDOMPager;
        }
        else {
          lPager = null;
        }

        EvaluatedSetOutPresentationNode lEvalSetOut = new EvaluatedSetOutPresentationNode(pParent, pOriginalPresentationNode, pEvalParseTree, pEvalContext, lMatchedDataItem, lDOMPager);

        EvaluatedPresentationNode<? extends PresentationNode> lEvalPageControl = null;
        if(lEvalSetOut.mChildEvaluatedNodeInfo != null && lPager != null) {
          lEvalPageControl = new EvaluatedPagerControlPresentationNode(lEvalSetOut, pOriginalPresentationNode, pEvalParseTree, pEvalContext, lPager);
        }

        //Add page controls above
        if(lEvalPageControl != null && pOriginalPresentationNode.getPageControlsPosition().isAbove()) {
          lResultEvalNodes.add(lEvalPageControl);
        }

        //Add evaluated node
        if(lEvalSetOut.mChildEvaluatedNodeInfo != null) {
          lResultEvalNodes.add(lEvalSetOut);
        }

        //Add page controls below
        if(lEvalPageControl != null && pOriginalPresentationNode.getPageControlsPosition().isBelow()) {
          lResultEvalNodes.add(lEvalPageControl);
        }
      }

      return lResultEvalNodes;
    }
    finally {
      Track.pop("EvaluatedSetOutPresentationNode");
    }
  }

  /**
   * Construct a list of EvaluatedNodeInfo objects from DOM nodes that the match attribute points to
   *
   * @see EvaluatedPresentationNode#EvaluatedPresentationNode
   */
  private <T extends GenericAttributesPresentationNode & SetOutInfoProvider> EvaluatedSetOutPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, T pOriginalPresentationNode,
                                                                                            EvaluatedParseTree pEvalParseTree, DOM pEvalContext, DOM pMatchedDataItem,
                                                                                            DOMPager pOptionalDOMPager) {
    super(pParent, pOriginalPresentationNode, pEvalContext);
    mDOMPager = pOptionalDOMPager;

    NodeInfo lMatchedItemNodeInfo = pEvalParseTree.getModule().getNodeInfo(pMatchedDataItem);
    if (lMatchedItemNodeInfo == null) {
      mChildEvaluatedNodeInfo = null;
      return;
    }

    // Determine current nodes evaluate context - for COMPLEX elements this is self, for SIMPLE its immediate parent
    DOM lEvaluateContextRuleItem;
    if (lMatchedItemNodeInfo.getNodeType() == NodeType.ITEM) {
      lEvaluateContextRuleItem = pMatchedDataItem.getParentOrNull();
      if (lEvaluateContextRuleItem == null) {
        throw new ExInternal("Error determining current nodes evaluate context: " + pMatchedDataItem.absolute());
      }
    }
    else {
      lEvaluateContextRuleItem = pMatchedDataItem;
    }

    // Make the Evaluated Node Info object
    EvaluatedNodeInfo lEvaluatedNodeInfo;
    Track.pushDebug("ConstructingEvalNode", lMatchedItemNodeInfo.getName());
    try {
      NodeEvaluationContext lNodeInfoEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(pEvalParseTree, this, pMatchedDataItem, lEvaluateContextRuleItem, null, lMatchedItemNodeInfo.getNamespaceAttributeTable(), null);
      lEvaluatedNodeInfo = EvaluatedNodeFactory.createEvaluatedNodeInfo(null, this, lNodeInfoEvaluationContext, lMatchedItemNodeInfo);
    }
    finally {
      Track.pop("ConstructingEvalNode");
    }

    // Add it to the Evaluated Node Info list
    if (lEvaluatedNodeInfo != null && lEvaluatedNodeInfo.getVisibility() != NodeVisibility.DENIED) {
      mChildEvaluatedNodeInfo = lEvaluatedNodeInfo;
    }
    else {
      mChildEvaluatedNodeInfo = null;
    }
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  public EvaluatedNode getChildEvaluatedNodeOrNull() {
    return mChildEvaluatedNodeInfo;
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.SET_OUT;
  }

  @Override
  public DOMPager getDOMPagerOrNull() {
    return mDOMPager;
  }
}
