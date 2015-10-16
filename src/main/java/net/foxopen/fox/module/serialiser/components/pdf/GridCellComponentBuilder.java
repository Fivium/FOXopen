package net.foxopen.fox.module.serialiser.components.pdf;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedGridCellPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

import java.util.Arrays;
import java.util.Collections;

/**
 * Serialises a grid cell
 */
public class GridCellComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedGridCellPresentationNode> {
  private static final String GRID_CELL_TAG = HTML.Tag.TD;
  /**
   * The class applied by default to every grid cell
   */
  private static final String GRID_CELL_CLASS = "gridCell";
  private static final ComponentBuilder<PDFSerialiser, EvaluatedGridCellPresentationNode> INSTANCE = new GridCellComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedGridCellPresentationNode> getInstance() {
    return INSTANCE;
  }

  private GridCellComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedGridCellPresentationNode pEvalNode) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, GRID_CELL_TAG,
                                               Arrays.asList(GRID_CELL_CLASS, pEvalNode.getClasses()),
                                               Collections.singletonList(pEvalNode.getStyles()));
    pSerialiser.pushElementAttributes(lElementAttributes);

    PdfPCell lCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();
    lCell.setColspan(pEvalNode.getColumnSpan());

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lCell, lCellParagraph));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();
    pSerialiser.add(lCell);

    pSerialiser.popElementAttributes();
  }
}
