package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import com.itextpdf.text.BaseColor;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves element attributes for the background color CSS property
 */
public class BackgroundColorPropertyResolver extends ParsedColorValuePropertyResolver {
  @Override
  public void apply(ElementAttributes pElementAttributes, BaseColor pColor) {
    pElementAttributes.getCellAttributes().setBackgroundColor(pColor);
  }
}
