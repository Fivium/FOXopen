package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.html.HtmlUtilities;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Parses a property value specifying a color and forwards resolving of the parsed color
 * @see HtmlUtilities#decodeColor
 */
public abstract class ParsedColorValuePropertyResolver implements PropertyResolver {
  @Override
  public void apply(ElementAttributes pElementAttributes, String pValue) {
    apply(pElementAttributes, HtmlUtilities.decodeColor(pValue));
  }

  public abstract void apply(ElementAttributes pElementAttributes, BaseColor pColor);
}
