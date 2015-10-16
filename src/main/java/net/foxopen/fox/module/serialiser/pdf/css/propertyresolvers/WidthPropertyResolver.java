package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves element attributes for the width CSS property. Only fixed values are supported (i.e. not percentages).
 */
public class WidthPropertyResolver extends ParsedPointValuePropertyResolver {
  @Override
  public void apply(ElementAttributes pElementAttributes, float pPointValue) {
    pElementAttributes.getTableAttributes().setFixedWidth(pPointValue);
  }
}
