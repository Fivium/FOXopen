package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import com.itextpdf.text.Phrase;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

/**
 * Serialises a sup or sub tag with a specified text rise ems
 */
public class SupSubComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private final float mTextRiseEms;

  /**
   * Constructs a sub or sup tag component builder with the specified text rise ems
   * @param pTextRiseEms The ems relative to the tag font size to rise or lower the text
   */
  public SupSubComponentBuilder(float pTextRiseEms) {
    mTextRiseEms = pTextRiseEms;
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    Phrase lPhrase = pSerialiser.getElementFactory().getPhrase();

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lPhrase));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();

    // Add the text rise for this sup/sub tag to each chunk that was added to the phrase. It is additive to any existing
    // text rise on the phrase chunks, as this allows nested sub/sup tags to be handled.
    float lAdditionalTextRise = pSerialiser.getElementAttributes().getFontAttributes().getSize() * mTextRiseEms;
    lPhrase.getChunks().forEach(pChunk -> pChunk.setTextRise(pChunk.getTextRise() + lAdditionalTextRise));

    pSerialiser.add(lPhrase);
  }
}
