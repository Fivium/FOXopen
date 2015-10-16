package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Serialises a ul tag
 */
public class UnorderedListComponentBuilder {
  private static final ListComponentBuilder.ListType LIST_TYPE = ListComponentBuilder.ListType.UNORDERED;
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new ListComponentBuilder(LIST_TYPE);

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private UnorderedListComponentBuilder() {
  }
}
