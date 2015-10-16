package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import net.foxopen.fox.module.serialiser.pdf.css.CSSResolverUtils;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Parses a property value specified as a HTML dimension to points and forwards resolving of the point value
 * @see CSSResolverUtils#parseValueToPt
 */
public abstract class ParsedPointValuePropertyResolver implements PropertyResolver {
  @Override
  public void apply(ElementAttributes pElementAttributes, String pValue) {
    float lPointValue = CSSResolverUtils.parseValueToPt(pValue, pElementAttributes);
    apply(pElementAttributes, lPointValue);
  }

  public abstract void apply(ElementAttributes pElementAttributes, float pPointValue);
}
