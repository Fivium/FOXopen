package net.foxopen.fox.module.serialiser.components.pdf;

import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedGridPresentationNode;
import net.foxopen.fox.module.serialiser.FOXGridUtils;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

import java.util.Collections;

/**
 * Serialises a grid
 */
public class GridComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedGridPresentationNode> {
  private static final String GRID_TAG = HTML.Tag.TABLE;
  private static final ComponentBuilder<PDFSerialiser, EvaluatedGridPresentationNode> INSTANCE = new GridComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedGridPresentationNode> getInstance() {
    return INSTANCE;
  }

  private GridComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedGridPresentationNode pEvalNode) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, GRID_TAG,
                                               Collections.singletonList(pEvalNode.getClasses()),
                                               Collections.singletonList(pEvalNode.getStyles()));
    pSerialiser.pushElementAttributes(lElementAttributes);

    PdfPTable lTable = pSerialiser.getElementFactory().getTable(FOXGridUtils.getMaxColumns());
    pSerialiser.startContainer(ElementContainerFactory.getContainer(lTable));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();
    pSerialiser.add(lTable);

    pSerialiser.popElementAttributes();
  }
}
