package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.draw.LineSeparator;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Serialises a hr tag
 */
public class HorizontalRuleComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new HorizontalRuleComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private HorizontalRuleComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    Paragraph lParagraph = pSerialiser.getElementFactory().getParagraph();
    LineSeparator lLineSeparator = pSerialiser.getElementFactory().getLineSeparator();

    lParagraph.add(new Chunk(lLineSeparator));
    pSerialiser.add(lParagraph);
  }
}
