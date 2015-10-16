package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import com.itextpdf.text.Chunk;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Serialises a br tag
 */
public class BreakComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new BreakComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private BreakComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    pSerialiser.add(Chunk.NEWLINE);
  }
}
