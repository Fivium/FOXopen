package net.foxopen.fox.module.serialiser.pdf.pages;

/**
 * Strategy for determining the Y position of a header or footer on a page
 */
@FunctionalInterface
public interface HeaderFooterPositionStrategy {
  /**
   * Get the Y position of the header or footer in the page. Y starts at 0 from the bottom of the page.
   * @param pPageAttributes The page attributes
   * @param pContentHeight The height of the header/footer content
   * @return The Y position the header or footer should be placed at
   */
  public float getYPosition(PageAttributes pPageAttributes, float pContentHeight);
}
