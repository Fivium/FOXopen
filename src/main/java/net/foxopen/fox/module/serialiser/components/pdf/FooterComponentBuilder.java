package net.foxopen.fox.module.serialiser.components.pdf;

import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedFooterPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Serialises a footer
 */
public class FooterComponentBuilder {
  private static final String FOOTER_TAG = HTML.Tag.FOOTER;
  /**
   * Create a header footer component that uses the footer html tag and consumes the footer content by setting the page
   * footer content in the serialiser
   */
  private static final ComponentBuilder<PDFSerialiser, EvaluatedFooterPresentationNode> INSTANCE = new HeaderFooterComponent<>(FOOTER_TAG, PDFSerialiser::setPageFooterContent);

  public static final ComponentBuilder<PDFSerialiser, EvaluatedFooterPresentationNode> getInstance() {
    return INSTANCE;
  }
}
