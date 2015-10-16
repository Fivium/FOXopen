package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

/**
 * Serialises a div tag, which can also be used generically for other block-layout tags such as h1, p etc.
 */
public class DivComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new DivComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private DivComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    PdfPTable lTable = pSerialiser.getElementFactory().getTable(1);
    PdfPCell lCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lCell, lCellParagraph));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();

    lTable.addCell(lCell);
    pSerialiser.addTableSpacer();
    pSerialiser.add(lTable);
  }
}
