package net.foxopen.fox.module.serialiser.components.pdf;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedCurrentPageNumberPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.CurrentPageNumberPlaceholder;

/**
 * Serialises the current page number. This is only valid when the node has a header or footer as an ancestor.
 */
public class CurrentPageNumberComponentBuilder {
  private static final PageNumberPlaceholderSupplier PAGE_NUMBER_PLACEHOLDER_SUPPLIER =
    (pPhrase, pFontSelector) -> new CurrentPageNumberPlaceholder(pPhrase, pFontSelector);
  private static final ComponentBuilder<PDFSerialiser, EvaluatedCurrentPageNumberPresentationNode> INSTANCE = new PageNumberComponent<>(PAGE_NUMBER_PLACEHOLDER_SUPPLIER);

  public static final ComponentBuilder<PDFSerialiser, EvaluatedCurrentPageNumberPresentationNode> getInstance() {
    return INSTANCE;
  }

  private CurrentPageNumberComponentBuilder() {
  }
}

