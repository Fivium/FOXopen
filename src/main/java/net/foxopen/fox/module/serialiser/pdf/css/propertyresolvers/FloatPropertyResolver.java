package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import com.itextpdf.text.Element;
import com.itextpdf.text.Rectangle;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves element attributes for the float color CSS property. Supported values are {@code left} and {@code right}.
 * Any other value will set the alignment to undefined.
 */
public class FloatPropertyResolver implements PropertyResolver {
  private static final int DEFAULT_ALIGNMENT = Rectangle.ALIGN_UNDEFINED;
  private static final Map<String, Integer> ALIGNMENTS = new HashMap<>();
  static {
    ALIGNMENTS.put("left", Element.ALIGN_LEFT);
    ALIGNMENTS.put("right", Element.ALIGN_RIGHT);
  }

  @Override
  public void apply(ElementAttributes pElementAttributes, String pValue) {
    int lAlignment = ALIGNMENTS.getOrDefault(pValue, DEFAULT_ALIGNMENT);
    pElementAttributes.getTableAttributes().setHorizontalAlignment(lAlignment);
  }
}
