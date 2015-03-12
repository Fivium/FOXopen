package net.foxopen.fox.module.serialiser.layout;

import net.foxopen.fox.module.serialiser.layout.items.LayoutItem;

import java.util.List;

public interface LayoutResult {
  /**
   * Get a flat list of the LayoutItems, such as row start/end and columns, to be serialised out
   *
   * @return List of LayoutItems
   */
  public List<LayoutItem> getLayoutItems();

  /**
   * Get a count of the Rows the LayoutMethod created for the given columns
   *
   * @return Count of Row Start LayoutItems
   */
  public int getRowCount();

  /**
   * Get a count of all the columns the LayoutMethod created that are not filler columns. This includes both widget fields
   * and prompts too.
   *
   * @return Count of non-filler column items (not cumulative colspan)
   */
  public int getFilledColumnCount();

  /**
   * Get a count of all the columns the LayoutMethod created that have a prompt in them
   *
   * @return Count of prompt column items (not cumulative colspan)
   */
  public int getPromptColumnCount();

  /**
   * Get a count of all the columns the LayoutMethod created that have a widget in them
   *
   * @return Count of widget column items (not cumulative colspan)
   */
  public int getWidgetColumnCount();

  /**
   * Get a count of all the columns the LayoutMethod created that are just filler columns to pad out a row
   *
   * @return Count of filler column items (not cumulative colspan)
   */
  public int getFillerColumnCount();
}
