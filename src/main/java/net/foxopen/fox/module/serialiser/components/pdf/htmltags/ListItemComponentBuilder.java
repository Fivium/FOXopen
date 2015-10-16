package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import com.itextpdf.text.ListItem;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

/**
 * Serialises a li tag
 */
public class ListItemComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new ListItemComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private ListItemComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    ListItem lListItem = pSerialiser.getElementFactory().getListItem();

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lListItem));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();

    pSerialiser.add(lListItem);
  }
}
