package net.foxopen.fox.module.serialiser.components.pdf;

import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedGridRowPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

import java.util.Collections;

/**
 * Serialises a grid row
 */
public class GridRowComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedGridRowPresentationNode> {
  private static final String GRID_ROW_TAG = HTML.Tag.TR;
  private static final ComponentBuilder<PDFSerialiser, EvaluatedGridRowPresentationNode> INSTANCE = new GridRowComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedGridRowPresentationNode> getInstance() {
    return INSTANCE;
  }

  private GridRowComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedGridRowPresentationNode pEvalNode) {
    // iText does not use explicit rows when adding cells to a table (a row is added internally when the number of cells
    // added reaches the column limit), so continue processing node children after the styles on the row have been
    // applied
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, GRID_ROW_TAG,
                                               Collections.singletonList(pEvalNode.getClasses()),
                                               Collections.singletonList(pEvalNode.getStyles()));
    pSerialiser.pushElementAttributes(lElementAttributes);
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.popElementAttributes();
  }
}
