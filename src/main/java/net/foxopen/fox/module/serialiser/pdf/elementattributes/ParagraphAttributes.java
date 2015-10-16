package net.foxopen.fox.module.serialiser.pdf.elementattributes;

/**
 * Contains the set of attributes used when adding a paragraph to the document
 */
public class ParagraphAttributes {
  private int mAlignment;

  /**
   * Create a set of paragraph attributes
   * @param pAlignment The alignment of the paragraph, see {@link com.itextpdf.text.Paragraph#setAlignment}
   */
  public ParagraphAttributes(int pAlignment) {
    mAlignment = pAlignment;
  }

  /**
   * Create a deep copy of the paragraph attributes
   * @param pParagraphAttributes The paragraph attributes to copy
   */
  public ParagraphAttributes(ParagraphAttributes pParagraphAttributes) {
    mAlignment = pParagraphAttributes.getAlignment();
  }

  public int getAlignment() {
    return mAlignment;
  }

  public void setAlignment(int pAlignment) {
    mAlignment = pAlignment;
  }
}
