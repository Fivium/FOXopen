package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
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

  /**
   * Construct an EvaluatedNodeInfo object from the DOM node that the match attribute points to
   *
   * @see EvaluatedPresentationNode#EvaluatedPresentationNode
   */
  public EvaluatedWidgetOutPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, WidgetOutPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    String lMatchXPath = pOriginalPresentationNode.getMatch();

    Track.pushDebug("EvaluatedWidgetOutPresentationNode", lMatchXPath);
    try {
      try {
        mShowPrompt = pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(getEvalContext(), pOriginalPresentationNode.getShowPromptXPath());
        mShowWidget = pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(getEvalContext(), pOriginalPresentationNode.getShowWidgetXPath());
        mShowError = pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(getEvalContext(), pOriginalPresentationNode.getShowErrorXPath());
        mShowHint = pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(getEvalContext(), pOriginalPresentationNode.getShowHintXPath());
        mShowDescription = pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(getEvalContext(), pOriginalPresentationNode.getShowDescriptionXPath());
      }
      catch (ExActionFailed e) {
        e.toUnexpected("Running boolean XPath on fm:widget-out failed");
      }

      if (!mShowPrompt && !mShowWidget && !mShowError && !mShowHint && !mShowDescription) {
        throw new ExInternal("fm:widget-out found but no attributes to specify which facets to show: " + lMatchXPath);
      }

      DOM lMatchedDataItem = null;
      try {
        lMatchedDataItem = pEvaluatedParseTree.getContextUElem().extendedXPath1E(getEvalContext(), lMatchXPath);
      }
      catch (ExCardinality | ExActionFailed e) {
        throw e.toUnexpected("fm:widget-out did not have a match attribute that matched one, and only one, element");
      }

      // Locate node info for the current display item
      NodeInfo lMatchedItemNodeInfo = pEvaluatedParseTree.getModule().getNodeInfo(lMatchedDataItem);
      if (lMatchedItemNodeInfo == null) {
        throw new ExInternal("fm:widget-out matched an element that doesn't appear in the schema: " + lMatchXPath);
      }

      // Determine current nodes evaluate context - for COMPLEX elements this is self, for SIMPLE its immediate parent
      DOM lEvaluateContextRuleItem;
      if (lMatchedItemNodeInfo.getNodeType() == NodeType.ITEM) {
        lEvaluateContextRuleItem = lMatchedDataItem.getParentOrNull();
        if (lEvaluateContextRuleItem == null) {
          throw new ExInternal("Error determining current nodes evaluate context: " + lMatchedDataItem.absolute());
        }
      }
      else {
        lEvaluateContextRuleItem = lMatchedDataItem;
      }

      // Make the Evaluated Node Info object
      Track.pushDebug("ConstructingEvalNodeInfo", lMatchedItemNodeInfo.getName());
      EvaluatedNodeInfo lEvaluatedNodeInfo;
      try {
        NodeEvaluationContext lNodeInfoEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(pEvaluatedParseTree, this, lMatchedDataItem, lEvaluateContextRuleItem, null, lMatchedItemNodeInfo.getNamespaceAttributeTable(), null);
        lEvaluatedNodeInfo = EvaluatedNodeFactory.createEvaluatedNodeInfo(null, this, lNodeInfoEvaluationContext, lMatchedItemNodeInfo);
      }
      finally {
        Track.pop("ConstructingEvalNodeInfo");
      }

      // Record it if it's visible
      if (lEvaluatedNodeInfo != null && lEvaluatedNodeInfo.getFieldMgr().getVisibility() != NodeVisibility.DENIED) {
        mEvaluatedNode = lEvaluatedNodeInfo;
      }
    }
    finally {
      Track.pop("EvaluatedWidgetOutPresentationNode");
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
