package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Serialises a sup tag
 */
public class SupComponentBuilder {
  private static final float TEXT_RISE_EMS = 0.5f;
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new SupSubComponentBuilder(TEXT_RISE_EMS);

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }
}
