package net.foxopen.fox.module.serialiser.layout;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.DisplayOrderSortable;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;

import java.util.Arrays;

public abstract class CellItem implements DisplayOrderSortable {
  private final static String ROW_BREAK_BEFORE_VALUE = "before";
  private final static String ROW_BREAK_AFTER_VALUE = "after";
  private final static String ROW_BREAK_BOTH_VALUE = "both";
  private final static String ROW_BREAK_NONE_VALUE = "none";

  private final boolean mIsRowBreakBefore;
  private final boolean mIsRowBreakAfter;

  /**
   * Construct a cell with the row break before/after calculated from the rowBreak attribute (enumeration of 'before',
   * 'after', 'both', 'none') or the individual rowBreakBefore/rowBreakAfter attributes when rowBreak is not set.
   *
   * @param pCellItemDataNode The evaluated node to get the cell attributes from
   */
  protected CellItem (EvaluatedNode pCellItemDataNode) {
    String lRowBreakEnum = pCellItemDataNode.getStringAttribute(NodeAttribute.ROW_BREAK);
    if (lRowBreakEnum != null) {
      // Row break before and after are set based on the enum value
      String lRowBreakEnumLowerCase = lRowBreakEnum.toLowerCase();

      if (!Arrays.asList(ROW_BREAK_BEFORE_VALUE, ROW_BREAK_AFTER_VALUE, ROW_BREAK_BOTH_VALUE, ROW_BREAK_NONE_VALUE).contains(lRowBreakEnumLowerCase)) {
        throw new ExInternal("Unknown rowBreak value: '" + lRowBreakEnum + "' on " + pCellItemDataNode.getIdentityInformation());
      }

      mIsRowBreakBefore = ROW_BREAK_BEFORE_VALUE.equals(lRowBreakEnumLowerCase) || ROW_BREAK_BOTH_VALUE.equals(lRowBreakEnumLowerCase);
      mIsRowBreakAfter = ROW_BREAK_AFTER_VALUE.equals(lRowBreakEnumLowerCase) || ROW_BREAK_BOTH_VALUE.equals(lRowBreakEnumLowerCase);
      // Note the tests above set row break before and after to false when the value is 'none'
    }
    else {
      // No enum value is set, determine using the individual row break before and after attributes
      mIsRowBreakBefore = pCellItemDataNode.getBooleanAttribute(NodeAttribute.ROW_BREAK_BEFORE, false);
      mIsRowBreakAfter = pCellItemDataNode.getBooleanAttribute(NodeAttribute.ROW_BREAK_AFTER, false);
    }
  }

  public abstract boolean isIndividualCell();
  public abstract Dimensions getDimensions();
  public abstract String getPromptLayout();
  public abstract EvaluatedNode getCellItem();

  /**
   * @return true if the cell has rowBreak 'before'/'both' or when rowBreak is null, if rowBreakBefore evaluates to true
   */
  public boolean isRowBreakBefore() {
    return mIsRowBreakBefore;
  }

  /**
   * @return true if the cell has rowBreak 'after'/'both' or when rowBreak is null, if rowBreakAfter evaluates to true
   */
  public boolean isRowBreakAfter() {
    return mIsRowBreakAfter;
  }

  public class Dimensions {
    public int offsetSpan = 0;
    public int promptSpan = 0;
    public int fieldSpan = 0;

    public String getDimensionInformation() {
      StringBuilder lDimensionInfo = new StringBuilder(45);
      lDimensionInfo.append("[");
      lDimensionInfo.append(NodeAttribute.OFFSET_SPAN.getExternalString());
      lDimensionInfo.append(": ");
      lDimensionInfo.append(offsetSpan);
      lDimensionInfo.append(", ");
      lDimensionInfo.append(NodeAttribute.PROMPT_SPAN.getExternalString());
      lDimensionInfo.append(": ");
      lDimensionInfo.append(promptSpan);
      lDimensionInfo.append(", ");
      lDimensionInfo.append(NodeAttribute.FIELD_SPAN.getExternalString());
      lDimensionInfo.append(": ");
      lDimensionInfo.append(fieldSpan);
      lDimensionInfo.append("]");
      return lDimensionInfo.toString();
    }
  }
}
