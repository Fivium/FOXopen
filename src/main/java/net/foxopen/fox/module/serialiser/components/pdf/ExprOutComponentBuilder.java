package net.foxopen.fox.module.serialiser.components.pdf;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Serialises an expr out
 */
public class ExprOutComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> INSTANCE = new ExprOutComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private ExprOutComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    String lText = pEvalNode.getText();

    if (lText != null) {
      pSerialiser.addText(lText);
    }
  }
}
