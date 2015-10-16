package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves element attributes for the page break inside CSS property. The supported value is {@code avoid}. Any other
 * values will allow page breaks.
 */
public class PageBreakInsidePropertyResolver implements PropertyResolver {
  private static final String PAGE_BREAK_AVOID = "avoid";

  @Override
  public void apply(ElementAttributes pElementAttributes, String pValue) {
    boolean lIsAvoidPageBreak = PAGE_BREAK_AVOID.equals(pValue);
    pElementAttributes.getTableAttributes().setKeepTogether(lIsAvoidPageBreak);
  }
}
