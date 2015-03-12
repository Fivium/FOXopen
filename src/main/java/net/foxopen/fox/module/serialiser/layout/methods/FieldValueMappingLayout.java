package net.foxopen.fox.module.serialiser.layout.methods;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.layout.LayoutResult;
import net.foxopen.fox.module.serialiser.layout.items.LayoutFieldValueMappingItemColumn;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItem;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemEnum;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemRowEnd;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemRowStart;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout manager for FieldValueMappings, e.g. Tickbox/Radio button grids.
 */
public class FieldValueMappingLayout implements LayoutMethod {
  private static final LayoutMethod INSTANCE = new FieldValueMappingLayout();

  public static final LayoutMethod getInstance() {
    return INSTANCE;
  }

  private FieldValueMappingLayout() {
  }

  private static final String FLOW_ACROSS = "across";
  private static final String FLOW_DOWN = "down";

  public LayoutResult doLayout(int pColumnLimit, OutputSerialiser pSerialiser, EvaluatedNodeInfo pEvalNodeInfo) {
    final List<LayoutItem> lItems = new ArrayList<>();

    int lRowCount = 0;
    int lFilledColumnCount = 0;
    int lWidgetColumnCount = 0;
    int lFillerColumnCount = 0;

    // Keep a reference to the current row to find out when a new row is needed
    LayoutItemRowStart lCurrentRow;

    String lFlowDirection = pEvalNodeInfo.getStringAttribute(NodeAttribute.FLOW, "across");

    lCurrentRow = new LayoutItemRowStart();
    lRowCount++;
    lItems.add(lCurrentRow);

    FieldMgr lFieldMgr = pEvalNodeInfo.getFieldMgr();

    List<FieldSelectOption> lOriginalSelectOptions = lFieldMgr.getSelectOptions();
    List<FieldSelectOption> lFilteredSelectOptions = new ArrayList<>(lOriginalSelectOptions.size());

    //Filter historical options out of the list
    for(FieldSelectOption lSelectOption : lOriginalSelectOptions) {
      if(!lSelectOption.isHistorical() || lSelectOption.isSelected()) {
        lFilteredSelectOptions.add(lSelectOption);
      }
    }

    if (FLOW_ACROSS.equals(lFlowDirection)) {
      for (FieldSelectOption lItem : lFilteredSelectOptions){
        LayoutFieldValueMappingItemColumn lWidgetCol = new LayoutFieldValueMappingItemColumn(1, lItem);
        lItems.add(lWidgetCol);
        lWidgetColumnCount++;
        lCurrentRow.addColumn(lWidgetCol);
        lFilledColumnCount++;

        if (lCurrentRow.getColumnsFilled() >= pColumnLimit) {
          lItems.add(new LayoutItemRowEnd());
          lCurrentRow = new LayoutItemRowStart();
          lRowCount++;
          lItems.add(lCurrentRow);
        }
      }
    }
    else if (FLOW_DOWN.equals(lFlowDirection)) {
      int lItemsPerColumn = (int)Math.ceil(new Double(lFilteredSelectOptions.size()) / new Double(pColumnLimit));

      for (int lItemOffset = 0; lItemOffset < lItemsPerColumn; lItemOffset++){
        for (int lItemPointer = 0; lItemPointer < pColumnLimit; lItemPointer++){
          int lItemIndex = lItemOffset + (lItemPointer * lItemsPerColumn);
          if (lItemIndex >= lFilteredSelectOptions.size()) {
            LayoutFieldValueMappingItemColumn lFillerCol = new LayoutFieldValueMappingItemColumn(pColumnLimit - lCurrentRow.getColumnsFilled());
            lItems.add(lFillerCol);
            lFillerColumnCount++;
            lCurrentRow.addColumn(lFillerCol);
          }
          else {
            FieldSelectOption lItem = lFilteredSelectOptions.get(lItemIndex);

            LayoutFieldValueMappingItemColumn lWidgetCol = new LayoutFieldValueMappingItemColumn(1, lItem);
            lItems.add(lWidgetCol);
            lWidgetColumnCount++;
            lCurrentRow.addColumn(lWidgetCol);
            lFilledColumnCount++;
          }

          if (lCurrentRow.getColumnsFilled() >= pColumnLimit) {
            lItems.add(new LayoutItemRowEnd());
            lCurrentRow = new LayoutItemRowStart();
            lRowCount++;
            lItems.add(lCurrentRow);
          }
        }
      }
    }
    else {
      throw new ExInternal("Unknown flow direction given for FieldValueMapping layout: " + lFlowDirection);
    }

    if (lItems.get(lItems.size() - 1).getItemType() != LayoutItemEnum.ROW_END) {
      if (lCurrentRow.getColumnsFilled() == 0) {
        lItems.remove(lItems.size()-1);
      }
      else {
        LayoutFieldValueMappingItemColumn lFillerCol = new LayoutFieldValueMappingItemColumn(pColumnLimit - lCurrentRow.getColumnsFilled());
        lItems.add(lFillerCol);
        lFillerColumnCount++;
        lItems.add(new LayoutItemRowEnd());
      }
    }

    final int lFinalRowCount = lRowCount;
    final int lFinalFiledColumnCount = lFilledColumnCount;
    final int lFinalPromptColumnCount = 0; // Prompts don't have their own columns with this LayoutMethod
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
}
