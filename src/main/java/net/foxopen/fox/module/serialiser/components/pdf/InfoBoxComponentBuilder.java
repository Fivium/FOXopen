package net.foxopen.fox.module.serialiser.components.pdf;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedInfoBoxPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class InfoBoxComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedInfoBoxPresentationNode> {
  private static final String INFO_BOX_TAG = "div";
  /**
   * The class that will be added by default to every info box
   */
  private static final String INFO_BOX_CLASS = "infoBox";
  /**
   * The string prefixed to the info box type to add a default class for that type.
   * @see EvaluatedInfoBoxPresentationNode#getInfoBoxType
   */
  private static final String INFO_BOX_TYPE_CLASS_PREFIX = "infoBox-";
  /**
   * The string to be prefixed to the info box title level to create the html tag for the title, e.g. h1, h2 etc.
   */
  private static final String INFO_BOX_TITLE_TAG_PREFIX = "h";
  private static final ComponentBuilder<PDFSerialiser, EvaluatedInfoBoxPresentationNode> INSTANCE = new InfoBoxComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedInfoBoxPresentationNode> getInstance() {
    return INSTANCE;
  }

  private InfoBoxComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedInfoBoxPresentationNode pEvalNode) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    List<String> lClasses = Arrays.asList(INFO_BOX_CLASS, INFO_BOX_TYPE_CLASS_PREFIX + pEvalNode.getInfoBoxType().toString().toLowerCase(), pEvalNode.getClasses());
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, INFO_BOX_TAG, lClasses, Collections.singletonList(pEvalNode.getStyles()));
    pSerialiser.pushElementAttributes(lElementAttributes);

    PdfPTable lInfoBoxTable = pSerialiser.getElementFactory().getTable(1);
    PdfPCell lInfoBoxCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lInfoBoxCell, lCellParagraph));

    // Add a title if one exists
    if (pEvalNode.getEvaluatedTitleContainer() != null) {
      addTitle(pSerialisationContext, pSerialiser, pEvalNode);
    }

    // Process info box content
    processChildren(pSerialisationContext, pSerialiser, pEvalNode.getEvaluatedContentContainer());

    pSerialiser.endContainer();
    lInfoBoxTable.addCell(lInfoBoxCell);
    // A table spacer is required so any content before the info box is not overlapped
    pSerialiser.addTableSpacer();
    pSerialiser.add(lInfoBoxTable);

    pSerialiser.popElementAttributes();
  }

  private void addTitle(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedInfoBoxPresentationNode pEvalNode) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, INFO_BOX_TITLE_TAG_PREFIX + pEvalNode.getTitleLevel(),
                                               Collections.singletonList(pEvalNode.getClasses()),
                                               Collections.singletonList(pEvalNode.getStyles()));
    pSerialiser.pushElementAttributes(lElementAttributes);

    Paragraph lTitle = pSerialiser.getElementFactory().getParagraph();
    pSerialiser.startContainer(ElementContainerFactory.getContainer(lTitle));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode.getEvaluatedTitleContainer());
    pSerialiser.endContainer();
    pSerialiser.add(lTitle);

    pSerialiser.popElementAttributes();
  }
}
