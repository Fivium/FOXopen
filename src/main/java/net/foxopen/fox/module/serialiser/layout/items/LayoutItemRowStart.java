package net.foxopen.fox.module.serialiser.layout.items;

import java.util.ArrayList;
import java.util.List;

import net.foxopen.fox.ex.ExInternal;


public class LayoutItemRowStart implements LayoutItem {
  private int mColumnsFilled = 0;
  private List<LayoutItemColumn> lColumns = new ArrayList<>();

  public LayoutItemRowStart() {
    super();
  }

  @Override
  public LayoutItemEnum getItemType() {
    return LayoutItemEnum.ROW_START;
  }

  public void addColumn(LayoutItemColumn pColumn) {
    lColumns.add(pColumn);
    mColumnsFilled += pColumn.getColSpan();
  }
  
  public void removeColumn(LayoutItemColumn pColumn) {
    if (lColumns.remove(pColumn)) {
      mColumnsFilled -= pColumn.getColSpan();
    }
    else {
      throw new ExInternal("Cannot remove column from row, it didn't exist in this row?");
    }
  }

  public int getColumnsFilled() {
    return mColumnsFilled;
  }

  public LayoutItemColumn getLastColumn() {
    if (lColumns.size() == 0) {
      return null;
    }

    return lColumns.get(lColumns.size() - 1);
  }
}
