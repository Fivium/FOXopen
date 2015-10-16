package net.foxopen.fox.module.serialiser.widgets.pdf;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Wraps widget content in an input field div
 * @param <EN> The {@link EvaluatedNode} type of the widget
 */
public class InputField<EN extends EvaluatedNode> {
  private static final String INPUT_FIELD_TAG = "div";
  private static final String INPUT_FIELD_CLASS = "inputField";
  private static final String TIGHT_FIELD_CLASS = "tightField";
  private static final String ZERO_WIDTH_SPACE_CHARACTER = "\u200B";

  private final InputFieldContent<EN> mInputFieldContent;
  private final boolean mIsTightField;
  private final List<String> mAdditionalFieldClasses;

  /**
   * Creates a wrapper div for input field content
   * @param pInputFieldContent The content serialiser that is added to the input field div when serialised
   * @param pIsTightField Whether or not the field is a tight field (i.e. it should not be 100% width)
   * @param pAdditionalFieldClasses A list of additional classes to be applied to the input field
   */
  public InputField(InputFieldContent<EN> pInputFieldContent, boolean pIsTightField, List<String> pAdditionalFieldClasses) {
    mInputFieldContent = pInputFieldContent;
    mIsTightField = pIsTightField;
    mAdditionalFieldClasses = pAdditionalFieldClasses;
  }

  /**
   * Serialise the input field div and content
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pEvalNode
   */
  public void serialise(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EN pEvalNode) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, INPUT_FIELD_TAG, getInputFieldClasses(), Collections.emptyList());
    pSerialiser.pushElementAttributes(lElementAttributes);

    PdfPTable lTable = pSerialiser.getElementFactory().getTable(1);
    PdfPCell lCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lCell, lCellParagraph));
    mInputFieldContent.addContent(pSerialisationContext, pSerialiser, pEvalNode);
    if (lCellParagraph.isEmpty()) {
      // If there is no cell content in all cells in a table row, the row will be 0 height - add a zero width space to
      // ensure that the input field div isn't collapsed like this (the height will be determined from the cell
      // paragraph font)
      lCellParagraph.add(ZERO_WIDTH_SPACE_CHARACTER);
    }
    pSerialiser.endContainer();

    lTable.addCell(lCell);
    pSerialiser.add(lTable);

    pSerialiser.popElementAttributes();
  }

  /**
   * Returns the classes that should be applied when resolving input field styles. This includes the input field class
   * added to all input fields, the tight field class if appropriate, and any of the specified additional classes.
   * @return The classes that should be applied when resolving input field styles
   */
  private List<String> getInputFieldClasses() {
    List<String> lClasses = new LinkedList<>(mAdditionalFieldClasses);
    lClasses.add(INPUT_FIELD_CLASS);

    if (mIsTightField) {
      lClasses.add(TIGHT_FIELD_CLASS);
    }

    return lClasses;
  }
}
