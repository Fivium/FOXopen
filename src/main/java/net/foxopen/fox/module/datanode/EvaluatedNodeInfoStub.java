package net.foxopen.fox.module.datanode;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetType;

/**
 * Stub EvaluatedNodeInfo class to be used when finding possible columns for list set outs
 */
public class EvaluatedNodeInfoStub extends EvaluatedNodeInfo {

  protected EvaluatedNodeInfoStub(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    // If the widget is for an action but it's not runnable while not being a plus widget, knock it to denied visibility
    if (getWidgetBuilderType().isAction() && !XFUtil.isNull(getActionName()) && !isRunnable() && !isPlusWidget()) {
      setVisibility(NodeVisibility.DENIED);
    }

    super.getEvaluatedParseTree().addImplicatedWidget(getWidgetBuilderType(), this);

    // If this item is in a list, check it for content and mark the column up as containing content on the list
    if (getParent() instanceof EvaluatedNodeInfoCollection && getParent().getParent() instanceof EvaluatedNodeInfoList) {
      EvaluatedNodeInfoList lListContainer = (EvaluatedNodeInfoList)getParent().getParent();
      if ((isAttributeDefined(NodeAttribute.HAS_CONTENT) && getBooleanAttribute(NodeAttribute.HAS_CONTENT, false))
        || (!isAttributeDefined(NodeAttribute.HAS_CONTENT) && XFUtil.exists(pNodeEvaluationContext.getDataItem().value(true)))) {
        lListContainer.registerNonCollapsibleColumn(pNodeInfo);
      }
    }
  }

  @Override
  protected WidgetType getWidgetType() {
    String lWidgetTypeString = getStringAttribute(NodeAttribute.WIDGET);
    WidgetType lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.INPUT);

    if (lWidgetTypeString != null) {
      lWidgetType = WidgetType.fromString(lWidgetTypeString, this);
    }
    return lWidgetType;
  }

  @Override
  public FieldMgr getFieldMgr() {
    throw new ExInternal(getClass().getName() + " cannot provide a FieldMgr - only applicable to items, actions and cellmates");
  }

  @Override
  public String getExternalFieldName() {
    throw new ExInternal(getClass().getName() + " cannot provide a FieldMgr - only applicable to items, actions and cellmates");
  }

  @Override
  public String getExternalFoxId() {
    throw new ExInternal(getClass().getName() + " cannot provide a FieldMgr - only applicable to items, actions and cellmates");
  }
}
