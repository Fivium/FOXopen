package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import com.itextpdf.text.BaseColor;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves element attributes for the color CSS property
 */
public class ColorPropertyResolver extends ParsedColorValuePropertyResolver {
  @Override
  public void apply(ElementAttributes pElementAttributes, BaseColor pColor) {
    pElementAttributes.getFontAttributes().setColor(pColor);
  }
}
