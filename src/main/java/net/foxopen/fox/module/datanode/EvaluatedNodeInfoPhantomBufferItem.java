package net.foxopen.fox.module.datanode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.evaluatedattributeresult.DOMAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.EvaluatedBufferAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.BufferPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetType;

// TODO - NP - For Monday 22/09/2014 - Add in a method to construct this with arbitrary params
public class EvaluatedNodeInfoPhantomBufferItem
extends EvaluatedNodeInfoItem {
  private final EvaluatedPresentationNode<? extends PresentationNode> mEvaluatedNode;

  public EvaluatedNodeInfoPhantomBufferItem(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    // Get evaluated phantom buffer
    mEvaluatedNode = getEvaluatedBufferNode(pEvaluatedPresentationNode, pNodeEvaluationContext, NodeAttribute.PHANTOM_BUFFER, NodeAttribute.PHANTOM_BUFFER_ATTACH_DOM);
  }

  /**
   * Construct a Phantom Buffer ENI with parametrised buffer attributes and optional prompt
   *
   * @param pParent
   * @param pEvaluatedPresentationNode
   * @param pNodeEvaluationContext
   * @param pNodeVisibility
   * @param pNodeInfo
   * @param pBufferNameAttribute
   * @param pBufferAttachAttribute
   */
  public EvaluatedNodeInfoPhantomBufferItem(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo, NodeAttribute pBufferNameAttribute, NodeAttribute pBufferAttachAttribute) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    // Get evaluated buffer using given attributes
    mEvaluatedNode = getEvaluatedBufferNode(pEvaluatedPresentationNode, pNodeEvaluationContext, pBufferNameAttribute, pBufferAttachAttribute);
  }

  /**
   * Find and evaluate the buffer for the ENI
   *
   * @param pNodeEvaluationContext Node context to use when setting the buffer attach point
   * @param pBufferNameAttribute NodeAttribute to use for the buffer name
   * @param pBufferAttachAttribute NodeAttribute to use for an alternate buffer attach point
   * @return EvaluatedBufferNode
   */
  private EvaluatedPresentationNode<? extends PresentationNode> getEvaluatedBufferNode(EvaluatedPresentationNode pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeAttribute pBufferNameAttribute, NodeAttribute pBufferAttachAttribute) {
    try {
      // Find the buffer from the parse tree
      String lBufferName = getStringAttribute(pBufferNameAttribute);
      if(XFUtil.isNull(lBufferName)) {
        throw new ExInternal("Failed to resolve a non-null phantom buffer name on node " + getIdentityInformation());
      }

      BufferPresentationNode lBuffer = getEvaluatedParseTree().getBuffer(lBufferName);

      // Get an attach point via attribute or regular :{attach}
      DOM lAttachContext = pNodeEvaluationContext.getEvaluateContextRuleItem();

      DOMAttributeResult lBufferAttachAttribute = getDOMAttributeOrNull(pBufferAttachAttribute);
      if (lBufferAttachAttribute != null && lBufferAttachAttribute.getDOM() != null) {
        lAttachContext = lBufferAttachAttribute.getDOM();
      }

      // Evaluate the buffer and its children
      return getEvaluatedParseTree().evaluateNode(pEvaluatedPresentationNode, lBuffer, lAttachContext);
    }
    catch (ExModule ex) {
      throw ex.toUnexpected("Phantom buffer failed on: " + getIdentityInformation());
    }
  }

  public EvaluatedPresentationNode<? extends PresentationNode> getPhantomBuffer() {
    return mEvaluatedNode;
  }

  @Override
  public boolean hasPrompt() {
    // Phantom buffers don't have a prompt by default. If no NodeAttribute.PROMPT specified, or it's set to "" then no prompt.
    // If the attribute is defined as anything then the prompt appears normally
    StringAttributeResult lPrompt = getStringAttributeResultOrNull(NodeAttribute.PROMPT);
    EvaluatedBufferAttributeResult lPromptBuffer = getEvaluatedBufferAttributeOrNull(NodeAttribute.PROMPT_BUFFER);
    return (lPrompt != null && !XFUtil.isNull(lPrompt.getString())) || lPromptBuffer != null;
  }

  @Override
  protected WidgetType getWidgetType() {
    return WidgetType.fromBuilderType(WidgetBuilderType.PHANTOM_BUFFER);
  }
}
