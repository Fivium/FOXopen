package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves CSS property values to element attributes
 */
@FunctionalInterface
public interface PropertyResolver {
  /**
   * Applies changes to the element attributes based on the property value
   * @param pElementAttributes The element attributes to be modified
   * @param pValue The CSS property value
   */
  public void apply(ElementAttributes pElementAttributes, String pValue);
}
