package net.foxopen.fox.module.serialiser.layout;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.LayoutDirection;
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
  private final LayoutDirection mJailPromptLayout;

  private final CellItem.Dimensions mDimensions;

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

    mJailPromptLayout = LayoutDirection.valueOf(mCellMatesContainer.getStringAttribute(NodeAttribute.PROMPT_LAYOUT, "west").toUpperCase());

    int lOffsetSpan, lPromptSpan, lFieldSpan;
    // Get field span
    String lJailFieldSpanAttributeOrNull = mCellMatesContainer.getStringAttribute(NodeAttribute.FIELD_SPAN);
    if (!XFUtil.isNull(lJailFieldSpanAttributeOrNull)) {
      lFieldSpan = Integer.valueOf(lJailFieldSpanAttributeOrNull);
    }
    else {
      throw new ExInternal("CellMate sequences require a fieldSpan to define how many columns the CellMate sub-grid container spans");
    }

    // Get prompt span
    if (LayoutDirection.NORTH == mJailPromptLayout) {
      // Prompt norths are defaulted to the width of the widget they will sit above
      lPromptSpan = lFieldSpan;
    }
    else {
      String lJailPromptSpanAttributeOrNull = mCellMatesContainer.getStringAttribute(NodeAttribute.PROMPT_SPAN);
      if (!XFUtil.isNull(lJailPromptSpanAttributeOrNull)) {
        lPromptSpan = Integer.valueOf(lJailPromptSpanAttributeOrNull);
      }
      else {
        throw new ExInternal("CellMate sequences require a promptSpan");
      }
    }

    // Calculate offset span
    String lOffsetSpanAttr = mCellMatesContainer.getStringAttribute(NodeAttribute.OFFSET_SPAN);
    if (!XFUtil.isNull(lOffsetSpanAttr)) {
      lOffsetSpan = Integer.parseInt(lOffsetSpanAttr);
    }
    else {
      lOffsetSpan = 0;
    }

    mDimensions = new CellItem.Dimensions(mCellMatesContainer, lOffsetSpan, lPromptSpan, lFieldSpan);
  }

  public String getCellmateKey() {
    return mCellmateKey;
  }

  @Override
  public CellItem.Dimensions getDimensions() {
    return mDimensions;
  }

  @Override
  public LayoutDirection getPromptLayout() {
    return mJailPromptLayout;
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
