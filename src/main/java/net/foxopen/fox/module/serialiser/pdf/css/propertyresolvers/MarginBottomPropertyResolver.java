package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves element attributes for the margin bottom CSS property
 */
public class MarginBottomPropertyResolver extends ParsedPointValuePropertyResolver {
  @Override
  public void apply(ElementAttributes pElementAttributes, float pPointValue) {
    pElementAttributes.getTableAttributes().setSpacingAfter(pPointValue);
  }
}
