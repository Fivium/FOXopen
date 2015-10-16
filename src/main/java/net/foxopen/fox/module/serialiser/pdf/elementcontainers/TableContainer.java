package net.foxopen.fox.module.serialiser.pdf.elementcontainers;

import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import net.foxopen.fox.ex.ExInternal;

/**
 * A container for cells to be added to a table. An exception if thrown if any element other than a cell is added. The
 * cell must be added via {@link #addChildCell}, adding a cell via {@link #addChildElement} will throw an exception even
 * if the element is an instance of a cell.
 */
public class TableContainer implements ElementContainer {
  private final PdfPTable mTable;

  /**
   * Creates a container to add elements to a table
   * @param pTable The table that elements should be added to
   */
  public TableContainer(PdfPTable pTable) {
    mTable = pTable;
  }

  /**
   * Unsupported for table containers. Only cells can be added to a table via {@link #addChildCell}. If the element
   * provided to this function is an instance of a PdfPCell the operation is still unsupported, {@link #addChildCell}
   * must be used.
   * @param pChildElement The element to be added
   * @throws ExInternal Always as this operation is unsupported for tables
   */
  @Override
  public void addChildElement(Element pChildElement) throws ExInternal {
    throw new ExInternal("Cannot not add element '" + pChildElement.toString() + "', only cells may be added to tables",
                         new UnsupportedOperationException());
  }

  /**
   * Add a cell to the table
   * @param pCell The cell to add
   */
  @Override
  public void addChildCell(PdfPCell pCell) {
    mTable.addCell(pCell);
  }
}
