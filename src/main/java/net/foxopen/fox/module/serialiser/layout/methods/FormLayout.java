package net.foxopen.fox.module.serialiser.layout.methods;

import net.foxopen.fox.module.DisplayOrderComparator;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoCellMateCollection;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.layout.CellItem;
import net.foxopen.fox.module.serialiser.layout.CellMates;
import net.foxopen.fox.module.serialiser.layout.IndividualCellItem;
import net.foxopen.fox.module.serialiser.layout.LayoutResult;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItem;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemColumn;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemEnum;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemRowEnd;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemRowStart;
import net.foxopen.fox.module.serialiser.layout.items.LayoutWidgetItemColumn;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FormLayout implements LayoutMethod {
  private static final LayoutMethod INSTANCE = new FormLayout();

  public static final LayoutMethod getInstance() {
    return INSTANCE;
  }

  private FormLayout() {
  }

  public LayoutResult doLayout(int pColumnLimit, OutputSerialiser pSerialiser, EvaluatedNodeInfo pEvalNodeInfo) {
    final List<LayoutItem> lItems = new ArrayList<>();

    int lRowCount = 0;
    int lFilledColumnCount = 0;
    int lPromptColumnCount = 0;
    int lWidgetColumnCount = 0;
    int lFillerColumnCount = 0;

    LayoutItemRowStart lCurrentRow = null;
    LayoutItemRowStart lCurrentPromptNorthRow = null;

    LayoutWidgetItemColumn lFillerCol;

    // TODO - NP - Perhaps add in Grid Start/End layout items to help with potential future sub-grid layouts
    lCurrentRow = new LayoutItemRowStart();
    lItems.add(lCurrentRow);
    lRowCount++;

    // Construct list of items to process, and group up cellmates
    List<CellItem> lCellItems = new ArrayList<>();
    Map<String, CellMates> lCellMateMap = new HashMap<>();
    for (EvaluatedNodeInfo lItemNodeInfo : pEvalNodeInfo.getChildren()){
      if (lItemNodeInfo.getVisibility() == NodeVisibility.DENIED) {
        continue;
      }

      WidgetBuilder lItemWidgetBuilder = pSerialiser.getWidgetBuilder(lItemNodeInfo.getWidgetBuilderType());

      String lCellmateKey = lItemNodeInfo.getStringAttribute(NodeAttribute.CELLMATE_KEY);
      if (lCellmateKey != null && !(pEvalNodeInfo instanceof EvaluatedNodeInfoCellMateCollection)) {
        CellMates lCellMates = lCellMateMap.get(lCellmateKey);
        if (lCellMates == null) {
          lCellMates = new CellMates(pColumnLimit, lItemWidgetBuilder, lCellmateKey, lItemNodeInfo);
          lCellMateMap.put(lCellmateKey, lCellMates);
          lCellItems.add(lCellMates);
        }

        lCellMates.addCellMate(lItemNodeInfo);
      }
      else {
        lCellItems.add(new IndividualCellItem(pColumnLimit, lItemWidgetBuilder, lItemNodeInfo));
      }
    }

    // Sort form items based on displayOrder
    Collections.sort(lCellItems, DisplayOrderComparator.getInstance());

    // Process the flow layout
    for (int lItemIndex = 0; lItemIndex < lCellItems.size(); lItemIndex++) {
      CellItem lCellItem = lCellItems.get(lItemIndex);

      // Calculate how wide the whole widget will be
      int lWidgetColumnSpan = 0;
      if ("north".equals(lCellItem.getPromptLayout())) {
        lWidgetColumnSpan = lCellItem.getDimensions().fieldSpan;
      }
      else {
        lWidgetColumnSpan = lCellItem.getDimensions().promptSpan + lCellItem.getDimensions().fieldSpan;
      }

      int lOffsetSpan = lCellItem.getDimensions().offsetSpan;

      // If the widget will bust the row or they asked to break the row before even starting to lay out...
      if (lCurrentRow.getColumnsFilled() + lWidgetColumnSpan + lOffsetSpan > pColumnLimit || (lCurrentRow.getColumnsFilled() > 0  && lCellItem.isRowBreakBefore())) {
        // Fill the rest of the current row with a filler column, if needed
        if (lCurrentRow.getColumnsFilled() < pColumnLimit) {
          lFillerCol = new LayoutWidgetItemColumn(pColumnLimit - lCurrentRow.getColumnsFilled());
          lItems.add(lFillerCol);
          lCurrentRow.addColumn(lFillerCol);
          lFillerColumnCount++;
        }

        // Close the current row
        lItems.add(new LayoutItemRowEnd());

        // Create new current row
        lCurrentRow = new LayoutItemRowStart();
        lItems.add(lCurrentRow);
        lRowCount++;

        if (lCurrentPromptNorthRow != null) {
          // Fill the rest of the prompt north row with a filler column, if needed
          if (lCurrentPromptNorthRow.getColumnsFilled() < pColumnLimit) {
            lFillerCol = new LayoutWidgetItemColumn(pColumnLimit - lCurrentRow.getColumnsFilled());
            lItems.add(getPromptNorthInsertIndex(lItems, lCurrentPromptNorthRow), lFillerCol);
            lCurrentPromptNorthRow.addColumn(lFillerCol);
            lFillerColumnCount++;
          }

          // Null out reference to prompt north row
          lCurrentPromptNorthRow = null;
        }
      }

      EvaluatedNode lItemNodeInfo = lCellItem.getCellItem();

      // If the offset and widget span will bust the row...
      if (lOffsetSpan > 0  && lOffsetSpan + lWidgetColumnSpan > pColumnLimit) {
        Track.alert("ColumnBust", "Offset span and widget column span greater than the form column limit: '" + lItemNodeInfo + "' - spans " + (lOffsetSpan + lWidgetColumnSpan) + " out of " + pColumnLimit + " columns " + lCellItem.getDimensions().getDimensionInformation() + ". Removing the offset.");
        lOffsetSpan = 0;
      }

      // If the widget alone will bust the row...
      if (lWidgetColumnSpan > pColumnLimit) {
        //throw new ExInternal("Widget column span greater than the form column limit: " + lCellItem.toString());
        Track.alert("ColumnBust", "Widget column span greater than the form column limit: '" + lItemNodeInfo + "' - spans " + lWidgetColumnSpan + " out of " + pColumnLimit + " columns " + lCellItem.getDimensions().getDimensionInformation() + ". Shrinking it down to fit the remaining space.");

        if ("north".equals(lCellItem.getPromptLayout())) {
          lCellItem.getDimensions().fieldSpan = Math.min(1, pColumnLimit - lCurrentRow.getColumnsFilled());
        }
        else {
          lCellItem.getDimensions().promptSpan = Math.min(1, pColumnLimit - lCurrentRow.getColumnsFilled());
        }
      }

      // Add offset (for the current row and the prompt north row too)
      if (lOffsetSpan > 0) {
        // Add filler column to current row
        lFillerCol = new LayoutWidgetItemColumn(lOffsetSpan);
        lItems.add(lFillerCol);
        lCurrentRow.addColumn(lFillerCol);
        lFillerColumnCount++;
        // Add filler column to prompt north, if applicable
        if (lCurrentPromptNorthRow != null) {
          lFillerCol = new LayoutWidgetItemColumn(lOffsetSpan);
          lItems.add(getPromptNorthInsertIndex(lItems, lCurrentPromptNorthRow), lFillerCol);
          lCurrentPromptNorthRow.addColumn(lFillerCol);
          lFillerColumnCount++;
        }
      }

      WidgetBuilder lItemWidgetBuilder = pSerialiser.getWidgetBuilder(lItemNodeInfo.getWidgetBuilderType());

      // Add prompt
      if (lItemWidgetBuilder.hasPrompt(lItemNodeInfo) && lCellItem.getDimensions().promptSpan > 0) {
        LayoutWidgetItemColumn lPromptColumnItem = new LayoutWidgetItemColumn(lCellItem.getDimensions().promptSpan, lItemNodeInfo, true, lItemWidgetBuilder);

        if ("north".equals(lCellItem.getPromptLayout())) {
          if (lCurrentPromptNorthRow == null) {
            // Create an empty prompt north row if we don't already have one
            lCurrentPromptNorthRow = new LayoutItemRowStart();
            lItems.add(lItems.indexOf(lCurrentRow), lCurrentPromptNorthRow);
            lRowCount++;
            lItems.add(getPromptNorthInsertIndex(lItems, lCurrentPromptNorthRow), new LayoutItemRowEnd());
          }

          // Pad the row so the prompt falls above the widget, if needed
          if (lCurrentRow.getColumnsFilled() > lCurrentPromptNorthRow.getColumnsFilled()) {
            lFillerCol = new LayoutWidgetItemColumn(lCurrentRow.getColumnsFilled() - lCurrentPromptNorthRow.getColumnsFilled());
            lItems.add(getPromptNorthInsertIndex(lItems, lCurrentPromptNorthRow), lFillerCol);
            lCurrentPromptNorthRow.addColumn(lFillerCol);
            lFillerColumnCount++;
          }

          // Add the prompt column
          lItems.add(getPromptNorthInsertIndex(lItems, lCurrentPromptNorthRow), lPromptColumnItem);
          lCurrentPromptNorthRow.addColumn(lPromptColumnItem);
        }
        else {
          lItems.add(lPromptColumnItem);
          lCurrentRow.addColumn(lPromptColumnItem);
        }
        lPromptColumnCount++;
        lFilledColumnCount++;
      }

      // Add field
      LayoutWidgetItemColumn lFieldColumnItem = new LayoutWidgetItemColumn(lCellItem.getDimensions().fieldSpan, lItemNodeInfo, false, lItemWidgetBuilder);
      lItems.add(lFieldColumnItem);
      lCurrentRow.addColumn(lFieldColumnItem);
      lWidgetColumnCount++;
      lFilledColumnCount++;

      // End the row if the cell is row break after and not the last item in this form
      if (lCellItem.isRowBreakAfter() && lItemIndex != (lCellItems.size() - 1)) {
        // Fill the rest of the current row with a filler column, if needed
        if (lCurrentRow.getColumnsFilled() < pColumnLimit) {
          lFillerCol = new LayoutWidgetItemColumn(pColumnLimit - lCurrentRow.getColumnsFilled());
          lItems.add(lFillerCol);
          lCurrentRow.addColumn(lFillerCol);
          lFillerColumnCount++;
        }

        // Close the current row
        lItems.add(new LayoutItemRowEnd());

        // Create new current row
        lCurrentRow = new LayoutItemRowStart();
        lItems.add(lCurrentRow);
        lRowCount++;

        if (lCurrentPromptNorthRow != null) {
          // Fill the rest of the prompt north row with a filler column, if needed
          if (lCurrentPromptNorthRow.getColumnsFilled() < pColumnLimit) {
            lFillerCol = new LayoutWidgetItemColumn(pColumnLimit - lCurrentPromptNorthRow.getColumnsFilled());
            lItems.add(getPromptNorthInsertIndex(lItems, lCurrentPromptNorthRow), lFillerCol);
            lCurrentPromptNorthRow.addColumn(lFillerCol);
            lFillerColumnCount++;
          }

          // Null out reference to prompt north row
          lCurrentPromptNorthRow = null;
        }
      }
    } // END CELL ITEM LOOP

    // Close rows and tidy up if the last item wasn't a row-end
    if (lItems.get(lItems.size() - 1).getItemType() != LayoutItemEnum.ROW_END) {
      // Fill the rest of the current row with a filler column
      if (lCurrentRow.getColumnsFilled() < pColumnLimit) {
        lFillerCol = new LayoutWidgetItemColumn(pColumnLimit - lCurrentRow.getColumnsFilled());
        lItems.add(lFillerCol);
        lCurrentRow.addColumn(lFillerCol);
        lFillerColumnCount++;
      }

      // Close the current row
      lItems.add(new LayoutItemRowEnd());

      if (lCurrentPromptNorthRow != null) {
        // Fill the rest of the prompt north row with a filler column
        if (lCurrentPromptNorthRow.getColumnsFilled() < pColumnLimit) {
          lFillerCol = new LayoutWidgetItemColumn(pColumnLimit - lCurrentPromptNorthRow.getColumnsFilled());
          lItems.add(getPromptNorthInsertIndex(lItems, lCurrentPromptNorthRow), lFillerCol);
          lCurrentPromptNorthRow.addColumn(lFillerCol);
          lFillerColumnCount++;
        }

        // Null out reference to prompt north row
        lCurrentPromptNorthRow = null;
      }
    }


    final int lFinalRowCount = lRowCount;
    final int lFinalFiledColumnCount = lFilledColumnCount;
    final int lFinalPromptColumnCount = lPromptColumnCount;
    final int lFinalWidgetColumnCount = lWidgetColumnCount;
    final int lFinalFillerColumnCount = lFillerColumnCount;

    return new LayoutResult(){
      public List<LayoutItem> getLayoutItems() { return lItems; }
      public int getRowCount() { return lFinalRowCount; }
      public int getFilledColumnCount() { return lFinalFiledColumnCount; }
      public int getPromptColumnCount() { return lFinalPromptColumnCount; }
      public int getWidgetColumnCount() { return lFinalWidgetColumnCount; }
      public int getFillerColumnCount() { return lFinalFillerColumnCount; }
    };
  }

  private int getPromptNorthInsertIndex(List<LayoutItem> pItems, LayoutItemRowStart pCurrentPromptNorthRow) {
    LayoutItemColumn lLastColumn = pCurrentPromptNorthRow.getLastColumn();
    if (lLastColumn == null) {
      return pItems.indexOf(pCurrentPromptNorthRow) + 1;
    }
    else {
      return pItems.indexOf(lLastColumn) + 1;
    }
  }
}
