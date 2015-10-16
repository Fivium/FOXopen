package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves element attributes for the margin top CSS property
 */
public class MarginTopPropertyResolver extends ParsedPointValuePropertyResolver {
  @Override
  public void apply(ElementAttributes pElementAttributes, float pPointValue) {
    pElementAttributes.getTableAttributes().setSpacingBefore(pPointValue);
  }
}
