package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import com.itextpdf.text.Font;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves element attributes for the font weight CSS property. The supported value is {@code bold}.
 */
public class FontWeightPropertyResolver implements PropertyResolver {
  private static final String BOLD_PROPERTY_VALUE = "bold";

  @Override
  public void apply(ElementAttributes pElementAttributes, String pValue) {
    if (BOLD_PROPERTY_VALUE.equals(pValue)) {
      int lFontStyle = pElementAttributes.getFontAttributes().getStyle() | Font.BOLD;
      pElementAttributes.getFontAttributes().setStyle(lFontStyle);
    }
  }
}
