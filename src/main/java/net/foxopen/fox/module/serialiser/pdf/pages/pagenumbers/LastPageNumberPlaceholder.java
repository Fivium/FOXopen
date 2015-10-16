package net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers;

import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.FontSelector;

/**
 * A placeholder for the last page number
 */
public class LastPageNumberPlaceholder extends AbstractPageNumberPlaceholder {
  /**
   * Create a placeholder for the last page number. The given phrase text will be set to the page number using the
   * provided font selector.
   * @param pPhrase The phrase that should contain the last page number once set
   * @param pFontSelector The font selector to use when setting the last page number
   */
  public LastPageNumberPlaceholder(Phrase pPhrase, FontSelector pFontSelector) {
    super(pPhrase, pFontSelector);
  }

  /**
   * Sets placeholder to the last page number (i.e. the total page count)
   * @param pCurrentPageNumber The current page number
   * @param pTotalPageCount The total page count
   */
  @Override
  public void setPageNumber(int pCurrentPageNumber, int pTotalPageCount) {
    setPageNumber(pTotalPageCount);
  }
}
