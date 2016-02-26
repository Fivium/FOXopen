package net.foxopen.fox.module.datanode;


import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.MandatoryDisplayOption;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetType;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class EvaluatedNodeInfoCollection extends EvaluatedNodeInfoGeneric {
  private final Map<NodeInfo, EvaluatedNodeInfo> mChildren = new LinkedHashMap<>();
  private final String mExternalFoxId;

  public EvaluatedNodeInfoCollection(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    mExternalFoxId = getEvaluatedParseTree().getFieldSet().getExternalFoxId(pNodeEvaluationContext.getDataItem());

    registerCollapsibleColumnStatus();

    addChildren();
  }

  void addChildren() {
    Map<String, EvaluatedNodeInfoCellMateCollection> lCellMateMap = new HashMap<>();

    // loop through nodeinfo item list constructing evaluated model dom nodes
    DOMList lChildren = getNodeInfo().getModelDOMElem().getChildElements();

    for (DOM lModelDomChild : lChildren) {
      NodeInfo lChildNodeInfo = getEvaluatedParseTree().getModule().getNodeInfo(lModelDomChild);

      DOM lSubDataDOM;
      try {
        lSubDataDOM = getDataItem().get1E(lModelDomChild.getName());
      }
      catch (ExCardinality e) {
        if ("phantom".equals(lChildNodeInfo.getDataType())) {
          lSubDataDOM = getDataItem();
        }
        else {
          // If there's no matching data item in the DOM and it's not a phantom, skip it
          continue;
        }
      }

      NAMESPACE_CHECK_LOOP: for (String lNameSpace : getNamespacePrecedenceList()) {
        if (lChildNodeInfo.getNamespaceExists(lNameSpace)) {

          // Determine current nodes evaluate context - for COMPLEX elements this is self, for SIMPLE (or complex for multi-selectors) its immediate parent
          // TODO - NP - should use NodeEvaluationContext#establishEvaluateContextRuleNode
          DOM lEvaluateContextRuleDOM = lSubDataDOM;
          if (lChildNodeInfo.getNodeType() == NodeType.ITEM || lChildNodeInfo.isMultiOptionItem()) {
            lEvaluateContextRuleDOM = getDataItem();
            if (lEvaluateContextRuleDOM == null) {
              throw new ExInternal("Error determining current nodes evaluate context: " + lSubDataDOM.absolute());
            }
          }

          NodeEvaluationContext lNodeInfoEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(getEvaluatedParseTree(), getEvaluatedPresentationNode(), lSubDataDOM, lEvaluateContextRuleDOM, null, lChildNodeInfo.getNamespaceAttributeTable(), this.getNamespacePrecedenceList(), getNodeEvaluationContext());
          EvaluatedNodeInfo lEvaluatedNodeInfo = EvaluatedNodeFactory.createEvaluatedNodeInfo(this, getEvaluatedPresentationNode(), lNodeInfoEvaluationContext, lChildNodeInfo);
          if (lEvaluatedNodeInfo != null && lEvaluatedNodeInfo.getVisibility() != NodeVisibility.DENIED) {
            String lCellmateKey = lEvaluatedNodeInfo.getStringAttribute(NodeAttribute.CELLMATE_KEY);
            // If the new child is part of a cell mates group, add it to a cell mates collection object
            if (lCellmateKey != null && !(lEvaluatedNodeInfo instanceof EvaluatedNodeInfoCellMateCollection)) {
              EvaluatedNodeInfoCellMateCollection lCellMateCollection = lCellMateMap.get(lCellmateKey);
              // If there's no existing cell mates collection for this cell mates key, create one
              if (lCellMateCollection == null) {
                lCellMateCollection = EvaluatedNodeInfoCellMateCollection.createEvaluatedNodeInfoCellMateCollection(lEvaluatedNodeInfo);
                lCellMateMap.put(lCellmateKey, lCellMateCollection);
                addChild(lCellMateCollection);
              }

              // Add the new child to the cellmate collection
              lCellMateCollection.addChild(lEvaluatedNodeInfo);
            }
            else {
              addChild(lEvaluatedNodeInfo);
            }
            break NAMESPACE_CHECK_LOOP;
          }
        }
      }
    }
  }

  @Override
  protected WidgetType getWidgetType() {
    if (getParent() != null && getParent().getWidgetBuilderType() == WidgetBuilderType.LIST) {
      return WidgetType.fromBuilderType(WidgetBuilderType.UNIMPLEMENTED); //TODO PN - rationalise (ENI expecting non-null result) (NP: This is a row, which is a "collection" of row "items")
    }
    else {
      return WidgetType.fromBuilderType(WidgetBuilderType.FORM);
    }
  }

  @Override
  public String getExternalFoxId() {
    return mExternalFoxId;
  }

  @Override
  public FieldMgr getFieldMgr() {
    throw new ExInternal(getClass().getName() + " cannot provide a FieldMgr - only applicable to items, actions and cellmates");
  }

  /**
   * There's no field for a collection
   *
   * @return
   */
  @Override
  public String getExternalFieldName() {
    return null;
  }

  /**
   * Collections don't have any data to be mandatory/optional about
   * @return MandatoryDisplayOption.NONE
   */
  @Override
  public MandatoryDisplayOption getMandatoryDisplay() {
    return MandatoryDisplayOption.NONE;
  }

  public List<EvaluatedNodeInfo> getChildren() {
    List<EvaluatedNodeInfo> lChildList = new LinkedList<>();
    for (Map.Entry<NodeInfo, EvaluatedNodeInfo> lEntry : mChildren.entrySet()) {
      lChildList.add(lEntry.getValue());
    }
    return lChildList;
  }

  public Map<NodeInfo, EvaluatedNodeInfo> getChildrenMap() {
    return Collections.unmodifiableMap(mChildren);
  }

  public void addChild(EvaluatedNodeInfo pEvaluatedNode) {
    mChildren.put(pEvaluatedNode.getIdentifyingNodeInfo(), pEvaluatedNode);
  }
}
