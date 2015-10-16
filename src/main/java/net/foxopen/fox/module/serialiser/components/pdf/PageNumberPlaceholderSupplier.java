package net.foxopen.fox.module.serialiser.components.pdf;

import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.FontSelector;
import net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.PageNumberPlaceholder;

/**
 * Supplies a page number placeholder
 */
@FunctionalInterface
public interface PageNumberPlaceholderSupplier {
  /**
   * Returns a placeholder that will add a page number to the specified phrase using the given font selector
   * @param pPhrase The phrase that will contain the page number
   * @param pFontSelector The the font selector that should be used when adding the page number
   * @return A page number placeholder
   */
  PageNumberPlaceholder getPlaceholder(Phrase pPhrase, FontSelector pFontSelector);
}
