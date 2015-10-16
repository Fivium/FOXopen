package net.foxopen.fox.module.serialiser.pdf.elementcontainers;

import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPCell;

/**
 * A container that elements may be added to
 */
public interface ElementContainer {
  /**
   * Add an element to the container
   * @param pChildElement The element to be added
   */
  public void addChildElement(Element pChildElement);

  /**
   * Add a cell to the container. Cells may be treated differently from other elements by implementing this method. By
   * default this forwards to {@link #addChildElement}.
   * @param pCell
   */
  public default void addChildCell(PdfPCell pCell) {
    addChildElement(pCell);
  }

  /**
   * Called immediately after the container has been ended (i.e. removed from the container stack).
   */
  public default void onEndContainer() {
  }
}
