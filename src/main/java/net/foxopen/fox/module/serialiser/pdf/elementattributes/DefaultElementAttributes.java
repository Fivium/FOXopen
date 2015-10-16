package net.foxopen.fox.module.serialiser.pdf.elementattributes;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfPCell;

import java.util.Optional;

/**
 * Defines static methods for generating the default element attributes
 */
public class DefaultElementAttributes {
  private DefaultElementAttributes() {
  }

  /**
   * Get the default element attributes used when creating document elements
   * @return The set of default element attributes
   */
  public static ElementAttributes getDefaultAttributes() {
    CellAttributes lCellAttributes = new CellAttributes(true, PdfPCell.NO_BORDER, 0f, null);
    FontAttributes lFontAttributes = new FontAttributes(11, Font.NORMAL, BaseColor.BLACK, 1.2f);
    ParagraphAttributes lParagraphAttributes = new ParagraphAttributes(Element.ALIGN_LEFT);
    TableAttributes lTableAttributes = new TableAttributes(Optional.of(100f), Optional.empty(), Element.ALIGN_LEFT, 0f, 0f, false);

    return new ElementAttributes(lCellAttributes, lFontAttributes, lParagraphAttributes, lTableAttributes);
  }
}
