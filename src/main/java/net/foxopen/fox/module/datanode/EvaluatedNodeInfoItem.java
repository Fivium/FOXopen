package net.foxopen.fox.module.datanode;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.evaluatedattributeresult.DOMAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetType;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;


public class EvaluatedNodeInfoItem extends EvaluatedNodeInfoGeneric {
  private final FieldMgr mFieldMgr;
  private final MapSet mMapSet;

  static EvaluatedNodeInfoItem getEvaluatedNode(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode,
                                                NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pVisibility, NodeInfo pNodeInfo) {
    EvaluatedNodeInfoItem lEvaluatedNodeInfoItem;
    StringAttributeResult lWidgetString = pNodeEvaluationContext.getStringAttributeOrNull(NodeAttribute.WIDGET);
    if(pNodeInfo.isUploadItem()) {
      lEvaluatedNodeInfoItem = new EvaluatedNodeInfoFileItem(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pVisibility, pNodeInfo);
    }
    else if("phantom".equals(pNodeInfo.getDataType()) && pNodeEvaluationContext.hasNodeAttribute(NodeAttribute.PHANTOM_BUFFER)) {
      lEvaluatedNodeInfoItem = new EvaluatedNodeInfoPhantomBufferItem(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pVisibility, pNodeInfo);
    }
    else if("phantom".equals(pNodeInfo.getDataType()) && pNodeEvaluationContext.hasNodeAttribute(NodeAttribute.PHANTOM_MENU_MODE)) {
      lEvaluatedNodeInfoItem = new EvaluatedNodeInfoPhantomMenuItem(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pVisibility, pNodeInfo);
    }
    else if("phantom".equals(pNodeInfo.getDataType()) && pNodeEvaluationContext.hasNodeAttribute(NodeAttribute.PHANTOM_DATA_XPATH)) {
      lEvaluatedNodeInfoItem = EvaluatedNodeInfoPhantomItem.createPhantomDataXPathENI(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pVisibility, pNodeInfo);
    }
    else if (lWidgetString != null && WidgetBuilderType.CARTOGRAPHIC.isMatchingWidget(lWidgetString.getString())) {
      lEvaluatedNodeInfoItem = new EvaluatedNodeInfoCartographicItem(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pVisibility, pNodeInfo);
    }
    else {
      lEvaluatedNodeInfoItem = new EvaluatedNodeInfoItem(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pVisibility, pNodeInfo);
    }

    return lEvaluatedNodeInfoItem;
  }

  protected EvaluatedNodeInfoItem(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    mMapSet = resolveMapSet();

    // If the widget is for an action but it's not runnable while not being a plus widget, knock it to denied visibility
    if (getWidgetBuilderType().isAction() && !XFUtil.isNull(getActionName()) && !isRunnable() && !isPlusWidget()) {
      setVisibility(NodeVisibility.DENIED);
    }

    // If this item is in a list, check it for content and mark the column up as containing content on the list
    if (getParent() instanceof EvaluatedNodeInfoCollection && getParent().getParent() instanceof EvaluatedNodeInfoList) {
      EvaluatedNodeInfoList lListContainer = (EvaluatedNodeInfoList)getParent().getParent();
      if ((isAttributeDefined(NodeAttribute.HAS_CONTENT) && getBooleanAttribute(NodeAttribute.HAS_CONTENT, false))
        || (!isAttributeDefined(NodeAttribute.HAS_CONTENT) && XFUtil.exists(pNodeEvaluationContext.getDataItem().value(true)))) {
        lListContainer.registerNonCollapsibleColumn(pNodeInfo);
      }
    }

    mFieldMgr = super.getEvaluatedParseTree().getFieldSet().createFieldMgr(this);
  }

  /**
   * Find the mapset based on the element itself or the selector attribute
   *
   * @return
   */
  private MapSet resolveMapSet() {
    String lMapSetName = getStringAttribute(NodeAttribute.MAPSET);
    if(lMapSetName != null && super.getNodeEvaluationContext().getDataItem() != null) {
      DOMAttributeResult lDOMAttributeResult = getDOMAttributeOrNull(NodeAttribute.MAPSET_ATTACH);
      return super.getEvaluatedParseTree().resolveMapSet(lMapSetName, super.getDataItem(), (lDOMAttributeResult == null ? null : lDOMAttributeResult.getDOM()));
    }
    else if(lMapSetName == null && !".".equals(getStringAttribute(NodeAttribute.SELECTOR,".")) && getSchemaEnumeration() == null) {
      //TODO PN need a better way of determining mapset vs schema enum (for multi select)
      //Legacy behaviour: mapset could be defined on child node
      NodeInfo lSelectorNodeInfo = getSelectorNodeInfo();
      lMapSetName = lSelectorNodeInfo.getAttribute(NodeInfo.FOX_NAMESPACE, NodeAttribute.MAPSET.getExternalString());
      // The child node isn't evaluated, so attributes aren't pre-cached. Potentially leading to issues if the mapset-attach here uses a relative context
      String lMapSetAttach = lSelectorNodeInfo.getAttribute(NodeInfo.FOX_NAMESPACE, NodeAttribute.MAPSET_ATTACH.getExternalString());

      if(!XFUtil.isNull(lMapSetName)) {
        return super.getEvaluatedParseTree().resolveMapSet(lMapSetName, super.getDataItem(), lMapSetAttach);
      }
      else {
        //Mapset might still not be defined
        return null;
      }
    }
    else {
      return null;
    }
  }

  /**
   * May be null
   *
   * @return
   */
  public MapSet getMapSet() {
    return mMapSet;
  }

  @Override
  protected WidgetType getWidgetType() {
    String lWidgetTypeString = getStringAttribute(NodeAttribute.WIDGET);
    WidgetType lWidgetType;

    // Assign correct widget based on data type info
    if (lWidgetTypeString == null) {
      if (getSelectorMaxCardinality() > 1 || getStringAttribute(NodeAttribute.MAPSET) != null) {
        lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.SELECTOR);
      }
      else if ("xs:date".equals(getNodeInfo().getDataType())) {
        lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.DATE);
      }
      else if ("xs:dateTime".equals(getNodeInfo().getDataType())) {
        lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.DATE_TIME);
      }
      else if ("phantom".equals(getNodeInfo().getDataType()) && !isPhantomDataNode()) {
        //Normal phantoms should be a link because they're probably an action
        //For phantom data XPaths, fall back to the standard default
        lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.LINK);
      }
      else if ("xs:boolean".equals(getNodeInfo().getDataType())) {
        lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.SELECTOR);
      }
      else {
        lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.INPUT);
      }
    }
    else {
      lWidgetType = WidgetType.fromString(lWidgetTypeString, this);

      if (lWidgetType.getBuilderType() == WidgetBuilderType.TICKBOX && !"xs:boolean".equals(getNodeInfo().getDataType()) && getSelectorMaxCardinality() <= 1) {
        // If you don't have multi-select cardinality you can't be a tickbox. You're after a radio widget instead
        lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.RADIO);
        Track.alert("SingleSelectTickbox", "Tickboxes not supported in single select mode, switching to radio for node " + getIdentityInformation(), TrackFlag.BAD_MARKUP);
      }
      else if(lWidgetType.getBuilderType().getFieldSelectConfig() != null && !"xs:boolean".equals(getNodeInfo().getDataType()) &&
        getStringAttribute(NodeAttribute.MAPSET) == null && getSchemaEnumeration() == null) {
        // TODO PN this is a hack; FVM should deal with this more gracefully
        Track.alert("FieldSelectWidgetWithoutFVM", lWidgetType.getBuilderType() + " found without a map-set or schema enumeration on node " + getIdentityInformation(), TrackFlag.BAD_MARKUP);
        // OptionFieldMgr will blow up as no option source can be found
      }
    }
    return lWidgetType;
  }

  @Override
  public FieldMgr getFieldMgr() {
    return mFieldMgr;
  }

  @Override
  public String getExternalFieldName() {
    return getFieldMgr().getExternalFieldName();
  }

  @Override
  public String getExternalFoxId() {
    return mFieldMgr.getExternalFieldName(); //TODO PN this is loosely coupled logic (relying on implicit FieldSet behaviour)
  }

  @Override
  protected String getAutoFieldWidth() {
    if (getWidgetBuilderType().isAction()) {
      // Return prompt width
      return Integer.toString(getPrompt().getString().length());
    }

    return super.getAutoFieldWidth();
  }

  /**
   * Returns true if this ENI represents a phantom-data-xpath definition node, which may need to be treated specially
   * in some cases.
   * @return True if this node is a phantom data node.
   */
  public boolean isPhantomDataNode(){
    return false;
  }
}
