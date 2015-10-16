package net.foxopen.fox.module.serialiser.pdf.elementcontainers;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.TextElementArray;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

/**
 * Factory methods to get a container for various elements
 */
public class ElementContainerFactory {
  private ElementContainerFactory() {
  }

  /**
   * Get the container to add elements to a document
   * @param pDocument The document
   * @return A container to add elements to a document
   */
  public static ElementContainer getContainer(Document pDocument) {
    return new DocumentContainer(pDocument);
  }

  /**
   * Get the container to add elements to a text element array
   * @param pTextElementArray The text element array
   * @return A container to add elements to a text element array
   */
  public static ElementContainer getContainer(TextElementArray pTextElementArray) {
    return new TextElementArrayContainer(pTextElementArray);
  }

  /**
   * Get the container to add elements to a table
   * @param pTable The table
   * @return A container to add elements to a table
   */
  public static ElementContainer getContainer(PdfPTable pTable) {
    return new TableContainer(pTable);
  }

  /**
   * Get the container to add elements to a cell. Elements are added to a paragraph which is then added to the cell when
   * the container ends. The wrapping paragraph is used to ensure the text leading behaves correctly between table
   * cells.
   * @param pCell The cell
   * @param pCellParagraph The paragraph that wraps the elements and will be added to the cell when the container ends
   * @return The container to add elements to a cell
   */
  public static ElementContainer getContainer(PdfPCell pCell, Paragraph pCellParagraph) {
    return new CellContainer(pCell, pCellParagraph);
  }
}
