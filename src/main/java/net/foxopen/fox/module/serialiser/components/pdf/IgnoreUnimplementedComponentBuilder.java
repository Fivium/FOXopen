package net.foxopen.fox.module.serialiser.components.pdf;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.track.Track;

/**
 * Ignores unimplemented components
 */
public class IgnoreUnimplementedComponentBuilder
extends ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> INSTANCE = new IgnoreUnimplementedComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private IgnoreUnimplementedComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    Track.alert("IgnoreUnsupported", "Ignoring unsupported component: " + pEvalNode.toString()
                + ", type: " + pEvalNode.getPageComponentType().toString());
  }
}
