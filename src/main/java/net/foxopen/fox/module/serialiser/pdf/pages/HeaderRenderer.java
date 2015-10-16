package net.foxopen.fox.module.serialiser.pdf.pages;

import net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.PageNumberPlaceholder;

import java.util.List;

/**
 * Renders a header to a completed document
 */
public class HeaderRenderer extends HeaderFooterRenderer {
  /**
   * Header content is positioned from the page top margin. This means content flows down into the page if the content
   * is taller than the footer height.
   */
  private static final HeaderFooterPositionStrategy POSITION_STRATEGY = (pPageAttributes, pContentHeight) -> pPageAttributes.getPageHeight() - pPageAttributes.getMarginTop();

  /**
   * Creates a header renderer
   * @param pPageAttributes The page attributes
   * @param pContent The header/footer content
   * @param pPageNumberPlaceholders A list of page number placeholders that should have the page numbers set when rendering
   * @param pPageNumbers A list of page numbers that the header/footer should be rendered on
   */
  public HeaderRenderer(PageAttributes pPageAttributes, HeaderFooterContent pContent,
                        List<PageNumberPlaceholder> pPageNumberPlaceholders, List<Integer> pPageNumbers) {
    super(POSITION_STRATEGY, pPageAttributes, pContent, pPageNumberPlaceholders, pPageNumbers);
  }
}
