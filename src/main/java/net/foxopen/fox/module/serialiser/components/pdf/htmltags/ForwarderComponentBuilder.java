package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Does not serialise anything but continues processing node children. This is used for tags that do not require any
 * serialisation as they do not have a corresponding pdf element, such as the tr tag, which forwards processing to its
 * child cells.
 */
public class ForwarderComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new ForwarderComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private ForwarderComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
  }
}
