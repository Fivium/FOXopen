package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

/**
 * Serialises a html table cell, i.e. a td or th
 */
public class CellComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new CellComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private CellComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    PdfPCell lCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lCell, lCellParagraph));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();

    pSerialiser.add(lCell);
  }
}
