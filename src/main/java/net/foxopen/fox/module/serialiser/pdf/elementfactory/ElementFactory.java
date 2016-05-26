package net.foxopen.fox.module.serialiser.pdf.elementfactory;

import com.itextpdf.text.Font;
import com.itextpdf.text.List;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.draw.LineSeparator;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.FontAttributes;

/**
 * A factory for {@link com.itextpdf.text.Element} instances with element attributes applied
 */
public class ElementFactory {
  private final ElementAttributes mElementAttributes;

  /**
   * Create an element factory where the provided element attributes are used to create the various elements
   * @param pElementAttributes The element attributes to be applied when creating elements.
   */
  public ElementFactory(ElementAttributes pElementAttributes) {
    mElementAttributes = pElementAttributes;
  }

  /**
   * Create a phrase with the factory element attributes applied
   * @return A phrase with the element attributes applied
   */
  public Phrase getPhrase() {
    Phrase lPhrase = new Phrase();
    setFontAttributes(lPhrase.getFont(), mElementAttributes.getFontAttributes());
    lPhrase.setMultipliedLeading(mElementAttributes.getFontAttributes().getMultipliedLeading());

    return lPhrase;
  }

  /**
   * Create a paragraph with the factory element attributes applied
   * @return A paragraph with the element attributes applied
   */
  public Paragraph getParagraph() {
    Paragraph lParagraph = new Paragraph();
    setFontAttributes(lParagraph.getFont(), mElementAttributes.getFontAttributes());
    lParagraph.setMultipliedLeading(mElementAttributes.getFontAttributes().getMultipliedLeading());
    lParagraph.setAlignment(mElementAttributes.getParagraphAttributes().getAlignment());

    return lParagraph;
  }

  /**
   * Create a cell with the factory element attributes applied
   * @return A cell with the element attributes applied
   */
  public PdfPCell getCell() {
    PdfPCell lCell = new PdfPCell();
    lCell.setUseAscender(false);
    lCell.setUseDescender(true);
    lCell.getColumn().setAdjustFirstLine(mElementAttributes.getCellAttributes().isAdjustFirstLine());
    lCell.setBorder(mElementAttributes.getCellAttributes().getBorder());
    lCell.setPaddingTop(mElementAttributes.getCellAttributes().getPaddingTop());
    lCell.setPaddingRight(mElementAttributes.getCellAttributes().getPaddingRight());
    lCell.setPaddingBottom(mElementAttributes.getCellAttributes().getPaddingBottom());
    lCell.setPaddingLeft(mElementAttributes.getCellAttributes().getPaddingLeft());
    lCell.setBackgroundColor(mElementAttributes.getCellAttributes().getBackgroundColor());

    return lCell;
  }

  /**
   * Create a table with the factory element attributes applied
   * @return A table with the element attributes applied
   */
  public PdfPTable getTable(int pColumns) {
    PdfPTable lTable = new PdfPTable(pColumns);
    // Set split late to false so that tables will be split immediately if they don't fit on a page instead of moving
    // them to the next page then attempting to split if they don't fit there. iText gets in an infinite loop with the
    // latter option when content in nested table rows can't split (see EDU-3490 on JIRA).
    lTable.setSplitLate(false);
    mElementAttributes.getTableAttributes().getWidthPercentage().ifPresent(pWidthPercentage -> {
      lTable.setWidthPercentage(pWidthPercentage);
      lTable.setLockedWidth(false);
    });
    mElementAttributes.getTableAttributes().getFixedWidth().ifPresent(pFixedWidth -> {
      lTable.setTotalWidth(pFixedWidth);
      lTable.setLockedWidth(true);
    });
    lTable.setHorizontalAlignment(mElementAttributes.getTableAttributes().getHorizontalAlignment());
    lTable.setSpacingBefore(mElementAttributes.getTableAttributes().getSpacingBefore());
    lTable.setSpacingAfter(mElementAttributes.getTableAttributes().getSpacingAfter());
    lTable.setKeepTogether(mElementAttributes.getTableAttributes().isKeepTogether());

    return lTable;
  }

  /**
   * Create a list with the factory element attributes applied
   * @return A list with the element attributes applied
   */
  public List getList() {
    List lList = new List();
    return lList;
  }

  /**
   * Create a list item with the factory element attributes applied
   * @return A list item with the element attributes applied
   */
  public ListItem getListItem() {
    ListItem lListItem = new ListItem();
    return lListItem;
  }

  /**
   * Create a line separator item with the factory element attributes applied
   * @return A line separator item with the element attributes applied
   */
  public LineSeparator getLineSeparator() {
    LineSeparator lLineSeparator = new LineSeparator();
    return lLineSeparator;
  }

  /**
   * Apply font attributes to a font
   * @param pFont The font to be modified
   * @param pFontAttributes The attributes to be applied
   */
  private void setFontAttributes(Font pFont, FontAttributes pFontAttributes) {
    // Note that font difference must be used instead of applying the changes directly to the font, as font difference
    // will take into account resolution of font families for styling (bold, italic fonts etc.). For example setting
    // the style to bold directly may lead to a case where a bold font (e.g. "Open Sans Bold") is used along with the
    // bold style - iText will then attempt to faux-bold the text due to the bold style, even though it is already using
    // a bold font.
    pFont = pFont.difference(new Font(Font.FontFamily.UNDEFINED, pFontAttributes.getSize(), pFontAttributes.getStyle(),
                                      pFontAttributes.getColor()));
  }
}
