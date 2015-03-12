package net.foxopen.fox.module.serialiser.layout;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


public class IndividualCellItem extends CellItem {
  private final EvaluatedNode mCellItem;
  private final CellItem.Dimensions mDimensions = new CellItem.Dimensions();
  private final String mPromptLayout;

  public IndividualCellItem(int pColumnLimit, WidgetBuilder pItemWidgetBuilder, EvaluatedNode pCellItem) {
    super(pCellItem);

    mCellItem = pCellItem;

    int lColChars = Integer.parseInt(mCellItem.getStringAttribute(NodeAttribute.FORM_COL_CHARS, "20"));

    // Calculate prompt column span
    if (pItemWidgetBuilder.hasPrompt(mCellItem)) {
      mPromptLayout = mCellItem.getStringAttribute(NodeAttribute.PROMPT_LAYOUT, "west");

      String lPromptSpan = mCellItem.getStringAttribute(NodeAttribute.PROMPT_SPAN);

      if (lPromptSpan == null) {
        // Deal with legacy attributes when new promptSpan is not specified
        String lPromptWidth = mCellItem.getStringAttribute(NodeAttribute.PROMPT_WIDTH, "auto");
        if ("auto".equals(lPromptWidth)) {
          String lPromptText = mCellItem.getPrompt().getString();
          if (lPromptText == null) {
            lPromptWidth = "1";
          }
          else {
            lPromptWidth = Integer.toString(Math.max(1, lPromptText.length()));
          }
        }

        lPromptSpan = Double.toString(Math.max(1, Math.ceil(new Double(lPromptWidth) / (double)lColChars)));
      }
      else {
        // If we have a new span attr, use it
        lPromptSpan = Double.toString(Math.min(pColumnLimit, new Double(lPromptSpan)));
      }

      mDimensions.promptSpan = new Double(lPromptSpan).intValue();
    }
    else {
      mPromptLayout = "west";
    }

    // Calculate field column span
    String lFieldSpan = mCellItem.getStringAttribute(NodeAttribute.FIELD_SPAN);
    if (lFieldSpan == null) {
      // Deal with legacy attributes when new fieldSpan is not specified
      String lWidgetWidth = mCellItem.getFieldWidth();

      lFieldSpan = Double.toString(Math.min(pColumnLimit, Math.max(1, Math.ceil(new Double(lWidgetWidth) / (double)lColChars))));
    }
    else {
      // If we have a new span attr use it
      lFieldSpan = Double.toString(Math.min(pColumnLimit, new Double(lFieldSpan)));
    }

    mDimensions.fieldSpan = new Double(lFieldSpan).intValue();

    // Prompt norths are defaulted to the width of the widget they will sit above
    if ("north".equals(mPromptLayout)) {
      mDimensions.promptSpan = mDimensions.fieldSpan;
    }

    // Calculate offset span
    String lOffsetSpan = pCellItem.getStringAttribute(NodeAttribute.OFFSET_SPAN);
    if (!XFUtil.isNull(lOffsetSpan)) {
      mDimensions.offsetSpan = Integer.parseInt(lOffsetSpan);
    }
  }

  public EvaluatedNode getCellItem() {
    return mCellItem;
  }

  @Override
  public boolean isIndividualCell() {
    return true;
  }

  @Override
  public CellItem.Dimensions getDimensions() {
    return mDimensions;
  }

  @Override
  public String getPromptLayout() {
    return mPromptLayout;
  }

  @Override
  public String toString() {
    return mCellItem.toString();
  }

  @Override
  public String getDisplayBeforeAttribute() {
    return mCellItem.getDisplayBeforeAttribute();
  }

  @Override
  public String getDisplayAfterAttribute() {
    return mCellItem.getDisplayAfterAttribute();
  }

  @Override
  public String getDisplayOrder() {
    return mCellItem.getDisplayOrder();
  }

  @Override
  public String getName() {
    return mCellItem.getName();
  }
}
