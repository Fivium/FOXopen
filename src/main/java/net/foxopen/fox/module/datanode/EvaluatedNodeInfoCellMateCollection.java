package net.foxopen.fox.module.datanode;


import net.foxopen.fox.dom.NamespaceAttributeTable;
import net.foxopen.fox.module.MandatoryDisplayOption;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.layout.CellItem;
import net.foxopen.fox.module.serialiser.layout.CellMates;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetType;

import java.util.LinkedList;
import java.util.List;


public class EvaluatedNodeInfoCellMateCollection extends EvaluatedNodeInfoGeneric {
  /**
   * Child list of the CellMates in this group
   */
  private final List<EvaluatedNodeInfo> mChildren = new LinkedList<>();
  private final EvaluatedNodeInfo mRootNode;
  private final CellMates mCellMates;
  private final String mExternalFoxId;

  public static EvaluatedNodeInfoCellMateCollection createEvaluatedNodeInfoCellMateCollection(EvaluatedNodeInfo pRootCellMate) {
    NamespaceAttributeTable lCellmatesAttributes = ((EvaluatedNodeInfo)pRootCellMate.getParent()).getNodeInfo().getCellmateAttributes(pRootCellMate.getStringAttribute(NodeAttribute.CELLMATE_KEY));

    // Create a enw NodeEvaluationContext from the root cell mate's but with the cellmate attributes passed in
    NodeEvaluationContext lStubNEC = pRootCellMate.getNodeEvaluationContext();
    NodeEvaluationContext lCellMatesNodeEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(lStubNEC.getEvaluatedParseTree(), pRootCellMate.getEvaluatedPresentationNode(),
      lStubNEC.getDataItem(), lStubNEC.getEvaluateContextRuleItem(),
      null, lCellmatesAttributes, null, lStubNEC);

    return new EvaluatedNodeInfoCellMateCollection(pRootCellMate, lCellMatesNodeEvaluationContext);
  }

  public EvaluatedNodeInfoCellMateCollection(EvaluatedNodeInfo pRootCellMate, NodeEvaluationContext pNodeEvaluationContext) {
    super(pRootCellMate.getParent(), pRootCellMate.getEvaluatedPresentationNode(), pNodeEvaluationContext, pRootCellMate.getVisibility(), pRootCellMate.getNodeInfo());

    mExternalFoxId = getEvaluatedParseTree().getFieldSet().getExternalFoxId(pNodeEvaluationContext.getDataItem());

    mCellMates = new CellMates(this);

    mRootNode = pRootCellMate;
  }

  /**
   * Get the prompt from the CellMates container
   *
   * @return The prompt for the CellMates object, AKA the Jail Prompt
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
    return mRootNode.getFieldMgr();
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

    //mCellMates might be null during super constructor call
    if(mCellMates != null) {
      lReturnValue.append("Cellmate Key: '");
      lReturnValue.append(mCellMates.getCellmateKey());
      lReturnValue.append("'");

      StringAttributeResult lPrompt = getPrompt();
      if (lPrompt != null) {
        lReturnValue.append(", ");
        lReturnValue.append("Prompt: '");
        lReturnValue.append(lPrompt.getString());
        lReturnValue.append("'");
      }
    }
    else {
      lReturnValue.append("Cellmate Key: '");
      lReturnValue.append(getStringAttribute(NodeAttribute.CELLMATE_KEY));
      lReturnValue.append("' (not constructed yet, best guess)");
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

  /**
   * Generate an CellMates Cell Item that can represent this EvaluatedNodeInfo in a layout
   *
   * @param pColumnLimit Max amount of columns possible for this item
   * @param pSerialiser Serialiser to use when generating this item
   * @return CellMates CellItem for this item
   */
  @Override
  public CellItem getCellItem(int pColumnLimit, OutputSerialiser pSerialiser) {
    return mCellMates;
  }
}
