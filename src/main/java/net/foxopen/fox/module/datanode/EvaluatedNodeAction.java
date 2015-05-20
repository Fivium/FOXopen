package net.foxopen.fox.module.datanode;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.ActionDefinition;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.evaluatedattributeresult.FixedStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetType;


public class EvaluatedNodeAction extends EvaluatedNode {
  private final String mActionName;
  private final FieldMgr mFieldMgr;

  public EvaluatedNodeAction(GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode,
                             NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, ActionDefinition pActionDefinition) {
    super(null, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility);
    mActionName = pActionDefinition.getActionName();
    mFieldMgr = pNodeEvaluationContext.getEvaluatedParseTree().getFieldSet().createFieldMgr(this);

    //ENAs are always visible, so widget must be implicated
    super.getEvaluatedParseTree().addImplicatedWidget(getWidgetBuilderType(), this);
  }

  /**
   * Set the widget based on what they specify or default to link. If it has defaultAction specified as true then make it a submit widget
   */
  @Override
  protected WidgetType getWidgetType() {
    String lWidgetTypeString = getStringAttribute(NodeAttribute.WIDGET);
    WidgetType lWidgetType;

    if (lWidgetTypeString == null) {
      // If no widget defined, default to link
      lWidgetType = WidgetType.fromBuilderType(WidgetBuilderType.LINK);
    }
    else {
      lWidgetType = WidgetType.fromString(lWidgetTypeString, this);
    }

    return lWidgetType;
  }
  @Override
  public StringAttributeResult getSummaryPrompt() throws ExInternal {
    StringAttributeResult lSummaryPromptValue = super.getSummaryPromptInternal();
    if (lSummaryPromptValue != null) {
      if ("".equals(lSummaryPromptValue.getString())) {
        return null;
      }
      return lSummaryPromptValue;
    }

    // Action name
    String lActionName = getActionName();
    if (XFUtil.exists(lActionName)) {
      int pos = lActionName.indexOf("/");
      if (pos != -1) {
        return new FixedStringAttributeResult(XFUtil.initCap(lActionName.substring(pos + 1)));
      }
      return new FixedStringAttributeResult(XFUtil.initCap(lActionName));
    }
    return null;
  }

  @Override
  public String getPromptInternal() {
    if (XFUtil.exists(mActionName)) {
      int pos = mActionName.indexOf("/");
      if (pos != -1) {
        return XFUtil.initCap(mActionName.substring(pos + 1));
      }
      return XFUtil.initCap(mActionName);
    }
    return null;
  }

  @Override
  public boolean isPhantom() {
    return false;
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
  public String getName() {
    return getActionName();
  }

  @Override
  public String getActionName() {
    return mActionName;
  }

  @Override
  public int getSelectorMaxCardinality() {
    throw new ExInternal(getClass().getName() + " cannot provide a selector cardinality - only applicable to items");
  }
}
