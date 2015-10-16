package net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers;

import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.FontSelector;

/**
 * A placeholder that adds page numbers as text to a containing phrase. This is essentially a wrapper around
 * {@link net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.TextPlaceholder} that converts the page number to
 * text.
 */
public abstract class AbstractPageNumberPlaceholder implements PageNumberPlaceholder {
  private final TextPlaceholder mTextPlaceholder;

  /**
   * Create a page number placeholder so that the given phrase text may be set to the page number
   * @param pPhrase The phrase that should contain the page number once set
   * @param pFontSelector The font selector to use when setting the page number
   */
  public AbstractPageNumberPlaceholder(Phrase pPhrase, FontSelector pFontSelector) {
    mTextPlaceholder = new TextPlaceholder(pPhrase, pFontSelector);
  }

  /**
   * Set the placeholder to the given page number
   * @param pPageNumber The page number
   */
  protected void setPageNumber(int pPageNumber) {
    mTextPlaceholder.setText(Integer.toString(pPageNumber));
  }
}
