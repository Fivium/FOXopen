package net.foxopen.fox.module.datanode;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.paging.DOMPager;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.DisplayOrder;
import net.foxopen.fox.module.MandatoryDisplayOption;
import net.foxopen.fox.module.evaluatedattributeresult.BooleanAttributeResult;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.behaviour.DOMPagerBehaviour;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetType;
import net.foxopen.fox.track.Track;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Attempt to evaluate data when marked up with list cardinality in the schema
 */
public class EvaluatedNodeInfoList extends EvaluatedNodeInfoGeneric {
  private final List<EvaluatedNodeInfo> mChildren = new LinkedList<>();

  private final List<EvaluatedNodeInfo> mColumns;
  private final Set<NodeInfo> mNonCollapsibleColumns = new HashSet<>();

  private final String mExternalFoxId;
  private final boolean mIsNestedList;

  public EvaluatedNodeInfoList(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    // FOX4 and below did not support lists in forms, FOX5 has NodeAttribute.ENABLE_LISTS_IN_FORMS to enable this new behaviour
    if (checkAncestry(WidgetBuilderType.FORM)) {
      BooleanAttributeResult lEnableListsInForms = getBooleanAttributeOrNull(NodeAttribute.ENABLE_LISTS_IN_FORMS);
      if (lEnableListsInForms != null && lEnableListsInForms.getBoolean()) {
        mIsNestedList = true;
        Track.debug("NestedList-ENABLED", "Found a list nested in a form: " + getIdentityInformation());
      }
      else {
        mIsNestedList = false;
        super.setVisibility(NodeVisibility.DENIED);
        Track.debug("NestedList-DISABLED", "Found a list nested in a form: " + getIdentityInformation());
      }
    }
    else {
      mIsNestedList = false;
    }

    mExternalFoxId = getEvaluatedParseTree().getFieldSet().getExternalFoxId(pNodeEvaluationContext.getDataItem());

    // Use the model dom to find all defined columns
    DOMList lModelDOMListChildren = pNodeInfo.getModelDOMElem().getChildElements().get(0).getChildElements();

    List<EvaluatedNodeInfo> lDefinedColumns = new LinkedList<>();
    Track.pushDebug("ListEvalNodeInfo", "Getting a list of defined columns");

    try {
      // Go into the list data and use the first "row" as the context when evaluating columns to find out defined columns
      DOMList lContainerChildren = getNodeEvaluationContext().getDataItem().getChildElements();
      DOM lContainerDOM = null;
      if (lContainerChildren != null && lContainerChildren.size() > 0) {
        lContainerDOM = lContainerChildren.get(0);
      }

      for (DOM lModelDOMItem : lModelDOMListChildren) {
        NodeInfo lChildNodeInfo = super.getEvaluatedParseTree().getModule().getNodeInfo(lModelDOMItem);
        NAMESPACE_CHECK_LOOP:
        for (String lNameSpace : super.getNamespacePrecedenceList()) {
          if (lChildNodeInfo.getNamespaceExists(lNameSpace)) {
            DOM lItemDOM = null;
            if (lContainerDOM != null) {
              // For phantoms the "item" (aka data element) is the containing element
              if ("phantom".equals(lChildNodeInfo.getDataType())) {
                lItemDOM = lContainerDOM;
              }
              else {
                lItemDOM = lContainerDOM.get1EOrNull(lChildNodeInfo.getName());
              }
            }

            // Construct NodeEvaluationContext for the column
            NodeEvaluationContext lNodeInfoEvaluationContext = getNodeEvaluationContext();
            if (lItemDOM != null && lContainerDOM != null) {
              lNodeInfoEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(getEvaluatedParseTree(),
                getEvaluatedPresentationNode(), lItemDOM, lContainerDOM, null, lChildNodeInfo.getNamespaceAttributeTable(),
                this.getNamespacePrecedenceList(), this.getNodeEvaluationContext());
            }
            else {
              lNodeInfoEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(getEvaluatedParseTree(),
                getEvaluatedPresentationNode(), lNodeInfoEvaluationContext.getDataItem(),
                lNodeInfoEvaluationContext.getEvaluateContextRuleItem(), null, lChildNodeInfo.getNamespaceAttributeTable(),
                this.getNamespacePrecedenceList(), this.getNodeEvaluationContext());
            }

            //  EvaluatedNodeInfo for the column
            EvaluatedNodeInfoStub lEvaluatedNodeInfoStub = EvaluatedNodeFactory.createEvaluatedNodeInfoStub(this,
              getEvaluatedPresentationNode(), lNodeInfoEvaluationContext, lChildNodeInfo);

            // Add it to the list of defined columns if it has an ro/edit defined that could potentially be turned on
            if (isColumnEditOrRo(lNodeInfoEvaluationContext)) {
              lDefinedColumns.add(lEvaluatedNodeInfoStub);
            }
            break NAMESPACE_CHECK_LOOP;
          }
        }
      }
      DisplayOrder.sort(lDefinedColumns);

      // Evaluate and add all the children of the list (the rows)
      addChildren();

      if (getBooleanAttribute(NodeAttribute.COLLAPSE_COLUMNS, false)) {
        // Collapse columns is enabled for the list, only retain defined columns that have been registered non-collapsible
        mColumns = lDefinedColumns
          .stream()
          .filter(pEvalNodeColumn -> mNonCollapsibleColumns.contains(pEvalNodeColumn.getNodeInfo()))
          .collect(Collectors.toList());
      }
      else {
        mColumns = lDefinedColumns;
      }
    }
    finally {
      Track.pop("ListEvalNodeInfo");
    }
  }

  /**
   * Test if a NodeEvaluationContext has an edit or ro attribute defined on it within the current list of enabled
   * namespaces (for the list), not caring what the attributes evaluate to
   *
   * @param pColumnContext Context for a column node
   * @return True if the column has an edit or ro attribute defined on it within the current list of enabled namespaces
   */
  private boolean isColumnEditOrRo(NodeEvaluationContext pColumnContext) {
    for (String lListNamespaceItem : super.getNamespacePrecedenceList()) {
      if (pColumnContext.hasNodeAttribute(lListNamespaceItem, NamespaceFunctionAttribute.RO.getExternalString())
        || pColumnContext.hasNodeAttribute(lListNamespaceItem, NamespaceFunctionAttribute.EDIT.getExternalString())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Test to see if a column was marked as non-collapsible
   *
   * @param pNodeInfo NodeInfo for a potentially non-collapsible column
   * @return
   */
  private boolean isNonCollapsibleColumn(NodeInfo pNodeInfo) {
    return mNonCollapsibleColumns.contains(pNodeInfo);
  }

  /**
   * This method should be called after constructing an EvaluatedNodeInfoList to potentially swap it out with an empty
   * list buffer if the list is empty and one is defined.
   *
   * @param pParent
   * @param pEvaluatedPresentationNode
   * @param pNodeEvaluationContext
   * @param pNodeVisibility
   * @param pNodeInfo
   * @return The EvaluatedNodeInfoList or an EvaluatedNodeInfoPhantomBufferItem for the EmptyListBuffer attribute
   */
  public static EvaluatedNodeInfo getListOrEmptyBuffer(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    BooleanAttributeResult lForceVisible = pNodeEvaluationContext.getBooleanAttributeOrNull(NodeAttribute.FORCE_VISIBLE);
    if (pNodeEvaluationContext.getDataItem().getChildElements().getLength() == 0 && lForceVisible != null && !lForceVisible.getBoolean()) {
      // If the list will have no rows and forceVisible is defined and set to false we can return null to not show the list headings
      return null;
    }
    else {
      EvaluatedNodeInfoList lEvaluatedNodeList = new EvaluatedNodeInfoList(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

      if (lEvaluatedNodeList.getChildren().size() == 0 && !XFUtil.isNull(lEvaluatedNodeList.getStringAttribute(NodeAttribute.EMPTY_LIST_BUFFER))) {
        return new EvaluatedNodeInfoPhantomBufferItem(  pParent
                                                      , pEvaluatedPresentationNode
                                                      , pNodeEvaluationContext
                                                      , pNodeVisibility
                                                      , pNodeInfo
                                                      , NodeAttribute.EMPTY_LIST_BUFFER
                                                      , NodeAttribute.EMPTY_LIST_BUFFER_ATTACH_DOM);
      }

      return lEvaluatedNodeList;
    }
  }

  /**
   * Loop through the data elements below this item, evaluate them and add them as children of this object in the EvaluatedNode tree
   */
  void addChildren() {
    DOMList lChildren = getDataItem().getChildElements();

    //If a DOM pager was specified on the SetOutPresentationNode, use it to filter the list.
    //NOTE: this relies on this ENI being created from an EvaluatedSetOutPresentationNode
    DOMPager lDOMPager = getEPNBehaviour(DOMPagerBehaviour.class).getDOMPagerOrNull();
    if(lDOMPager != null) {
      lChildren = lDOMPager.trimDOMListForCurrentPage(getEvaluatedParseTree().getRequestContext(), lChildren);
    }

    for (DOM lChild : lChildren) {
      NodeInfo lChildNodeInfo = getEvaluatedParseTree().getModule().getNodeInfo(lChild);
      if (lChildNodeInfo != null) {
        NAMESPACE_CHECK_LOOP: for (String lNameSpace : getNamespacePrecedenceList()) {
          if (lChildNodeInfo.getNamespaceExists(lNameSpace)) {
            // Context changed to the record item under list
            NodeEvaluationContext lNodeEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(getEvaluatedParseTree(), getEvaluatedPresentationNode(), lChild, lChild, null, lChildNodeInfo.getNamespaceAttributeTable(), this.getNamespacePrecedenceList(), this.getNodeEvaluationContext());
            EvaluatedNodeInfo lEvaluatedNodeInfo = EvaluatedNodeFactory.createEvaluatedNodeInfo(this, getEvaluatedPresentationNode(), lNodeEvaluationContext, lChildNodeInfo);
            if (lEvaluatedNodeInfo != null && lEvaluatedNodeInfo.getVisibility() != NodeVisibility.DENIED) {
              addChild(lEvaluatedNodeInfo);
              break NAMESPACE_CHECK_LOOP;
            }
          }
        }
      }
    }
  }

  /**
   * Get the ordered list of list columns
   *
   * @return Ordered list columns
   */
  public List<EvaluatedNodeInfo> getColumns() {
    return Collections.unmodifiableList(mColumns);
  }

  /**
   * Items (columns) in a row can add their NodeInfo to their list to mark it as actually existing when there's content
   * or phantom information to display. This is used to hide "empty" columns when NodeAttribute.COLLAPSE_COLUMNS is enabled
   *
   * @param pNodeInfo NodeInfo object for the column that should be marked as non-collapsible
   */
  public void registerNonCollapsibleColumn(NodeInfo pNodeInfo) {
    mNonCollapsibleColumns.add(pNodeInfo);
  }

  @Override
  protected WidgetType getWidgetType() {
    WidgetType lWidgetType;
    if (getBooleanAttribute(NodeAttribute.FORM_LIST, false)) {
      lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.FORM_LIST);
    }
    else {
      lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.LIST);
    }
    return lWidgetType;
  }

  @Override
  public FieldMgr getFieldMgr() {
    throw new ExInternal(getIdentityInformation() + " cannot provide a FieldMgr - only applicable to items, actions and cellmates");
  }

  @Override
  public String getExternalFieldName() {
    return null;
  }

  /**
   * Lists don't have any data to be mandatory/optional about
   * @return MandatoryDisplayOption.NONE
   */
  @Override
  public MandatoryDisplayOption getMandatoryDisplay() {
    return MandatoryDisplayOption.NONE;
  }

  @Override
  public String getExternalFoxId() {
    return mExternalFoxId;
  }

  @Override
  public List<EvaluatedNodeInfo> getChildren() {
    return mChildren;
  }

  @Override
  public void addChild(EvaluatedNodeInfo pEvaluatedNode) {
    mChildren.add(pEvaluatedNode);
  }

  /**
   * @return true when the list is nested in a form and nested lists in forms is enabled
   */
  public boolean isNestedList() {
    return mIsNestedList;
  }
}
