package net.foxopen.fox.module.serialiser.pdf.pages;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.serialiser.pdf.SerialisationObserver;
import net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.PageNumberPlaceholder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * <p>Manages document page properties and creation. Page properties are set using a {@link PageTemplate}. Pages added
 * to the document are created using the properties of the last added page template (i.e. the one on the top of the
 * stack). As well as allowing page attributes (the dimensions, margins etc.) to be set, page templates store the header
 * and footer content for pages created using that page template.</p>
 *
 * <p>When a page template is ended, the current document page is ended. Any content added after is added to a new page
 * using the current page template. When a page template is started, the same occurs: The current page if it exists
 * is ended, and a new page using the current page template begins when content is added.</p>
 *
 * <p>The header and footer content of pages created using a page template can be set any point during the lifetime of
 * the page template, as the content is added to the document during post-processing. The header and footer content is
 * the same for every page created using that page template.</p>
 */
public class PageManager extends PdfPageEventHelper {
  // Unfortunately it's not possible to simply deal with all applied to page numbers in a
  // {@link PdfPageHelper#onEndPage} event, due to an issue with how iText creates a 'blank' new page. Although iText
  // will ignore a blank page and not add it, it does internally initialise a line in the document ready to be written
  // to. This line is positioned using the current document margins. This means the following situation can occur:
  //
  // 1. A new page is started when a page template is ended, line is positioned using the current margins at x.
  // 2. The document margins are updated (i.e. a due to a new page template being started), so a new page is started -
  //    the previous page is not added as it had no content (it was only used to end the current page).
  // 3. When content is added to the line, the line is still positioned at x instead of using the new margins - only
  //    when the content goes onto a new line will the new margins be used.
  //
  // iText does not provide a way to retroactively reinitialise or reposition the line to respect the current document
  // margins.
  //
  // What this means is that it must be for certain that a page will have content before creating it (and therefore
  // initialising the line at the correct position). It can't be done on page template start/end as it's not known
  // whether the related buffer will actually contain any content. The page manager uses a serialisation observer as a
  // solution - when a new page is required, a state is set saying that a new page is pending, which is then actualised
  // when any content is serialised.

  /**
   * The document pages are being added to
   */
  private final Document mDocument;
  /**
   * The stack of page templates. At least one page template must exist during serialisation.
   */
  private final Deque<PageTemplate> mPageTemplates = new ArrayDeque<>();
  /**
   * A list of header/footer renderers. The renderers will output a header or footer to a document page during
   * post-processing
   */
  private final List<HeaderFooterRenderer> mHeaderFooterRenderers = new LinkedList<>();
  /**
   * If true, a new page is added the next time content is serialised
   */
  private boolean mIsNewPagePending = true;
  /**
   * The page template that was current when content was last added to it. It is required to store this so that when
   * ending a page the page template that created that page is known (this may not be the same as the current page
   * template, as for example the page may be ending due to a new page template being added)
   */
  private Optional<PageTemplate> mLastTemplateWithContent = Optional.empty();

  /**
   * Creates a page manager for the given document
   * @param pDocument The document pages are going to be added to
   */
  public PageManager(Document pDocument) {
    mDocument = pDocument;
  }

  /**
   * Returns a serialisation observer for the page manager. The page manager uses the serialisation observer to
   * add a new page if one is pending.
   * @return A serialisation observer for the page manager
   */
  public SerialisationObserver getSerialisationObserver() {
    return pElement -> {
      if (mIsNewPagePending) {
        // Add a new page or open the document if it hasn't yet been (this starts the first page)
        if (!mDocument.isOpen()) {
          mDocument.open();
        }
        else {
          mDocument.newPage();
        }

        // Assuming a page template exists - this should be true, as no content should be serialised to the document
        // before a page template is started (as the page dimensions etc. are unknown)
        if (!hasPageTemplate()) {
          throw new ExInternal("A page template must exist to serialise content to the document");
        }

        // The current page template is the last page template with content added to it - this only needs to be updated
        // when adding a pending page instead of every time content is serialised, as when the page template changes
        // (i.e. it is ended or a new one added) a new page is always set to pending, therefore it can only change here
        mLastTemplateWithContent = Optional.of(getCurrentPageTemplate());
        mIsNewPagePending = false;
      }
    };
  }

  /**
   * Starts a new page template. The current page will be ended. When content is added to the document, a new page is
   * started using the given page template. If no content is added during the lifetime of this page template, no pages
   * are added to the document using it.
   * @param pPageTemplate The page template
   */
  public void startPageTemplate(PageTemplate pPageTemplate) {
    mPageTemplates.push(pPageTemplate);

    // Apply the new page attributes and set new page pending, so the page is added when content is serialised
    applyCurrentPageAttributes();
    mIsNewPagePending = true;
  }

  /**
   * Ends the current page template. If the current page has content it is ended. Further content is added to a new page
   * using the page template next on the stack.
   * @throws ExInternal If no page templates exist
   */
  public void endPageTemplate() throws ExInternal {
    PageTemplate lEndedPageTemplate;
    try {
      lEndedPageTemplate = mPageTemplates.pop();
    }
    catch (NoSuchElementException e) {
      throw new ExInternal("Cannot end page template as no page templates exist", e);
    }

    // If header or footer content has been set for this page template, add header/footer page renderers for the page
    // numbers that the page template which is ending has been applied to
    addHeaderFooterRenderers(lEndedPageTemplate);

    // If the ended page template was not the top-level page template, re-apply the parent page template attributes and
    // set a new page pending so it is added when further content is serialised
    if (hasPageTemplate()) {
      applyCurrentPageAttributes();
      mIsNewPagePending = true;
    }
  }

  /**
   * Returns whether or not a page template exists
   * @return True if a page template exists, false if the stack is empty
   */
  public boolean hasPageTemplate() {
    return !mPageTemplates.isEmpty();
  }

  /**
   * Sets the header content for the current page template. All pages created from that template will have the header
   * applied to them during post-processing. Header content can only be set once per page template.
   * @param pHeaderContent The header content
   * @throws ExInternal If the header content for the page template has already been set
   */
  public void setPageHeaderContent(HeaderFooterContent pHeaderContent) throws ExInternal {
    getCurrentPageTemplate().setHeaderContent(pHeaderContent);
  }

  /**
   * Sets the footer content for the current page template. All pages created from that template will have the footer
   * applied to them during post-processing. Footer content can only be set once per page template.
   * @param pFooterContent The footer content
   * @throws ExInternal If the footer content for the page template has already been set
   */
  public void setPageFooterContent(HeaderFooterContent pFooterContent) {
    getCurrentPageTemplate().setFooterContent(pFooterContent);
  }

  /**
   * Adds a placeholder for a page number. Page numbers are resolved during post-processing of the document when headers
   * and footers are resolved, as the total number of pages are known then.
   * @param pPageNumberPlaceholder
   */
  public void addPageNumberPlaceholder(PageNumberPlaceholder pPageNumberPlaceholder) {
    getCurrentPageTemplate().addPageNumberPlaceholder(pPageNumberPlaceholder);
  }

  /**
   * Renders the headers and footers added to the page manager during first-pass serialisation to the document. Headers
   * and footers are added to all pages created using the page template they belong to. Page number placeholders are
   * resolved to the correct page number during rendering.
   * @param pStamper A stamper over the first-pass document output
   */
  public void renderHeadersFooters(PdfStamper pStamper) {
    mHeaderFooterRenderers.forEach(pRenderer -> pRenderer.render(pStamper));
  }

  @Override
  public void onEndPage(PdfWriter pWriter, Document pDocument) {
    // Record the page number of the ending page as a page created from the current template, so that post-processing
    // of the template can manipulate the page if required (i.e. if adding headers and footers)
    // The page template that created the ending page is the last page template with content (this may not be the same
    // as the current page template, as for example the page may be ending due to a new page template being added)
    PageTemplate lPageTemplate = mLastTemplateWithContent.orElseThrow(
      () -> new ExInternal("A page end event has been encountered before content has been added to a page template"));
    lPageTemplate.addPageNumberAppliedTo(pWriter.getCurrentPageNumber());
  }

  /**
   * Returns the current page template
   * @return The current page template
   * @throws ExInternal If no page templates exist
   */
  private PageTemplate getCurrentPageTemplate() throws ExInternal {
    PageTemplate lPageTemplate;

    try {
      lPageTemplate = mPageTemplates.element();
    }
    catch (NoSuchElementException e) {
      throw new ExInternal("Failed to get current page template as no page templates exist", e);
    }

    return lPageTemplate;
  }

  /**
   * Applies the page attributes of the current page template to the document, i.e. the page size and margins
   */
  private void applyCurrentPageAttributes() {
    PageTemplate lPageTemplate = getCurrentPageTemplate();
    PageAttributes lPageAttributes = lPageTemplate.getPageAttributes();
    float lTotalLeftMargin = lPageAttributes.getMarginLeft() + lPageAttributes.getBodyAttributes().getMarginLeft();
    float lTotalRightMargin = lPageAttributes.getMarginRight() + lPageAttributes.getBodyAttributes().getMarginRight();
    float lTotalTopMargin = lPageAttributes.getMarginTop() + lPageAttributes.getBodyAttributes().getMarginTop() + lPageAttributes.getHeaderAttributes().getHeight();
    float lTotalBottomMargin = lPageAttributes.getMarginBottom() + lPageAttributes.getBodyAttributes().getMarginBottom() + lPageAttributes.getFooterAttributes().getHeight();

    mDocument.setPageSize(new Rectangle(lPageAttributes.getPageWidth(), lPageAttributes.getPageHeight()));
    mDocument.setMargins(lTotalLeftMargin, lTotalRightMargin, lTotalTopMargin, lTotalBottomMargin);
  }

  /**
   * Adds header and footer renderers for the given page template. A renderer is added for each page that was created
   * using the page template.
   * @param pPageTemplate The page template that may contain header or footer content
   */
  private void addHeaderFooterRenderers(PageTemplate pPageTemplate) {
    PageAttributes lPageAttributes = pPageTemplate.getPageAttributes();
    List<PageNumberPlaceholder> lPageNumberPlaceholders = pPageTemplate.getPageNumberPlaceholders();
    List<Integer> lPageNumbers = pPageTemplate.getPageNumbersAppliedTo();

    pPageTemplate.getHeaderContent().ifPresent(pHeaderContent -> {
      HeaderRenderer lHeaderRenderer = new HeaderRenderer(lPageAttributes, pHeaderContent, lPageNumberPlaceholders, lPageNumbers);
      mHeaderFooterRenderers.add(lHeaderRenderer);
    });

    pPageTemplate.getFooterContent().ifPresent(pFooterContent -> {
      FooterRenderer lFooterRenderer = new FooterRenderer(lPageAttributes, pFooterContent, lPageNumberPlaceholders, lPageNumbers);
      mHeaderFooterRenderers.add(lFooterRenderer);
    });
  }
}
