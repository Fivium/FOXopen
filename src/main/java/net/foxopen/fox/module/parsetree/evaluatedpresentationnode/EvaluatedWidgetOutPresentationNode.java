package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
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
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.WidgetOutPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Store an EvaluatedNodeInfo object based off the match attribute
 */
public class EvaluatedWidgetOutPresentationNode extends GenericAttributesEvaluatedPresentationNode<WidgetOutPresentationNode> {
  private EvaluatedNode mEvaluatedNode;
  private boolean mShowPrompt = false;
  private boolean mShowWidget = false;
  private boolean mShowError = false;
  private boolean mShowHint = false;
  private boolean mShowDescription = false;

  public static List<EvaluatedPresentationNode<? extends PresentationNode>> evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, WidgetOutPresentationNode pOriginalPresentationNode,
                                                                                    EvaluatedParseTree pEvalParseTree, DOM pEvalContext) {

    String lMatchXPath = pOriginalPresentationNode.getMatch();

    Track.pushDebug("EvaluatedWidgetOutPresentationNode", lMatchXPath);
    try {
      // Get a DOMList for the match attribute supplied
      DOMList lMatchedDataList;
      try {
        lMatchedDataList = pEvalParseTree.getContextUElem().extendedXPathUL(pEvalContext, lMatchXPath);
      }
      catch (ExActionFailed x) {
        throw new ExInternal("Bad set-out match expression: " + lMatchXPath, x);
      }

      // Show a dev toolbar warning when a set-out matches no nodes
      if (lMatchedDataList.size() < 1) {
        Track.debug("WidgetOutMatchedNothing", "fm:widget-out found with no matching elements, " + pOriginalPresentationNode.toString(), TrackFlag.PARSE_TREE_WARNING);
        return Collections.emptyList();
      }
      else if (lMatchedDataList.size() > 1) {
        throw new ExInternal("fm:widget-out match XPath should match only 1 element but matched " + lMatchedDataList.size() + " elements");
      }

      // Get the single matched item
      DOM lMatchedDataItem = lMatchedDataList.get(0);

      // Locate node info for the current display item
      NodeInfo lMatchedItemNodeInfo = pEvalParseTree.getModule().getNodeInfo(lMatchedDataItem);
      if (lMatchedItemNodeInfo == null) {
        throw new ExInternal("fm:widget-out matched an element that doesn't appear in the schema: " + lMatchXPath);
      }

      ArrayList<EvaluatedPresentationNode<? extends PresentationNode>> lResultNode = new ArrayList<>(0);
      lResultNode.add(new EvaluatedWidgetOutPresentationNode(pParent, pOriginalPresentationNode, pEvalParseTree, pEvalContext, lMatchedDataItem, lMatchedItemNodeInfo));
      return lResultNode;

    }
    finally {
      Track.pop("EvaluatedWidgetOutPresentationNode");
    }
  }

  /**
   * Construct an EvaluatedNodeInfo object from the DOM node that the match attribute points to
   *
   * @see EvaluatedPresentationNode#EvaluatedPresentationNode
   */
  private EvaluatedWidgetOutPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, WidgetOutPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext
  , DOM pMatchedDataItem, NodeInfo pMatchedItemNodeInfo) {
    super(pParent, pOriginalPresentationNode, pEvalContext);
    try {
      mShowPrompt = pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(getEvalContext(), pOriginalPresentationNode.getShowPromptXPath());
      mShowWidget = pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(getEvalContext(), pOriginalPresentationNode.getShowWidgetXPath());
      mShowError = pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(getEvalContext(), pOriginalPresentationNode.getShowErrorXPath());
      mShowHint = pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(getEvalContext(), pOriginalPresentationNode.getShowHintXPath());
      mShowDescription = pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(getEvalContext(), pOriginalPresentationNode.getShowDescriptionXPath());
    }
    catch (ExActionFailed e) {
      throw e.toUnexpected("Running boolean XPath on fm:widget-out failed");
    }

    if (!mShowPrompt && !mShowWidget && !mShowError && !mShowHint && !mShowDescription) {
      throw new ExInternal("fm:widget-out found but no attributes to specify which facets to show");
    }

    // Determine current nodes evaluate context - for COMPLEX elements this is self, for SIMPLE its immediate parent
    DOM lEvaluateContextRuleItem;
    if (pMatchedItemNodeInfo.getNodeType() == NodeType.ITEM) {
      lEvaluateContextRuleItem = pMatchedDataItem.getParentOrNull();
      if (lEvaluateContextRuleItem == null) {
        throw new ExInternal("Error determining current nodes evaluate context: " + pMatchedDataItem.absolute());
      }
    }
    else {
      lEvaluateContextRuleItem = pMatchedDataItem;
    }

    // Make the Evaluated Node Info object
    Track.pushDebug("ConstructingEvalNodeInfo", pMatchedItemNodeInfo.getName());
    EvaluatedNodeInfo lEvaluatedNodeInfo;
    try {
      NodeEvaluationContext lNodeInfoEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(pEvaluatedParseTree, this, pMatchedDataItem, lEvaluateContextRuleItem, null, pMatchedItemNodeInfo.getNamespaceAttributeTable(), null);
      lEvaluatedNodeInfo = EvaluatedNodeFactory.createEvaluatedNodeInfo(null, this, lNodeInfoEvaluationContext, pMatchedItemNodeInfo);
    }
    finally {
      Track.pop("ConstructingEvalNodeInfo");
    }

    // Record it if it's visible
    if (lEvaluatedNodeInfo != null && lEvaluatedNodeInfo.getFieldMgr().getVisibility() != NodeVisibility.DENIED) {
      mEvaluatedNode = lEvaluatedNodeInfo;
    }
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  /**
   * Get the EvaluatedNodeInfo object found from the match attribute
   * @return EvaluatedNodeInfo
   */
  public EvaluatedNode getEvaluatedNode() {
    return mEvaluatedNode;
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.WIDGET_OUT;
  }

  public boolean isShowPrompt() {
    return mShowPrompt;
  }

  public boolean isShowWidget() {
    return mShowWidget;
  }

  public boolean isShowError() {
    return mShowError;
  }

  public boolean isShowHint() {
    return mShowHint;
  }

  public boolean isShowDescription() {
    return mShowDescription;
  }
}
