package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import com.itextpdf.text.Phrase;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

/**
 * Serialises a span tag, which can also be used generically for other inline-layout tags such as strong, em etc.
 */
public class SpanComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new SpanComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private SpanComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    Phrase lPhrase = pSerialiser.getElementFactory().getPhrase();

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lPhrase));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();

    pSerialiser.add(lPhrase);
  }
}
