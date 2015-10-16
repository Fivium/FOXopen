package net.foxopen.fox.module.serialiser.components.pdf;


import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.PDFTempSerialiserOutput;

/**
 * Serialises a HTML tag, adding the name of the tag to the temporary serialiser output
 */
public class TempHTMLComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private final PDFTempSerialiserOutput mTempSerialiserOutput;

  /**
   * Create a HTML component builder that records the name of the HTML tag to the temporary serialiser output
   * @param pTempSerialiserOutput The temporary serialiser output
   */
  public TempHTMLComponentBuilder(PDFTempSerialiserOutput pTempSerialiserOutput) {
    mTempSerialiserOutput = pTempSerialiserOutput;
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    // Add the name of the tag to the temporary output, and forward the component building to the actual HTML component
    // builder
    mTempSerialiserOutput.addSerialisedHTMLTag(pEvalNode.getTagName());
    HTMLComponentBuilder.getInstance().buildComponent(pSerialisationContext, pSerialiser, pEvalNode);
  }
}
