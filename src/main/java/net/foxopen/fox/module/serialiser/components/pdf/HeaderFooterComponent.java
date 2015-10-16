package net.foxopen.fox.module.serialiser.components.pdf;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHeaderFooterPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;
import net.foxopen.fox.module.serialiser.pdf.pages.HeaderFooterContent;

import java.util.Collections;
import java.util.function.BiConsumer;

/**
 * Serialises a header
 * @param <EPN> An EvaluatedHeaderFooterPresentationNode implementation
 */
public class HeaderFooterComponent<EPN extends EvaluatedHeaderFooterPresentationNode> extends ComponentBuilder<PDFSerialiser, EPN> {
  private final String mTag;
  private final BiConsumer<PDFSerialiser, HeaderFooterContent> mHeaderFooterContentConsumer;

  /**
   * Creates a header or footer component with the given HTML tag and content consumer. The content consumer should
   * serialise the header or footer content.
   * @param pTag The HTML tag
   * @param pHeaderFooterContentConsumer A function that will serialise the header/footer content
   */
  protected HeaderFooterComponent(String pTag, BiConsumer<PDFSerialiser, HeaderFooterContent> pHeaderFooterContentConsumer) {
    mTag = pTag;
    mHeaderFooterContentConsumer = pHeaderFooterContentConsumer;
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EPN pEvalNode) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, mTag,
                                               Collections.singletonList(pEvalNode.getClasses()),
                                               Collections.singletonList(pEvalNode.getStyles()));
    pSerialiser.pushElementAttributes(lElementAttributes);

    // Add header/footer content to a table
    PdfPTable lTable = pSerialiser.getElementFactory().getTable(1);
    PdfPCell lCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lCell, lCellParagraph));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();

    lTable.addCell(lCell);

    // Consume the header/footer content (i.e. set the header or footer content via the serialiser)
    mHeaderFooterContentConsumer.accept(pSerialiser, new HeaderFooterContent(lTable));
    pSerialiser.popElementAttributes();
  }
}
