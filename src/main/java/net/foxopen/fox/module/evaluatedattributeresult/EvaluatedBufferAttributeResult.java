package net.foxopen.fox.module.evaluatedattributeresult;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.PresentationAttribute;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeAttributeAssociations;
import net.foxopen.fox.module.datanode.NodeEvaluationContext;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.BufferPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;

public class EvaluatedBufferAttributeResult implements EvaluatedAttributeResult {
  private final EvaluatedPresentationNode<? extends PresentationNode> mEvaluatedBuffer;

  public EvaluatedBufferAttributeResult(NodeAttribute pNodeAttribute, PresentationAttribute pBufferNameAttribute, GenericAttributesEvaluatedPresentationNode<? extends PresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext) {
    try {
      // Find the buffer from the parse tree
      String lBufferName = pNodeEvaluationContext.getContextUElem().extendedStringOrXPathString(pBufferNameAttribute.getEvalContextRuleDOM(), pBufferNameAttribute.getValue());
      if(XFUtil.isNull(lBufferName)) {
        throw new ExInternal("Failed to resolve a non-null buffer name on node");
      }

      BufferPresentationNode lBuffer = pNodeEvaluationContext.getEvaluatedParseTree().getBuffer(lBufferName);

      // Get an attach point via attribute or regular :{attach}
      DOM lAttachContext = pNodeEvaluationContext.getEvaluateContextRuleItem();

      DOMAttributeResult lBufferAttachAttribute = pNodeEvaluationContext.getDOMAttributeOrNull(NodeAttributeAssociations.ASSOCIATED_ATTRIBUTES.get(pNodeAttribute));
      if (lBufferAttachAttribute != null && lBufferAttachAttribute.getDOM() != null) {
        lAttachContext = lBufferAttachAttribute.getDOM();
      }

      // Push current data item on to stack for possible nested fm:labels to reference
      pNodeEvaluationContext.getEvaluatedParseTree().pushCurrentBufferLabelTargetElement(pNodeEvaluationContext.getDataItem());
      try {
        // Evaluate the buffer and its children
        mEvaluatedBuffer = pNodeEvaluationContext.getEvaluatedParseTree().evaluateNode(pEvaluatedPresentationNode, lBuffer, lAttachContext);
      }
      finally {
        pNodeEvaluationContext.getEvaluatedParseTree().popCurrentBufferLabelTargetElement(pNodeEvaluationContext.getDataItem());
      }
    }
    catch (ExModule | ExActionFailed ex) {
      throw ex.toUnexpected("Buffer attribute failed on: ");
    }
  }

  public EvaluatedPresentationNode<? extends PresentationNode> getEvaluatedBuffer() {
    return mEvaluatedBuffer;
  }
}
