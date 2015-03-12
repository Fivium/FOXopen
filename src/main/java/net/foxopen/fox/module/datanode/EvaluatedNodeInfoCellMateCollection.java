package net.foxopen.fox.module.datanode;


import net.foxopen.fox.module.MandatoryDisplayOption;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.serialiser.layout.CellMates;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetType;

import java.util.LinkedList;
import java.util.List;


public class EvaluatedNodeInfoCellMateCollection extends EvaluatedNodeInfoGeneric {
  /**
   * Child list of the cellmates in this group
   */
  private final List<EvaluatedNodeInfo> mChildren = new LinkedList<>();
  private final CellMates mCellMates;
  private final String mExternalFoxId;

  public EvaluatedNodeInfoCellMateCollection(CellMates pCellMates, EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    mExternalFoxId = getEvaluatedParseTree().getFieldSet().getExternalFoxId(pNodeEvaluationContext.getDataItem());

    mCellMates = pCellMates;
  }

  /**
   * Get the prompt from the cellmates container
   *
   * @return
   */
  @Override
  public StringAttributeResult getPrompt() {
    return mCellMates.getJailPrompt();
  }

  /**
   * Cellmate containers don't have the concept of mandatoryness
   *
   * @return false
   */
  @Override
  public boolean isMandatory() {
    return false;
  }

  /**
   * Because we mark the cellmate container as non-mandatory, we don't want it to show up as optional, so turn off
   * the mandatory display too
   *
   * @return NONE
   */
  @Override
  public MandatoryDisplayOption getMandatoryDisplay() {
    return MandatoryDisplayOption.NONE;
  }

  /**
   * Cellmates containers don't have errors, the items inside them may, but the container div shouldn't show this
   *
   * @return false
   */
  @Override
  public boolean hasError() {
    return false;
  }

  /**
   * Cellmate containers don't have hints, the items inside them may, but the container div shouldn't show this
   *
   * @return false
   */
  @Override
  public boolean hasHint() {
    return false;
  }

  /**
   * Cellmate containers don't have descriptions, the items inside them may, but the container div shouldn't show this
   *
   * @return false
   */
  @Override
  public boolean hasDescription() {
    return false;
  }

  /**
   * The FieldMgr should point to the first item in the cellmates group, this way the label prompt for the cellmates
   * will be for the first item, making it nicer accessibility-wise
   *
   * @return FieldMgr for the first cellmate
   */
  @Override
  public FieldMgr getFieldMgr() {
    // TODO - NP - Robustify this
    return getChildren().get(0).getFieldMgr();
  }

  @Override
  public String getExternalFieldName() {
    return getFieldMgr().getExternalFieldName();
  }

  @Override
  public String getExternalFoxId() {
    return mExternalFoxId;
  }

  @Override
  protected WidgetType getWidgetType() {
    return WidgetType.fromBuilderType(WidgetBuilderType.CELLMATES);
  }

  @Override
  public String getIdentityInformation() {
    StringBuilder lReturnValue = new StringBuilder();
    lReturnValue.append(getClass().getSimpleName());
    lReturnValue.append("[");

    lReturnValue.append("Cellmate Key: '");
    lReturnValue.append(mCellMates.getCellmateKey());
    lReturnValue.append("'");

    String lPrompt = getPrompt().getString();
    if (lPrompt != null) {
      lReturnValue.append(", ");
      lReturnValue.append("Prompt: '");
      lReturnValue.append(lPrompt);
      lReturnValue.append("'");
    }

    if (getEvaluatedPresentationNode() != null) {
      lReturnValue.append(", ");
      lReturnValue.append("PresentationNode: '");
      lReturnValue.append(getEvaluatedPresentationNode().toString());
      lReturnValue.append("'");
    }

    lReturnValue.append("]");
    return lReturnValue.toString();
  }

  public List<EvaluatedNodeInfo> getChildren() {
    return mChildren;
  }

  public void addChild(EvaluatedNodeInfo pEvaluatedNode) {
    mChildren.add(pEvaluatedNode);
  }
}
