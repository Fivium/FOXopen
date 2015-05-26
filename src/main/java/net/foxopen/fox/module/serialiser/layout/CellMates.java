package net.foxopen.fox.module.serialiser.layout;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoCellMateCollection;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.evaluatedattributeresult.FixedStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;

/**
 * The CellMates class is a type of CellItem which is a container for regular CellItems. Cellmates are defined
 * as an xs:sequence in the module and at module parse time their attributes are tied to a Cellmate Key as a map on the
 * parent element of the xs:sequence.
 *
 * This item is also referred to as the "Jail" as it is the container of the CellMates and goes along with the analogy.
 */
public class CellMates extends CellItem {
  private final String mCellmateKey;
  private final EvaluatedNodeInfoCellMateCollection mCellMatesContainer;

  private final StringAttributeResult mJailPrompt;

  private final CellItem.Dimensions mDimensions = new CellItem.Dimensions();

  public CellMates(EvaluatedNodeInfoCellMateCollection pEvaluatedNodeInfoCellMateCollection) {
    super(pEvaluatedNodeInfoCellMateCollection);

    mCellMatesContainer = pEvaluatedNodeInfoCellMateCollection;

    // Set jail members from root
    mCellmateKey = mCellMatesContainer.getStringAttribute(NodeAttribute.CELLMATE_KEY);

    // Get the prompt for the Jail
    StringAttributeResult lCellmatePrompt = mCellMatesContainer.getStringAttributeResultOrNull(NodeAttribute.PROMPT);
    if (lCellmatePrompt != null) {
      mJailPrompt = lCellmatePrompt;
    }
    else {
      mJailPrompt = new FixedStringAttributeResult("");
    }

    // Get prompt span
    String lJailPromptSpanAttributeOrNull = mCellMatesContainer.getStringAttribute(NodeAttribute.PROMPT_SPAN);
    if (!XFUtil.isNull(lJailPromptSpanAttributeOrNull)) {
      mDimensions.promptSpan = Integer.valueOf(lJailPromptSpanAttributeOrNull);
    }
    else {
      throw new ExInternal("Cellmates require a promptSpan for the prompt of the jail");
    }

    // Get field span
    String lJailFieldSpanAttributeOrNull = mCellMatesContainer.getStringAttribute(NodeAttribute.FIELD_SPAN);
    if (!XFUtil.isNull(lJailFieldSpanAttributeOrNull)) {
      mDimensions.fieldSpan = Integer.valueOf(lJailFieldSpanAttributeOrNull);
    }
    else {
      throw new ExInternal("Cellmates require a fieldSpan for the prompt of the jail");
    }

    // Calculate offset span
    String lOffsetSpan = mCellMatesContainer.getStringAttribute(NodeAttribute.OFFSET_SPAN);
    if (!XFUtil.isNull(lOffsetSpan)) {
      mDimensions.offsetSpan = Integer.parseInt(lOffsetSpan);
    }
  }

  public String getCellmateKey() {
    return mCellmateKey;
  }

  @Override
  public boolean isIndividualCell() {
    return false;
  }

  @Override
  public CellItem.Dimensions getDimensions() {
    return mDimensions;
  }

  @Override
  public String getPromptLayout() {
    return "west";
  }

  @Override
  public String toString() {
    return mCellmateKey + "(" + mCellMatesContainer.getChildren().size() + ")";
  }

  @Override
  public EvaluatedNodeInfoCellMateCollection getCellItem() {
    return mCellMatesContainer;
  }

  public final StringAttributeResult getJailPrompt() {
    return mJailPrompt;
  }

  @Override
  public String getDisplayBeforeAttribute() {
    return mCellMatesContainer.getDisplayBeforeAttribute();
  }

  @Override
  public String getDisplayAfterAttribute() {
    return mCellMatesContainer.getDisplayAfterAttribute();
  }

  @Override
  public String getDisplayOrder() {
    return mCellMatesContainer.getDisplayOrder();
  }

  @Override
  public String getName() {
    return mCellMatesContainer.getName();
  }
}
