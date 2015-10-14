package net.foxopen.fox.module.datanode;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.ActionIdentifier;
import net.foxopen.fox.module.evaluatedattributeresult.BooleanAttributeResult;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

public class EvaluatedNodeFactory {

  /**
   * Create an EvaluatedNodeInfo for the type of NodeInfo passed in
   *
   * @param pParent
   * @param pEvaluatedPresentationNode
   * @param pNodeEvaluationContext
   * @param pNodeInfo
   * @return
   */
  public static EvaluatedNodeInfo createEvaluatedNodeInfo(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode,
                                                          NodeEvaluationContext pNodeEvaluationContext, NodeInfo pNodeInfo) {
    NodeVisibility lVisibility = getMaxVisibility(pNodeEvaluationContext, pNodeInfo.getNodeType());

    if (lVisibility == NodeVisibility.DENIED) {
      return null;
    }
    else {
      /**
       * In FOX4 a schema element could be complex but still set out as an individual element if it had a widget defined
       * that was enabled.
       * FOX5 relied on the schema driving the type of EvaluatedNodeInfo to create, but we have implemented this attribute
       * so that FOX4 migration is made easier and less code has to be re-written initially.
       */
      BooleanAttributeResult llDisplayFormAsItemBooleanAttribute = pNodeEvaluationContext.getBooleanAttributeOrNull(NodeAttribute.DISPLAY_FORM_AS_ITEM);
      boolean lDisplayFormAsItem = false;
      if (llDisplayFormAsItemBooleanAttribute != null && llDisplayFormAsItemBooleanAttribute.getBoolean()) {
        lDisplayFormAsItem = true;
        Track.info(NodeAttribute.DISPLAY_FORM_AS_ITEM.getExternalString(), "Found use of " + NodeAttribute.DISPLAY_FORM_AS_ITEM + ", this code should be refactored", TrackFlag.BAD_MARKUP);
      }

      if (pNodeInfo.getNodeType() == NodeType.LIST) {
        // Node is a list, but it is not necessarily a list of complex types - it could be a multi selector or multi upload container, in which case we need an Item node info
        if(pNodeInfo.isMultiOptionItem()) {
          // Create an item if a multi- type item
          return EvaluatedNodeInfoItem.getEvaluatedNode(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, lVisibility, pNodeInfo);
        }
        else {
          return EvaluatedNodeInfoList.getListOrEmptyBuffer(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, lVisibility, pNodeInfo);
        }
      }
      else if (pNodeInfo.getNodeType() == NodeType.COLLECTION && !lDisplayFormAsItem) {
        return new EvaluatedNodeInfoCollection(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, lVisibility, pNodeInfo);
      }
      else if (pNodeInfo.getNodeType() == NodeType.ITEM || lDisplayFormAsItem) {
        return EvaluatedNodeInfoItem.getEvaluatedNode(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, lVisibility, pNodeInfo);
      }
      else {
        throw new ExInternal("Don't know what type of EvaluatedNodeInfo to return based on the parameters passed: " + pNodeInfo.getNodeType());
      }
    }
  }

  /**
   * Create only an Action type EvaluatedNode
   *
   * @param pEvaluatedPresentationNode An attribute-providing EvaluatedPresentationNode  to make the EvaluatedNodeAction for
   * @param pNodeEvaluationContext A NodeEvaluationContext to get attributes from
   * @return
   */
  public static EvaluatedNodeAction createEvaluatedNodeAction(GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode,
                                                              NodeEvaluationContext pNodeEvaluationContext, ActionIdentifier pActionIdentifier) {
    NodeVisibility lVisibility = getMaxVisibility(pNodeEvaluationContext, NodeType.ACTION);

    if (lVisibility == NodeVisibility.DENIED) {
      return null;
    }
    else {
      return new EvaluatedNodeAction(pEvaluatedPresentationNode, pNodeEvaluationContext, lVisibility, pActionIdentifier);
    }
  }

  /**
   * Derives the Edit/ReadOnly visibility of a field
   *
   * @param pNodeEvaluationContext NodeEvaluationContext holding the view/mode lists
   * @param pNodeType Type of the current node, used to check if it was an action and ro was defaulted for a track message
   * @return NodeVisibility value of the visibility level
   * @throws ExInternal
   */
  private static NodeVisibility getMaxVisibility(NodeEvaluationContext pNodeEvaluationContext, NodeType pNodeType)
    throws ExInternal {
    // Compute visibility
    NodeVisibility lNodeVisibility;
    COMPUTE: {
      // Check modes to see if access has been given
      if (pNodeEvaluationContext.getModeList().size() != 0) {
        if (pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.MODE, NamespaceFunctionAttribute.EDIT).attributeValue()) {
          lNodeVisibility = NodeVisibility.EDIT;
          break COMPUTE;
        }
        else if (pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.MODE, NamespaceFunctionAttribute.RO).attributeValue()) {
          lNodeVisibility = NodeVisibility.VIEW;
          break COMPUTE;
        }
        else if (pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.MODE, NamespaceFunctionAttribute.RUN).attributeValue()
          && !pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.MODE, NamespaceFunctionAttribute.EDIT).attributeExists()
          && !pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.MODE, NamespaceFunctionAttribute.RO).attributeExists()
          && !pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.MODE, NamespaceFunctionAttribute.EDIT).attributeExists()
          && !pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.MODE, NamespaceFunctionAttribute.RO).attributeExists()) {
          if(pNodeType != NodeType.ACTION) {
            Track.info("RunElementNoEditRO", "No edit/ro, but a run attribute defaulted \"" + pNodeEvaluationContext.getDataItem().getName() + "\" to ro", TrackFlag.BAD_MARKUP);
          }
          lNodeVisibility = NodeVisibility.VIEW;
          break COMPUTE;
        }
      }

      // Check views to see if access has been given
      if (pNodeEvaluationContext.getViewList().size() != 0) {
        if(
          pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.VIEW, NamespaceFunctionAttribute.EDIT).attributeValue()
            || pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.VIEW, NamespaceFunctionAttribute.RO).attributeValue()
          ) {
          lNodeVisibility = NodeVisibility.VIEW;
          break COMPUTE;
        }
        else if (pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.VIEW, NamespaceFunctionAttribute.RUN).attributeValue()
          && !pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.VIEW, NamespaceFunctionAttribute.EDIT).attributeExists()
          && !pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.VIEW, NamespaceFunctionAttribute.RO).attributeExists()
          && !pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.VIEW, NamespaceFunctionAttribute.EDIT).attributeExists()
          && !pNodeEvaluationContext.checkCachedNamespaceListFunction(NodeEvaluationContext.NamespaceListType.VIEW, NamespaceFunctionAttribute.RO).attributeExists()) {
          if(pNodeType != NodeType.ACTION) {
            Track.info("RunElementNoEditRO", "No edit/ro, but a run attribute defaulted \"" + pNodeEvaluationContext.getDataItem().getName() + "\" to ro", TrackFlag.BAD_MARKUP);
          }
          lNodeVisibility = NodeVisibility.VIEW;
          break COMPUTE;
        }
      }

      // No modes or views
      lNodeVisibility = NodeVisibility.DENIED;

    } // end COMPUTE

    // Return computed result
    return lNodeVisibility;
  }
}
