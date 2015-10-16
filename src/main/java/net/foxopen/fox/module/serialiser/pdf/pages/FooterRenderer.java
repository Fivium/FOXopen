package net.foxopen.fox.module.serialiser.pdf.pages;

import net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.PageNumberPlaceholder;

import java.util.List;

/**
 * Renders a footer to a completed document
 */
public class FooterRenderer extends HeaderFooterRenderer {
  /**
   * Footer content is positioned so the bottom of the content is at the bottom page margin. This means content flows up
   * into the page if the content is taller than the footer height.
   */
  private static final HeaderFooterPositionStrategy POSITION_STRATEGY = (pPageAttributes, pContentHeight) -> pPageAttributes.getMarginBottom() + pContentHeight;

  /**
   * Creates a footer renderer
   * @param pPageAttributes The page attributes
   * @param pContent The header/footer content
   * @param pPageNumberPlaceholders A list of page number placeholders that should have the page numbers set when rendering
   * @param pPageNumbers A list of page numbers that the header/footer should be rendered on
   */
  public FooterRenderer(PageAttributes pPageAttributes, HeaderFooterContent pContent,
                        List<PageNumberPlaceholder> pPageNumberPlaceholders, List<Integer> pPageNumbers) {
    super(POSITION_STRATEGY, pPageAttributes, pContent, pPageNumberPlaceholders, pPageNumbers);
  }
}
