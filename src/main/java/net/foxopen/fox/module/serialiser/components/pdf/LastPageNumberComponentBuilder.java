package net.foxopen.fox.module.serialiser.components.pdf;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedLastPageNumberPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.LastPageNumberPlaceholder;

/**
 * Serialises the last page number. This is only valid when the node has a header or footer as an ancestor.
 */
public class LastPageNumberComponentBuilder {
  private static final PageNumberPlaceholderSupplier PAGE_NUMBER_PLACEHOLDER_SUPPLIER =
    (pPhrase, pFontSelector) -> new LastPageNumberPlaceholder(pPhrase, pFontSelector);
  private static final ComponentBuilder<PDFSerialiser, EvaluatedLastPageNumberPresentationNode> INSTANCE = new PageNumberComponent<>(PAGE_NUMBER_PLACEHOLDER_SUPPLIER);

  public static final ComponentBuilder<PDFSerialiser, EvaluatedLastPageNumberPresentationNode> getInstance() {
    return INSTANCE;
  }

  private LastPageNumberComponentBuilder() {
  }
}
