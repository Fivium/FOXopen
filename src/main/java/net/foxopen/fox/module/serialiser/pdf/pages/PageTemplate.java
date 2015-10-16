package net.foxopen.fox.module.serialiser.pdf.pages;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers.PageNumberPlaceholder;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * The template used when creating and manipulating document pages
 */
public class PageTemplate {
  /**
   * The attributes of the pages created using this template (i.e. dimensions)
   */
  private final PageAttributes mPageAttributes;
  /**
   * The header content of pages created using this template, this is set when a header command is encountered in the
   * scope of the page template
   */
  private Optional<HeaderFooterContent> mHeaderContent = Optional.empty();
  /**
   * The footer content of pages created using this template, this is set when a footer command is encountered in the
   * scope of the page template
   */
  private Optional<HeaderFooterContent> mFooterContent = Optional.empty();
  /**
   * A list of page number placeholders adding to pages created using this template, that should be set prior to
   * rendering the headers/footers
   */
  private List<PageNumberPlaceholder> mPageNumberPlaceholders = new LinkedList<>();
  /**
   * The numbers of pages created using this template
   */
  private List<Integer> mPageNumbersAppliedTo = new LinkedList<>();

  /**
   * Creates a page template with the given attributes
   * @param pPageAttributes The page attributes
   */
  public PageTemplate(PageAttributes pPageAttributes) {
    mPageAttributes = pPageAttributes;
  }

  /**
   * Get the page attributes of all pages created using this template
   * @return The page attributes of pages created using this template
   */
  public PageAttributes getPageAttributes() {
    return mPageAttributes;
  }

  /**
   * Return the header content if it has been set
   * @return The header content or empty
   */
  public Optional<HeaderFooterContent> getHeaderContent() {
    return mHeaderContent;
  }

  /**
   * Set the header content that should be added to all pages created using this template. Pages that already have been
   * added will still have the header content added.
   * @param pHeaderContent The content to be added to all pages created using this template
   * @throws ExInternal If the header content has already been set
   */
  public void setHeaderContent(HeaderFooterContent pHeaderContent) throws ExInternal {
    if (mHeaderContent.isPresent()) {
      throw new ExInternal("Page header content is already set");
    }

    mHeaderContent = Optional.of(pHeaderContent);
  }

  /**
   * Return the footer content if it has been set
   * @return The footer content or empty
   */
  public Optional<HeaderFooterContent> getFooterContent() {
    return mFooterContent;
  }

  /**
   * Set the footer content that should be added to all pages created using this template. Pages that already have been
   * added will still have the footer content added.
   * @param pFooterContent The content to be added to all pages created using this template
   * @throws ExInternal If the footer content has already been set
   */
  public void setFooterContent(HeaderFooterContent pFooterContent) {
    if (mFooterContent.isPresent()) {
      throw new ExInternal("Page footer content is already set");
    }

    mFooterContent = Optional.of(pFooterContent);
  }

  /**
   * Add a page number placeholder that will be resolved when the header/footer content the placeholder is added to is
   * added to the template pages
   * @param pPageNumberPlaceholder The page number placeholder that should be resolved when the header/footer is added
   */
  public void addPageNumberPlaceholder(PageNumberPlaceholder pPageNumberPlaceholder) {
    mPageNumberPlaceholders.add(pPageNumberPlaceholder);
  }

  /**
   * Get a list of the page number placeholders that have been added to this page template, and should be resolved when
   * adding the header/footer content to the template pages
   * @return The page number placeholders that have been added to this page template
   */
  public List<PageNumberPlaceholder> getPageNumberPlaceholders() {
    return mPageNumberPlaceholders;
  }

  /**
   * Add the number of a page that has been created using this page template
   * @param pPageNumber The number of the page created using this template
   */
  public void addPageNumberAppliedTo(int pPageNumber) {
    mPageNumbersAppliedTo.add(pPageNumber);
  }

  /**
   * Return the page numbers of pages created using this page template
   * @return the page numbers of pages created using this page template
   */
  public List<Integer> getPageNumbersAppliedTo() {
    return mPageNumbersAppliedTo;
  }
}
