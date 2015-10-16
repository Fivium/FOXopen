package net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers;

import net.foxopen.fox.module.serialiser.pdf.elementattributes.CellAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

import java.util.function.BiConsumer;

/**
 * Resolves element attributes for padding CSS properties
 */
public class PaddingPropertyResolver extends ParsedPointValuePropertyResolver {
  private final BiConsumer<CellAttributes, Float> mPaddingSetter;

  /**
   * Creates a padding property resolver which sets padding using the specified setter
   * @param pPaddingSetter The padding setter
   */
  public PaddingPropertyResolver(BiConsumer<CellAttributes, Float> pPaddingSetter) {
    mPaddingSetter = pPaddingSetter;
  }

  @Override
  public void apply(ElementAttributes pElementAttributes, float pPointValue) {
    mPaddingSetter.accept(pElementAttributes.getCellAttributes(), pPointValue);
  }
}
