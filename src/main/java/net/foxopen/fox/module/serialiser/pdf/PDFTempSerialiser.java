package net.foxopen.fox.module.serialiser.pdf;

import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.TempSerialiser;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.module.serialiser.components.pdf.TempHTMLComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.DefaultElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.VoidContainer;
import net.foxopen.fox.module.serialiser.pdf.pages.BodyAttributes;
import net.foxopen.fox.module.serialiser.pdf.pages.HeaderFooterAttributes;
import net.foxopen.fox.module.serialiser.pdf.pages.PageAttributes;
import net.foxopen.fox.module.serialiser.pdf.pages.PageTemplate;

/**
 * Temporary PDFSerialiser serialiser
 */
public class PDFTempSerialiser extends PDFSerialiser implements TempSerialiser<PDFTempSerialiserOutput> {
  private final static PageTemplate TEMP_PAGE_TEMPLATE = getTempPageTemplate();
  private final PDFTempSerialiserOutput mOutput = new PDFTempSerialiserOutput();

  public PDFTempSerialiser(EvaluatedParseTree pEvalParseTree, boolean pIsDebug) {
    super(pEvalParseTree, pIsDebug);

    startPageTemplate(TEMP_PAGE_TEMPLATE);
    pushElementAttributes(DefaultElementAttributes.getDefaultAttributes());
    // Start a void element container so serialisation will not actually add the elements to anything
    startContainer(new VoidContainer());
  }

  @Override
  public ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> getComponentBuilder(ComponentBuilderType pComponentBuilderType) {
    ComponentBuilder lComponentBuilder;

    // The temporary serialiser is used to determine which html tags have been encountered (used to set the number of
    // columns in a table - the number of cells in the first row), so detour any html tag component builder instances
    // via the temp html component builder - this will add the name of the html tag to the temporary output
    if (pComponentBuilderType == ComponentBuilderType.HTML_TAG) {
      lComponentBuilder = new TempHTMLComponentBuilder(mOutput);
    }
    else {
      lComponentBuilder = super.getComponentBuilder(pComponentBuilderType);
    }

    return lComponentBuilder;
  }

  @Override
  public PDFTempSerialiserOutput getOutput() {
    return mOutput;
  }

  /**
   * Returns the page template to be used as the root during temporary serialisation. The temp serialiser does not
   * actually output a document, however at least one page template is required by events that can occur during
   * serialisation (e.g. set header/footer, adding a new template).
   * @return A dummy page template for use as the temp serialiser root page template
   */
  private static PageTemplate getTempPageTemplate() {
    HeaderFooterAttributes lHeaderFooterAttributes = new HeaderFooterAttributes(0f);
    BodyAttributes lBodyAttributes = new BodyAttributes(0f, 0f, 0f, 0f);
    PageAttributes lPageAttributes = new PageAttributes(0f, 0f, 0f, 0f, 0f, 0f, lBodyAttributes, lHeaderFooterAttributes, lHeaderFooterAttributes);

    return new PageTemplate(lPageAttributes);
  }
}
