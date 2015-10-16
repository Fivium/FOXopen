package net.foxopen.fox.module.serialiser.components.pdf;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedContainerPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Serialises a container
 */
public class ContainerComponentBuilder
extends ComponentBuilder<PDFSerialiser, EvaluatedContainerPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedContainerPresentationNode> INSTANCE = new ContainerComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedContainerPresentationNode> getInstance() {
    return INSTANCE;
  }

  private ContainerComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedContainerPresentationNode pEvalContainerNode) {
    processChildren(pSerialisationContext, pSerialiser, pEvalContainerNode);
  }
}
