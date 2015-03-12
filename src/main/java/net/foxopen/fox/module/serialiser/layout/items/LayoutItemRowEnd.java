package net.foxopen.fox.module.serialiser.layout.items;

public class LayoutItemRowEnd implements LayoutItem {
  public LayoutItemRowEnd() {
    super();
  }

  @Override
  public LayoutItemEnum getItemType() {
    return LayoutItemEnum.ROW_END;
  }
}
