package net.foxopen.fox.module.serialiser.components.pdf;

import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHeaderPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Serialises a header
 */
public class HeaderComponentBuilder {
  public static final String HEADER_TAG = HTML.Tag.HEADER;
  /**
   * Create a header footer component that uses the header html tag and consumes the header content by setting the page
   * header content in the serialiser
   */
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHeaderPresentationNode> INSTANCE = new HeaderFooterComponent<>(HEADER_TAG, PDFSerialiser::setPageHeaderContent);

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHeaderPresentationNode> getInstance() {
    return INSTANCE;
  }
}
