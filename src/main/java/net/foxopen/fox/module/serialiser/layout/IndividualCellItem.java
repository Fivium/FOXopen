package net.foxopen.fox.module.serialiser.layout;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.LayoutDirection;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


public class IndividualCellItem extends CellItem {
  private final EvaluatedNode mCellItem;
  private final CellItem.Dimensions mDimensions;
  private final LayoutDirection mPromptLayout;

  public IndividualCellItem(int pColumnLimit, WidgetBuilder pItemWidgetBuilder, EvaluatedNode pCellItem) {
    super(pCellItem);

    mCellItem = pCellItem;

    int lColChars = Integer.parseInt(mCellItem.getStringAttribute(NodeAttribute.FORM_COL_CHARS, "20"));

    int lOffsetSpan, lPromptSpan, lFieldSpan;
    // Calculate prompt column span
    if (pItemWidgetBuilder.hasPrompt(mCellItem)) {
      mPromptLayout = LayoutDirection.valueOf(mCellItem.getStringAttribute(NodeAttribute.PROMPT_LAYOUT, "west").toUpperCase());
    }
    else {
      mPromptLayout = LayoutDirection.WEST;
    }

    // Calculate field column span
    String lFieldSpanAttr = mCellItem.getStringAttribute(NodeAttribute.FIELD_SPAN);
    if (lFieldSpanAttr == null) {
      // Deal with legacy attributes when new fieldSpan is not specified
      String lWidgetWidth = mCellItem.getFieldWidth();

      lFieldSpan =  Math.min(pColumnLimit, Math.max(1, (int)Math.ceil(Integer.valueOf(lWidgetWidth) / lColChars)));
    }
    else {
      // If we have a new span attr use it
      lFieldSpan = Math.min(pColumnLimit, Integer.valueOf(lFieldSpanAttr));
    }

    // Prompt norths are defaulted to the width of the widget they will sit above
    if (LayoutDirection.NORTH == mPromptLayout) {
      lPromptSpan = lFieldSpan;
    }
    else {
      String lPromptSpanAttr = mCellItem.getStringAttribute(NodeAttribute.PROMPT_SPAN);

      if (lPromptSpanAttr == null) {
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

        lPromptSpan = Math.max(1, (int)Math.ceil(Integer.valueOf(lPromptWidth) / lColChars));
      }
      else {
        // If we have a new span attr, use it
        lPromptSpan = Math.min(pColumnLimit, Integer.valueOf(lPromptSpanAttr));
      }
    }

    // Calculate offset span
    String lOffsetSpanAttr = pCellItem.getStringAttribute(NodeAttribute.OFFSET_SPAN);
    if (!XFUtil.isNull(lOffsetSpanAttr)) {
      lOffsetSpan = Integer.parseInt(lOffsetSpanAttr);
    }
    else {
      lOffsetSpan = 0;
    }

    mDimensions = new CellItem.Dimensions(mCellItem, lOffsetSpan, lPromptSpan, lFieldSpan);
  }

  public EvaluatedNode getCellItem() {
    return mCellItem;
  }

  @Override
  public CellItem.Dimensions getDimensions() {
    return mDimensions;
  }

  @Override
  public LayoutDirection getPromptLayout() {
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
