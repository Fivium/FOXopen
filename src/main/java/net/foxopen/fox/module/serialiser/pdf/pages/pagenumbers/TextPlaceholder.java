package net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers;

import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.FontSelector;

/**
 * A placeholder for text so that a phrase may be added and then modified. Note that setting the text after the phrase
 * has been serialised to an output stream will have no effect. Placeholders are used for when the text is unknown when
 * the phrase is added, but is known and can be set before the phrase is serialised (e.g. added to a document).
 */
public class TextPlaceholder {
  private final Phrase mPhrase;
  private final FontSelector mFontSelector;

  /**
   * Create a text placeholder so that the given phrase text may be set later using the provided font selector
   * @param pPhrase The phrase that should contain the text once set
   * @param pFontSelector The font selector to use when setting the text
   */
  public TextPlaceholder(Phrase pPhrase, FontSelector pFontSelector) {
    mPhrase = pPhrase;
    mFontSelector = pFontSelector;
  }

  /**
   * Set the phrase text. If the phrase already contains text this text is first cleared.
   * @param pText The new phrase text
   */
  public void setText(String pText) {
    mPhrase.clear();
    mPhrase.add(mFontSelector.process(pText));
  }
}
