package net.foxopen.fox.module.serialiser.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.FontSelector;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfWriter;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.OutputHint;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.fieldset.action.InternalActionContext;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.module.serialiser.components.pdf.BufferComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.ContainerComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.CurrentPageNumberComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.ExprOutComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.FooterComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.GridCellComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.GridComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.GridRowComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.HTMLComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.HeaderComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.HeadingComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.IgnoreUnimplementedComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.InfoBoxComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.LastPageNumberComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.SetOutComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.TextComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.UnimplementedComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.css.CSSFileManager;
import net.foxopen.fox.module.serialiser.pdf.css.CSSResolver;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.DefaultElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributeManager;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainer;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerManager;
import net.foxopen.fox.module.serialiser.pdf.elementfactory.ElementFactory;
import net.foxopen.fox.module.serialiser.pdf.font.FontManager;
import net.foxopen.fox.module.serialiser.pdf.pages.HeaderFooterContent;
import net.foxopen.fox.module.serialiser.pdf.pages.HeaderFooterPostProcessingOperation;
import net.foxopen.fox.module.serialiser.pdf.pages.PageManager;
import net.foxopen.fox.module.serialiser.pdf.pages.PageTemplate;
import net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.PageNumberPlaceholder;
import net.foxopen.fox.module.serialiser.pdf.postprocessing.PostProcessingManager;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.pdf.CellmateWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.DateWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.FormWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.IgnoreUnimplementedWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.InputWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.LinkWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.ListWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.PhantomBufferWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.RadioWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.SelectorWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.StaticTextWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.TextWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.TickboxWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.pdf.UnimplementedWidgetBuilder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serialises PDF output of an evaluated parse tree
 */
public class PDFSerialiser implements OutputSerialiser {
  /**
   * The widget builders that should be used to serialise the specified widget builder types
   */
  static private final Map<WidgetBuilderType, WidgetBuilder<PDFSerialiser, ? extends EvaluatedNode>> PDF_WIDGET_MAP = new EnumMap<>(WidgetBuilderType.class);
  /**
   * The component builders that should be used to serialise the specified component builder types
   */
  static private final Map<ComponentBuilderType, ComponentBuilder<PDFSerialiser, ? extends EvaluatedPresentationNode>> PDF_COMPONENT_MAP = new EnumMap<>(ComponentBuilderType.class);

  static {
    PDF_WIDGET_MAP.put(WidgetBuilderType.FORM, FormWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.LIST, ListWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.INPUT, InputWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.TEXT, TextWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.TICKBOX, TickboxWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.RADIO, RadioWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.SELECTOR, SelectorWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.SEARCH_SELECTOR, SelectorWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.PHANTOM_BUFFER, PhantomBufferWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.STATIC_TEXT, StaticTextWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.DATE, DateWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.DATE_TIME, DateWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.CELLMATES, CellmateWidgetBuilder.getInstance());
    PDF_WIDGET_MAP.put(WidgetBuilderType.LINK, LinkWidgetBuilder.getInstance());

    PDF_COMPONENT_MAP.put(ComponentBuilderType.BUFFER, BufferComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.TEXT, TextComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.HEADING, HeadingComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.SET_OUT, SetOutComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.HEADING, HeadingComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.HTML_TAG, HTMLComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.NODE_CONTAINER, ContainerComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.INFO_BOX, InfoBoxComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.EXPR_OUT, ExprOutComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.GRID, GridComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.GRID_ROW, GridRowComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.GRID_CELL, GridCellComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.HEADER, HeaderComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.FOOTER, FooterComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.CURRENT_PAGE_NUMBER, CurrentPageNumberComponentBuilder.getInstance());
    PDF_COMPONENT_MAP.put(ComponentBuilderType.LAST_PAGE_NUMBER, LastPageNumberComponentBuilder.getInstance());
  }

  private final EvaluatedParseTree mEvalParseTree;
  private final boolean mIsDebug;
  private final boolean mIsIgnoreUnsupported;
  private final Document mDocument = new Document();
  private final PageManager mPageManager = new PageManager(mDocument);
  private final FontManager mFontManager = new FontManager();
  private final ElementAttributeManager mElementAttributeManager = new ElementAttributeManager();
  private final ElementContainerManager mElementContainerManager = new ElementContainerManager();
  private final CSSResolver mCSSResolver;
  private final List<SerialisationObserver> mSerialisationObservers = new LinkedList<>();
  private final WidgetBuilder<PDFSerialiser, ? extends EvaluatedNode> mUnimplementedWidgetBuilder;
  private final ComponentBuilder<PDFSerialiser, ? extends EvaluatedPresentationNode> mUnimplementedComponentBuilder;

  /**
   * Create a serialiser for the provided evaluated parse tree
   * @param pEvalParseTree The parse tree to process during serialisation
   * @param pIsDebug Whether or not the serialiser should serialise PDFs in debug mode
   * @param pIsIgnoreUnsupported If true unsupported components and widgets are ignored, else an exception is thrown
   *                            when they are encountered
   */
  public PDFSerialiser(EvaluatedParseTree pEvalParseTree, boolean pIsDebug, boolean pIsIgnoreUnsupported) {
    mEvalParseTree = pEvalParseTree;
    mIsDebug = pIsDebug;
    mIsIgnoreUnsupported = pIsIgnoreUnsupported;

    if (mIsIgnoreUnsupported) {
      mUnimplementedWidgetBuilder = IgnoreUnimplementedWidgetBuilder.getInstance();
      mUnimplementedComponentBuilder = IgnoreUnimplementedComponentBuilder.getInstance();
    }
    else {
      mUnimplementedWidgetBuilder = UnimplementedWidgetBuilder.getInstance();
      mUnimplementedComponentBuilder = UnimplementedComponentBuilder.getInstance();
    }

    // Create a CSS resolver after loading css files from the module css-list
    CSSFileManager lCSSFileManager = new CSSFileManager(mEvalParseTree, mEvalParseTree.getModule().getStyleSheets());
    mCSSResolver = new CSSResolver(lCSSFileManager.getCSSFiles(), mIsDebug);

    // Add the page manager observer, so that new pages can be added when content is serialised if a new page is pending
    addObserver(mPageManager.getSerialisationObserver());
  }

  public void addObserver(SerialisationObserver pSerialisationObserver) {
    mSerialisationObservers.add(pSerialisationObserver);
  }

  /**
   * Serialises the evaluated parse tree as a PDF to the output stream
   * @param pOutputStream The output stream for the serialised PDF
   */
  public void serialise(OutputStream pOutputStream) {
    // TODO WH: Is there actually a possibility this could be null at runtime, or should this be an assertion instead? (copied from HTML serialiser)
    if (pOutputStream == null) {
      throw new ExInternal("Output stream must not be null");
    }

    // PDF serialisation is done in two passes, the first pass bytes are streamed first, the second pass will then
    // stream the final document to pOutputStream
    ByteArrayOutputStream lFirstPassOutput = new ByteArrayOutputStream();
    // Opening a writer with the first pass output stream will allow the document to be opened and written to the stream
    PdfWriter lWriter = openWriter(mDocument, lFirstPassOutput);
    // Page manager will handle document page events (i.e. page end)
    lWriter.setPageEvent(mPageManager);

    // Start with the default element attributes, start a document container and render from the root buffer of the
    // evaluated parse tree
    pushElementAttributes(DefaultElementAttributes.getDefaultAttributes());
    startContainer(ElementContainerFactory.getContainer(mDocument));
    mEvalParseTree.getRootBuffer().render(mEvalParseTree, this);
    endContainer();
    popElementAttributes();

    // Post conditions to ensure that there are no lingering element attributes, containers, or page templates open
    if (mElementAttributeManager.hasAttributes()) {
      throw new ExInternal("Document processing has finished but not all element attributes have been popped");
    }

    if (mElementContainerManager.hasContainer()) {
      throw new ExInternal("Document processing has finished but not all containers have been ended");
    }

    if (mPageManager.hasPageTemplate()) {
      throw new ExInternal("Document processing has finished but not all page templates have been ended");
    }

    // TODO WH: handle document with nothing added/no pages (otherwise exception is thrown) - or just allow exception
    // Close the document - this completes the first pass output
    mDocument.close();

    // Apply second pass processing, currently the only post processing required is headers/footers
    PostProcessingManager lPostProcessingManager = new PostProcessingManager();
    lPostProcessingManager.addPostProcessingOperation(new HeaderFooterPostProcessingOperation(mPageManager));
    lPostProcessingManager.process(pOutputStream, lFirstPassOutput);
  }

  @Override
  public WidgetBuilder<PDFSerialiser, EvaluatedNode> getWidgetBuilder(WidgetBuilderType pWidgetBuilderType) {
    return Optional.<WidgetBuilder>ofNullable(PDF_WIDGET_MAP.get(pWidgetBuilderType))
                   .orElse(mUnimplementedWidgetBuilder);
  }

  @Override
  public ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> getComponentBuilder(ComponentBuilderType pComponentBuilderType) {
    return Optional.<ComponentBuilder>ofNullable(PDF_COMPONENT_MAP.get(pComponentBuilderType))
                   .orElse(mUnimplementedComponentBuilder);
  }

  @Override
  public PDFTempSerialiser getTempSerialiser() {
    return new PDFTempSerialiser(mEvalParseTree, mIsDebug, mIsIgnoreUnsupported);
  }

  @Override
  public void addHint(SerialisationContext pSerialisationContext, OutputHint pHint, String pTargetID, boolean pAddIcon) {
    throw new ExInternal("Cannot add hints during PDF serialisation", new UnsupportedOperationException());
  }

  @Override
  public void addHint(SerialisationContext pSerialisationContext, OutputHint pHint) {
    throw new ExInternal("Cannot add hints during PDF serialisation", new UnsupportedOperationException());
  }

  @Override
  public void addDescription(SerialisationContext pSerialisationContext, EvaluatedNode pEvaluatedNode) {
    throw new ExInternal("Cannot add descriptions during PDF serialisation", new UnsupportedOperationException());
  }

  @Override
  public String getInternalActionSubmitString(InternalActionContext pActionContext) {
    throw new ExInternal("Action submit string is not applicable during PDF serialisation", new UnsupportedOperationException());
  }

  @Override
  public String getInternalActionSubmitString(InternalActionContext pActionContext, Map<String, String> pParamMap) {
    throw new ExInternal("Action submit string is not applicable during PDF serialisation", new UnsupportedOperationException());
  }

  @Override
  public String getSafeStringAttribute(StringAttributeResult pStringAttributeResult) {
    if (pStringAttributeResult == null) {
      return null;
    }

    // HTML entity escaping is not required during PDF serialisation
    return pStringAttributeResult.getString();
  }

  @Override
  public void addDebugInformation(String pDebugInformation) {
    // There is no way to add hidden debug information to a PDF as there is during HTML serialisation (i.e. hidden div)
    throw new ExInternal("Cannot add debug information during PDF serialisation", new UnsupportedOperationException());
  }

  @Override
  public String escapeNewlines(String pString) {
    throw new ExInternal("Newline escaping is not applicable during PDF serialisation", new UnsupportedOperationException());
  }

  /**
   * Returns the CSS resolver to resolve CSS styles to their associated element attributes
   * @return The CSS resolver associated with this serialiser
   */
  public CSSResolver getCSSResolver() {
    return mCSSResolver;
  }

  /**
   * Starts a page template. A new page is started with the new attributes when content is added to the document. Added
   * page templates must be ended via {@link #endPageTemplate} once they are out of scope (i.e. further pages should no
   * longer be created using this template).
   * @param pPageTemplate The page template to be started
   */
  public void startPageTemplate(PageTemplate pPageTemplate) {
    mPageManager.startPageTemplate(pPageTemplate);
  }

  /**
   * Ends the current page template. The current page is ended (content added to the document after end will be added to
   * a new page created using whatever page template is previous to the ended template).
   */
  public void endPageTemplate() {
    mPageManager.endPageTemplate();
  }

  /**
   * Sets the page header content for the current page template. Page header content may only be set once per page
   * template.
   * @param pPageHeaderContent The header content
   */
  public void setPageHeaderContent(HeaderFooterContent pPageHeaderContent) {
    mPageManager.setPageHeaderContent(pPageHeaderContent);
  }

  /**
   * Sets the page footer content for the current page template. Page footer content may only be set once per page
   * template.
   * @param pPageFooterContent The footer content
   */
  public void setPageFooterContent(HeaderFooterContent pPageFooterContent) {
    mPageManager.setPageFooterContent(pPageFooterContent);
  }

  /**
   * Adds a page number placeholder which will be resolved when the current page template headers and footers are
   * processed during post processing (i.e. when the first pass document serialisation is completed)
   * @param pPageNumberPlaceholder The page number placeholder that will be resolved during post processing
   */
  public void addPageNumberPlaceholder(PageNumberPlaceholder pPageNumberPlaceholder) {
    mPageManager.addPageNumberPlaceholder(pPageNumberPlaceholder);
  }

  /**
   * Sets the element attributes that should be used when creating elements, until the element attributes are popped off
   * the stack using {@link #popElementAttributes}
   * @param pElementAttributes
   */
  public void pushElementAttributes(ElementAttributes pElementAttributes) {
    mElementAttributeManager.pushAttributes(pElementAttributes);
  }

  /**
   * Pops the current element attributes off the element attribute stack
   */
  public void popElementAttributes() {
    mElementAttributeManager.popAttributes();
  }

  /**
   * Returns a set of element attributes created by inheriting the current element attributes. Element attributes
   * should be inherited to allow CSS-like behaviour, where only certain properties such as font will be inherited by
   * children.
   * @return a set of element attributes created by inheriting the current element attributes
   */
  public ElementAttributes getInheritedElementAttributes() {
    return ElementAttributes.inheritFrom(mElementAttributeManager.getCurrentAttributes());
  }

  /**
   * Returns a set of element attributes created from the current element attributes. Note that
   * {@link #getInheritedElementAttributes} should be used unless the current element attributes have already been
   * inherited from their parent.
   * @return A set of element attributes created from the current element attributes
   */
  public ElementAttributes getElementAttributes() {
    return new ElementAttributes(mElementAttributeManager.getCurrentAttributes());
  }

  /**
   * Returns an element factory to create elements with the current element attributes applied. Elements should always
   * be created using the element factory if attributes can be applied to them.
   * @return an element factory to create elements with the current element attributes applied
   */
  public ElementFactory getElementFactory() {
    return new ElementFactory(getElementAttributes());
  }

  /**
   * Starts an element container. Elements serialised after the container has started will be added via the container.
   * @param pElementContainer The element container
   */
  public void startContainer(ElementContainer pElementContainer) {
    mElementContainerManager.startContainer(pElementContainer);
  }

  /**
   * Ends the current element container
   */
  public void endContainer() {
    mElementContainerManager.endContainer();
  }

  /**
   * Add an element to the current element container. Do not use this method when adding a cell, use
   * {@link #add(PdfPCell)} instead so it may be correctly added if the current container is a table container.
   * @param pElement The element to add to the current element container
   */
  public void add(Element pElement) {
    mSerialisationObservers.forEach(pObserver -> pObserver.beforeAddElement(pElement));
    mElementContainerManager.getCurrentContainer().addChildElement(pElement);
  }

  /**
   * Adds a cell to the current element container. This method must be used as opposed to {@link #add(Element)} to allow
   * the cell to be correctly added to a table container.
   * @param pCell The cell to add to the current element container
   */
  public void add(PdfPCell pCell) {
    mSerialisationObservers.forEach(pObserver -> pObserver.beforeAddCell(pCell));
    mElementContainerManager.getCurrentContainer().addChildCell(pCell);
  }

  /**
   * Adds the provided text as a {@link Phrase} after processing through the current font selector
   * @param pText The text to be processed and added
   */
  public void addText(String pText) {
    add(processText(pText));
  }

  /**
   * Adds the provided text as a {@link Paragraph} after processing through the current font selector. This is a
   * convienence method to avoid having to start and end a paragraph container when adding text that should be within
   * its own paragraph.
   * @param pText The text to be processed and added as a paragraph
   */
  public void addParagraphText(String pText) {
    Paragraph lParagraph = getElementFactory().getParagraph();
    lParagraph.add(processText(pText));
    add(lParagraph);
  }

  /**
   * Adds a table spacer. Table spacers must be added before a table is added to the document content to ensure that
   * the content following the table will behave correctly - if a table spacer is not added the table will overlap the
   * content. Table spacers are not required when adding a table to a cell as a nested table, as there is no
   * following/preceding content within the cell.
   */
  public void addTableSpacer() {
    add(new Paragraph());
  }

  /**
   * Returns a font selector with the current font attributes applied. A font selector is used to set a font that can
   * render the text on a per-character basis.
   * @return A font selector with the current font attributes applied.
   */
  public FontSelector getFontSelector() {
    return mFontManager.getFontSelector(getElementAttributes().getFontAttributes());
  }

  /**
   * Returns a PDF writer for the document to be written to the output stream
   * @param pDocument The document that is going to be written
   * @param pOutputStream The PDF output stream
   * @return A PDF writer for the document to be written to the output stream
   * @throws ExInternal If the writer failed to open
   */
  private PdfWriter openWriter(Document pDocument, OutputStream pOutputStream) throws ExInternal {
    try {
      return PdfWriter.getInstance(pDocument, pOutputStream);
    }
    catch (DocumentException e) {
      throw new ExInternal("Failed to open PDF writer", e);
    }
  }

  /**
   * Processes the provided text using the current font selector into a {@link Phrase}. Text processing will apply a
   * font that can render the text on a per-character basis.
   * @param pText The text to be processed
   * @return A {@link Phrase} with the processed text
   */
  private Phrase processText(String pText) {
    return getFontSelector().process(pText);
  }
}
