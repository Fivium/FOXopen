package net.foxopen.fox.module.serialiser.pdf.pages;

import com.itextpdf.text.pdf.PdfPTable;

/**
 * Contains the content of a header or footer
 */
public class HeaderFooterContent {
  private final PdfPTable mContentTable;

  /**
   * Create header or footer content using the table element that contains the elements
   * @param pContentTable The table that contains the header/footer content
   */
  public HeaderFooterContent(PdfPTable pContentTable) {
    mContentTable = pContentTable;
  }

  /**
   * Get the table that contains the header/footer content
   * @return The table that contains the header/footer content
   */
  public PdfPTable getContentTable() {
    return mContentTable;
  }
}
