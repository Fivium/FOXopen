package net.foxopen.fox.module.serialiser.components.pdf;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHeadingPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

import java.util.Collections;

/**
 * Serialises a heading
 */
public class HeadingComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHeadingPresentationNode> {
  /**
   * The string to be prefixed to the heading level to create the html tag for the heading, e.g. h1, h2 etc.
   */
  public static final String HEADING_TAG_PREFIX = "h";
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHeadingPresentationNode> INSTANCE = new HeadingComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHeadingPresentationNode> getInstance() {
    return INSTANCE;
  }

  private HeadingComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHeadingPresentationNode pEvalNode) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, HEADING_TAG_PREFIX + pEvalNode.getLevel(),
                                               Collections.singletonList(pEvalNode.getClasses()),
                                               Collections.singletonList(pEvalNode.getStyles()));
    pSerialiser.pushElementAttributes(lElementAttributes);

    // Heading text is contained within a table so that it may be styled as a block-level element (i.e. the same as a
    // div with borders, background colours etc.)
    PdfPTable lTable = pSerialiser.getElementFactory().getTable(1);
    PdfPCell lCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lCell, lCellParagraph));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();

    lTable.addCell(lCell);
    // A table spacer is required so any content before the heading is not overlapped
    pSerialiser.addTableSpacer();
    pSerialiser.add(lTable);

    pSerialiser.popElementAttributes();
  }
}
