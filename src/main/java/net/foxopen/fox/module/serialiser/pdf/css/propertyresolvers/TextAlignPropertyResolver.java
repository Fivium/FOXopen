package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import com.itextpdf.text.Element;
import com.itextpdf.text.Rectangle;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves element attributes for the text align CSS property. Supported values are {@code left}, {@code center},
 * {@code right} and {@code justify}. Any other values will set alignment to undefined.
 */
public class TextAlignPropertyResolver implements PropertyResolver {
  private static final int DEFAULT_ALIGNMENT = Rectangle.ALIGN_UNDEFINED;
  private static final Map<String, Integer> ALIGNMENTS = new HashMap<>();
  static {
    ALIGNMENTS.put("left", Element.ALIGN_LEFT);
    ALIGNMENTS.put("center", Element.ALIGN_CENTER);
    ALIGNMENTS.put("right", Element.ALIGN_RIGHT);
    ALIGNMENTS.put("justify", Element.ALIGN_JUSTIFIED);
  }

  @Override
  public void apply(ElementAttributes pElementAttributes, String pValue) {
    int lAlignment = ALIGNMENTS.getOrDefault(pValue, DEFAULT_ALIGNMENT);
    pElementAttributes.getParagraphAttributes().setAlignment(lAlignment);
  }
}
