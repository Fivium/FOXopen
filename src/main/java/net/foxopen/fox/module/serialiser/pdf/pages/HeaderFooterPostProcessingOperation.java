package net.foxopen.fox.module.serialiser.pdf.pages;

import com.itextpdf.text.pdf.PdfStamper;
import net.foxopen.fox.module.serialiser.pdf.postprocessing.PostProcessingOperation;

/**
 * Renders headers and footers after the document has completed its first pass
 */
public class HeaderFooterPostProcessingOperation implements PostProcessingOperation {
  private final PageManager mPageManager;

  /**
   * Create a post processing operationt that renders headers and footers on the document
   * @param pPageManager The page manager that contains the headers and footers added during module parsing
   */
  public HeaderFooterPostProcessingOperation(PageManager pPageManager) {
    mPageManager = pPageManager;
  }

  /**
   * Render headers and footers added during the first pass to the document
   * @param pStamper The document stamper
   */
  @Override
  public void process(PdfStamper pStamper) {
    mPageManager.renderHeadersFooters(pStamper);
  }
}
