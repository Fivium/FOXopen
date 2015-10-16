package net.foxopen.fox.module.serialiser;

import net.foxopen.fox.track.Track;

/**
 * Helper class for laying out items on the FOX Grid, which is currently locked to 12 columns.
 * HTML serialiser: Grid cells are marked up as: &lt;div class="{colspan} columns"&gt;...&lt;/div&gt; where {colspan}
 * relates to COL_CLASS_NAMES
 */
public class FOXGridUtils {
  /**
   * Class names for the various column spans
   */
  private static final String[] COL_CLASS_NAMES = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve"};

  /**
   * Return the column span needed to correctly fit a column with pColSpan into a grid of pMaxCols
   *
   * @param pColSpan How many columns the current column spans
   * @param pMaxCols How many pseudo-columns there are in the grid
   * @return column span needed to correctly fit a column with pColSpan into a grid of pMaxCols
   */
  public static int calculateAdjustedColumnSpan(int pColSpan, int pMaxCols) {
    if (pMaxCols > getMaxColumns()) {
      Track.alert("The FOX Grid currently supports 1-" + getMaxColumns() + " columns, asking for more will give unexpected behaviour: " + pMaxCols);
    }

    return (int)Math.floor((float)pColSpan * ((float)getMaxColumns() / (float)pMaxCols));
  }

  /**
   * Return the class name for the amount of columns needed
   *
   * @param pColSpan How many columns the current column spans
   * @param pMaxCols How many pseudo-columns there are in the grid
   * @return class name that defines the colspan for a FOX Grid
   */
  public static String calculateColumnClassName(int pColSpan, int pMaxCols) {
    // Get class index from the adjusted column span, unless the input has caused the span to be out of the range
    // [0, 11] (where 11 is the maximum index of the column classes) in which case use the lower or upper index
    final int lAdjustedColumnSpan = calculateAdjustedColumnSpan(pColSpan, pMaxCols);
    final int lClassIndex = Math.max(0, Math.min(getMaxColumns() - 1, lAdjustedColumnSpan - 1));

    return COL_CLASS_NAMES[lClassIndex];
  }

  public static int getMaxColumns() {
    return COL_CLASS_NAMES.length;
  }
}
