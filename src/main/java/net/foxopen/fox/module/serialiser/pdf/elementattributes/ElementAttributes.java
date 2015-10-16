package net.foxopen.fox.module.serialiser.pdf.elementattributes;

/**
 * Contains attributes for various element types that should be applied when creating those elements
 * TODO WH: all the individual attributes have non-final members as they must be modified later, but this is a bit nasty
 * as it means someone may leave out setting something in a copy constructor for example. Could convert to a builder
 * which builds an immutable attribute set - would this be worth the extra abstraction however? It also means that in
 * the CSS resolvers that modify the attributes, builders must be created from the immutable attributes, which isn't great
 */
public class ElementAttributes {
  private final CellAttributes mCellAttributes;
  private final FontAttributes mFontAttributes;
  private final ParagraphAttributes mParagraphAttributes;
  private final TableAttributes mTableAttributes;

  /**
   * Create a set of element attributes inheriting from the given source attributes. All font attributes are inherited
   * from the source. Cell, Paragraph and Table attributes are not inherited, these are set to the default element
   * attributes.
   * @param lSourceAttributes The element attributes to inherit from
   * @return A set of inherited element attributes
   */
  public static ElementAttributes inheritFrom(ElementAttributes lSourceAttributes) {
    ElementAttributes lDefaultElementAttributes = DefaultElementAttributes.getDefaultAttributes();

    return new ElementAttributes(new CellAttributes(lDefaultElementAttributes.getCellAttributes()),
                                 new FontAttributes(lSourceAttributes.getFontAttributes()),
                                 new ParagraphAttributes(lDefaultElementAttributes.getParagraphAttributes()),
                                 new TableAttributes(lDefaultElementAttributes.getTableAttributes()));
  }

  /**
   * Create a set of element attributes with the given attributes
   * @param pCellAttributes The cell attributes
   * @param pFontAttributes The font attributes
   * @param pParagraphAttributes The paragraph attributes
   * @param pTableAttributes The table attributes
   */
  public ElementAttributes(CellAttributes pCellAttributes, FontAttributes pFontAttributes,
                           ParagraphAttributes pParagraphAttributes, TableAttributes pTableAttributes) {
    mCellAttributes = pCellAttributes;
    mFontAttributes = pFontAttributes;
    mParagraphAttributes = pParagraphAttributes;
    mTableAttributes = pTableAttributes;
  }

  /**
   * Create a deep copy of the given element attributes
   * @param pElementAttributes The element attributes to copy
   */
  public ElementAttributes(ElementAttributes pElementAttributes) {
    mCellAttributes = new CellAttributes(pElementAttributes.getCellAttributes());
    mFontAttributes = new FontAttributes(pElementAttributes.getFontAttributes());
    mParagraphAttributes = new ParagraphAttributes(pElementAttributes.getParagraphAttributes());
    mTableAttributes = new TableAttributes(pElementAttributes.getTableAttributes());
  }

  public CellAttributes getCellAttributes() {
    return mCellAttributes;
  }

  public FontAttributes getFontAttributes() {
    return mFontAttributes;
  }

  public TableAttributes getTableAttributes() {
    return mTableAttributes;
  }

  public ParagraphAttributes getParagraphAttributes() {
    return mParagraphAttributes;
  }
}
