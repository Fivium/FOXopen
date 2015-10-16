package net.foxopen.fox.module.serialiser.pdf.pages;

import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfStamper;
import net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.PageNumberPlaceholder;

import java.util.List;

/**
 * Renders a header or footer to a completed document
 */
public abstract class HeaderFooterRenderer {
  private final HeaderFooterPositionStrategy mPositionStrategy;
  private final PageAttributes mPageAttributes;
  private final HeaderFooterContent mContent;
  private final List<PageNumberPlaceholder> mPageNumberPlaceholders;
  private final List<Integer> mPageNumbers;

  /**
   * Creates a header or footer renderer
   * @param pPositionStrategy The strategy for determining the Y position of the header or footer (this will differ
   *                          between a header and footer)
   * @param pPageAttributes The page attributes
   * @param pContent The header/footer content
   * @param pPageNumberPlaceholders A list of page number placeholders that should have the page numbers set when rendering
   * @param pPageNumbers A list of page numbers that the header/footer should be rendered on
   */
  public HeaderFooterRenderer(HeaderFooterPositionStrategy pPositionStrategy, PageAttributes pPageAttributes,
                              HeaderFooterContent pContent, List<PageNumberPlaceholder> pPageNumberPlaceholders,
                              List<Integer> pPageNumbers) {
    mPositionStrategy = pPositionStrategy;
    mPageAttributes = pPageAttributes;
    mContent = pContent;
    mPageNumberPlaceholders = pPageNumberPlaceholders;
    mPageNumbers = pPageNumbers;
  }

  /**
   * Render the header/footer to the document on the pages provided in the constructor. Page number placeholders are
   * set during rendering.
   * @param pStamper The stamper used to manipulate the completed document
   */
  public void render(PdfStamper pStamper) {
    PdfPTable lTable = getContentTable();
    float lXPosition = mPageAttributes.getMarginLeft();
    float lYPosition = mPositionStrategy.getYPosition(mPageAttributes, lTable.getTotalHeight());
    int lTotalPageCount = pStamper.getReader().getNumberOfPages();

    mPageNumbers.forEach(pPageNumber -> {
      mPageNumberPlaceholders.forEach(pPageNumberPlaceholder -> pPageNumberPlaceholder.setPageNumber(pPageNumber, lTotalPageCount));
      lTable.writeSelectedRows(0, -1, lXPosition, lYPosition, pStamper.getOverContent(pPageNumber));
    });
  }

  /**
   * Get the header/footer content table, set to the page width up to the page margins
   * @return The header/footer content table set to the correct width based on the page attributes
   */
  private PdfPTable getContentTable() {
    PdfPTable lTable = mContent.getContentTable();
    float lContentWidth = mPageAttributes.getPageWidth() - mPageAttributes.getMarginLeft() - mPageAttributes.getMarginRight();
    lTable.setTotalWidth(lContentWidth);

    return lTable;
  }
}
