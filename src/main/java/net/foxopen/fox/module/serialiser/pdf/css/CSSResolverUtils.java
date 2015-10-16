package net.foxopen.fox.module.serialiser.pdf.css;

import com.itextpdf.tool.xml.css.CssUtils;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Utility methods for CSS resolvers
 */
public class CSSResolverUtils {
  private CSSResolverUtils() {
  }

  /**
   * Convert HTML style dimensions to points. The value can contain the numeric value in pixels or define a specific
   * unit. Ems and exs are relative to the current font size taken from the provided element attributes.
   * @param pValue The string value to be parsed
   * @param pElementAttributes The element attributes containing the current font size
   * @return The value in points
   * @see com.itextpdf.tool.xml.css.CssUtils#parseValueToPt
   */
  public static final float parseValueToPt(String pValue, ElementAttributes pElementAttributes) {
    return CssUtils.getInstance().parseValueToPt(pValue, pElementAttributes.getFontAttributes().getSize());
  }
}
