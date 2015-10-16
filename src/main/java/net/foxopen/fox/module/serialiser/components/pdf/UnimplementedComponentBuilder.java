package net.foxopen.fox.module.serialiser.components.pdf;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Handles unimplemented components
 */
public class UnimplementedComponentBuilder
extends ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> INSTANCE = new UnimplementedComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private UnimplementedComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    throw new ExInternal("FOX5 PDF generation does not support unimplemented component " + pEvalNode.toString()
                         + ", type: " + pEvalNode.getPageComponentType().toString());
  }
}
