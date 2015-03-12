package net.foxopen.fox.module.serialiser.layout.items;

/**
 * Interface for general column items. All columns have a ColSpan porperty that should be set/gettable
 */
public interface LayoutItemColumn extends LayoutItem {
  public int getColSpan();
  public boolean isFiller();
}
