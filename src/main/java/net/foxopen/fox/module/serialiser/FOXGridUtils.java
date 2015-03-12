package net.foxopen.fox.module.serialiser;

import net.foxopen.fox.track.Track;

/**
 * Helper class for laying out items on the FOX Grid, which is currently locked to 12 columns.
 * Grid cells are marked up as: &lt;div class="{colspan} columns"&gt;...&lt;/div&gt; where {colspan} relates to COL_CLASS_NAMES
 */
public class FOXGridUtils {
  /**
   * Class names for the various column spans
   */
  private static final String[] COL_CLASS_NAMES = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve"};

  /**
   * Return the class name for the amount of columns needed
   *
   * @param pColSpan How many columns the current column spans
   * @param pMaxCols How many pseudo-columns there are in the grid
   * @return class name that defines the colspan for a FOX Grid
   */
  public static String calculateColumnClassName(int pColSpan, int pMaxCols) {
    if (pMaxCols > getMaxColumns()) {
      Track.alert("The FOX Grid currently supports 1-" + getMaxColumns() + " columns, asking for more will give unexpected behaviour: " + pMaxCols);
    }

    // Get column span of FOXGrid getMaxColumns() from pColSpan of pMaxCols
    int lAdjustedColumnSpan = (int)Math.floor((float)pColSpan * ((float)getMaxColumns() / (float)pMaxCols));

    // Get the CSS class name for the FOXGrid
    return COL_CLASS_NAMES[Math.min(11, Math.max(0, --lAdjustedColumnSpan))];
  }

  public static int getMaxColumns() {
    return COL_CLASS_NAMES.length;
  }
}
