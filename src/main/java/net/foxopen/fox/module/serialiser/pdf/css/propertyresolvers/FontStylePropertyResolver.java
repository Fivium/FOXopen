package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import com.itextpdf.text.Font;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves element attributes for the font style CSS property. The supported value is {@code italic}.
 */
public class FontStylePropertyResolver implements PropertyResolver {
  private static final String ITALIC_PROPERTY_VALUE = "italic";

  @Override
  public void apply(ElementAttributes pElementAttributes, String pValue) {
    if (ITALIC_PROPERTY_VALUE.equals(pValue)) {
      int lFontStyle = pElementAttributes.getFontAttributes().getStyle() | Font.ITALIC;
      pElementAttributes.getFontAttributes().setStyle(lFontStyle);
    }
  }
}
