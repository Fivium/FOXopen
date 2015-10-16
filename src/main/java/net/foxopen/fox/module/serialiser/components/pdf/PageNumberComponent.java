package net.foxopen.fox.module.serialiser.components.pdf;

import com.itextpdf.text.Phrase;
import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHeaderFooterPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPageNumberPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elements.ElementWrapper;
import net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.PageNumberPlaceholder;

import java.util.Collections;

/**
 * Serialises a page number. This is only valid when the node has a header or footer as an ancestor.
 */
public class PageNumberComponent<EPN extends EvaluatedPageNumberPresentationNode> extends ComponentBuilder<PDFSerialiser, EPN> {
  private final static String PAGE_NUMBER_TAG = HTML.Tag.SPAN;

  /**
   * A supplier to construct the page number placeholder from the given phrase and font selector
   */
  private final PageNumberPlaceholderSupplier mPageNumberPlaceholderSupplier;

  /**
   * Creates a component that will serialise a page number placeholder
   * @param pPageNumberPlaceholderSupplier A supplier that constructs a page number placeholder from the given phrase
   *                                       and font selector
   */
  protected PageNumberComponent(PageNumberPlaceholderSupplier pPageNumberPlaceholderSupplier) {
    mPageNumberPlaceholderSupplier = pPageNumberPlaceholderSupplier;
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EPN pEvalNode) {
    if (pEvalNode.getClosestAncestor(EvaluatedHeaderFooterPresentationNode.class) == null) {
      throw new ExInternal("Page number output is only valid within a header or footer");
    }

    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, PAGE_NUMBER_TAG,
                                               Collections.singletonList(pEvalNode.getClasses()),
                                               Collections.singletonList(pEvalNode.getStyles()));
    pSerialiser.pushElementAttributes(lElementAttributes);

    // Add a placeholder for the page number. The page number will be added as text to the phrase during header/footer
    // rendering. The current font selector is provided so that the page number text may be processed with all the
    // current font attributes later during rendering.
    Phrase lPageNumberPhrase = pSerialiser.getElementFactory().getPhrase();
    PageNumberPlaceholder lPageNumberPlaceholder = mPageNumberPlaceholderSupplier.getPlaceholder(lPageNumberPhrase,
                                                                                                 pSerialiser.getFontSelector());
    pSerialiser.addPageNumberPlaceholder(lPageNumberPlaceholder);

    // The element itself is wrapped so that iText will not attempt to extract the chunks from the phrase and discard
    // the phrase itself - the element wrapper tells iText the type of the element is not one with chunks
    pSerialiser.add(new ElementWrapper(lPageNumberPhrase));
    pSerialiser.popElementAttributes();
  }
}
