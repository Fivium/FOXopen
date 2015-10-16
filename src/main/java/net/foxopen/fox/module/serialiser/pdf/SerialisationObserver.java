package net.foxopen.fox.module.serialiser.pdf;

import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPCell;

/**
 * Observers PDF serialisation events
 */
public interface SerialisationObserver {
  /**
   * Notifies that the given element is about to be serialised. This method is invoked before the element is added to
   * the current container within the serialiser.
   * @param pElement The element amount to be serialised
   */
  public void beforeAddElement(Element pElement);

  /**
   * Notifies that the given cell is about to be serialised. This method is invoked before the cell is added to the
   * current container within the serialiser.
   * @param pCell The element amount to be serialised
   */
  public default void beforeAddCell(PdfPCell pCell) {
    beforeAddElement(pCell);
  }
}
