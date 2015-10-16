package net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers;

import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.FontSelector;

/**
 * A placeholder for the current page number
 */
public class CurrentPageNumberPlaceholder extends AbstractPageNumberPlaceholder {
  /**
   * Create a placeholder for the current page number. The given phrase text will be set to the page number using the
   * provided font selector.
   * @param pPhrase The phrase that should contain the current page number once set
   * @param pFontSelector The font selector to use when setting the current page number
   */
  public CurrentPageNumberPlaceholder(Phrase pPhrase, FontSelector pFontSelector) {
    super(pPhrase, pFontSelector);
  }

  /**
   * Sets placeholder to the current page number
   * @param pCurrentPageNumber The current page number
   * @param pTotalPageCount The total page count
   */
  @Override
  public void setPageNumber(int pCurrentPageNumber, int pTotalPageCount) {
    setPageNumber(pCurrentPageNumber);
  }
}
