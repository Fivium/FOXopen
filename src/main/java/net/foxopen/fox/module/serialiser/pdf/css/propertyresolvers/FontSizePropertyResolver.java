package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves element attributes for the font size CSS property
 */
public class FontSizePropertyResolver extends ParsedPointValuePropertyResolver {
  @Override
  public void apply(ElementAttributes pElementAttributes, float pPointValue) {
    pElementAttributes.getFontAttributes().setSize(pPointValue);
  }
}
