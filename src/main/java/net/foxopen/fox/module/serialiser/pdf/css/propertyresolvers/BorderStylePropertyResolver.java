package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

/**
 * Resolves element attributes for border properties
 */
public class BorderStylePropertyResolver implements PropertyResolver {
  private static final String SOLID_BORDER_STYLE = "solid";
  private final int mSide;

  /**
   * Creates a border property resolver for the specified the border side
   * @param pSide Which side the border is for, one of {@link com.itextpdf.text.Rectangle#TOP},
   * {@link com.itextpdf.text.Rectangle#RIGHT}, {@link com.itextpdf.text.Rectangle#BOTTOM} or
   * {@link com.itextpdf.text.Rectangle#LEFT}
   */
  public BorderStylePropertyResolver(int pSide) {
    mSide = pSide;
  }

  /**
   * Modify the element border attributes to match the border property value
   * @param pElementAttributes The element attributes to be modified
   * @param pValue The border property value
   */
  @Override
  public void apply(ElementAttributes pElementAttributes, String pValue) {
    int lCurrentBorder = pElementAttributes.getCellAttributes().getBorder();
    int lNewBorder = SOLID_BORDER_STYLE.equals(pValue) ? addSide(lCurrentBorder) : removeSide(lCurrentBorder);
    pElementAttributes.getCellAttributes().setBorder(lNewBorder);
  }

  /**
   * Adds the side defined for this border resolver from the border flags
   * @param pBorder The current border flags
   * @return The border with the side added
   */
  private int addSide(int pBorder) {
    return pBorder | mSide;
  }

  /**
   * Removes the side defined for this border resolver from the border flags
   * @param pBorder The current border flags
   * @return The border with the side added
   */
  private int removeSide(int pBorder) {
    return pBorder & (~mSide);
  }
}
