package net.foxopen.fox.module.serialiser.pdf.elementcontainers;

import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;

/**
 * A container for elements to be added to a cell. The elements are added to a paragraph within the cell, to ensure that
 * text behaves correctly (e.g. it is able to apply the correct leading at the top of the cell).
 */
public class CellContainer implements ElementContainer {
  private final PdfPCell mCell;
  private final Paragraph mCellParagraph;

  /**
   * Create an element container that adds elements to a cell. The elements are added to a paragraph which is then added
   * to the cell itself when the container is ended. Having the cell wrap a paragraph means text can behave correctly,
   * e.g. leading at the top of the cell will be correct.
   * @param pCell The containing cell that the cell paragraph is added to
   * @param pCellParagraph The containing paragraph that elements are added to
   */
  public CellContainer(PdfPCell pCell, Paragraph pCellParagraph) {
    mCell = pCell;
    mCellParagraph = pCellParagraph;
  }

  @Override
  public void addChildElement(Element pChildElement) {
    mCellParagraph.add(pChildElement);
  }

  @Override
  public void onEndContainer() {
    // Add the cell paragraph to the cell
    mCell.addElement(mCellParagraph);
  }
}
