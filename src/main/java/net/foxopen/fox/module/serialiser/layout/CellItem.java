package net.foxopen.fox.module.serialiser.layout;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.DisplayOrderSortable;
import net.foxopen.fox.module.LayoutDirection;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

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

  public abstract Dimensions getDimensions();
  public abstract LayoutDirection getPromptLayout();
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
    private final int mOffsetSpan;
    private final int mPromptSpan;
    private final int mFieldSpan;

    public Dimensions(EvaluatedNode pEvaluatedNode, int pOffsetSpan, int pPromptSpan, int pFieldSpan) {
      if (pOffsetSpan < 0) {
        Track.alert("FieldDimensionIssue", "Offset Span (" + pOffsetSpan + ") must be >=0: " + pEvaluatedNode.getIdentityInformation(), TrackFlag.BAD_MARKUP);
      }
      if (pPromptSpan < 0) {
        Track.alert("FieldDimensionIssue", "Prompt Span (" + pPromptSpan + ") must be >=0: " + pEvaluatedNode.getIdentityInformation(), TrackFlag.BAD_MARKUP);
      }
      if (pFieldSpan < 0) {
        Track.alert("FieldDimensionIssue", "Field Span (" + pFieldSpan + ") must be >=0: " + pEvaluatedNode.getIdentityInformation(), TrackFlag.BAD_MARKUP);
      }

      mOffsetSpan = pOffsetSpan;
      mPromptSpan = pPromptSpan;
      mFieldSpan = pFieldSpan;
    }

    public int getOffsetSpan() {
      return mOffsetSpan;
    }

    public int getPromptSpan() {
      return mPromptSpan;
    }

    public int getFieldSpan() {
      return mFieldSpan;
    }

    public String getDimensionInformation() {
      return "[" +
        NodeAttribute.OFFSET_SPAN.getExternalString() + ": " + mOffsetSpan + ", " +
        NodeAttribute.PROMPT_SPAN.getExternalString() + ": " + mPromptSpan + ", " +
        NodeAttribute.FIELD_SPAN.getExternalString() + ": " + mFieldSpan +
        "]";
    }
  }
}
