package net.foxopen.fox.module.serialiser.pdf.pages;

import com.itextpdf.tool.xml.css.CssUtils;
import net.foxopen.fox.page.Body;
import net.foxopen.fox.page.HeaderFooter;
import net.foxopen.fox.page.PageDefinition;

import java.util.Optional;
import java.util.function.Function;

/**
 * The attributes of a document page
 */
public class PageAttributes {
  /**
   * Create page attributes from a page definition. This parses the attributes defined in the page definition to the
   * correct units. The page definition units may be in any format allowed by
   * {@link com.itextpdf.tool.xml.css.CssUtils#parsePxInCmMmPcToPt}. Optional dimensions default to 0.
   * @param pPageDefinition The page definition
   * @return Page attributes taken from the page definition
   */
  public static PageAttributes createFromPageDefinition(PageDefinition pPageDefinition) {
    Optional<PageDefinition> lPageDefinitionOptional = Optional.of(pPageDefinition);

    float lPageWidth = parseAttributeOrDefault(lPageDefinitionOptional, PageDefinition::getPageWidth);
    float lPageHeight = parseAttributeOrDefault(lPageDefinitionOptional, PageDefinition::getPageHeight);
    float lMarginLeft = parseAttributeOrDefault(lPageDefinitionOptional, PageDefinition::getMarginLeft);
    float lMarginRight = parseAttributeOrDefault(lPageDefinitionOptional, PageDefinition::getMarginRight);
    float lMarginTop = parseAttributeOrDefault(lPageDefinitionOptional, PageDefinition::getMarginTop);
    float lMarginBottom = parseAttributeOrDefault(lPageDefinitionOptional, PageDefinition::getMarginBottom);

    BodyAttributes lBodyAttributes = createBodyAttributes(pPageDefinition.getBody());
    HeaderFooterAttributes lHeaderAttributes = createHeaderFooterAttributes(pPageDefinition.getHeader());
    HeaderFooterAttributes lFooterAttributes = createHeaderFooterAttributes(pPageDefinition.getFooter());

    return new PageAttributes(lPageWidth, lPageHeight, lMarginLeft, lMarginRight, lMarginTop, lMarginBottom,
                              lBodyAttributes, lHeaderAttributes, lFooterAttributes);
  }

  /**
   * Create body attributes from the page body definition
   * @param pBody The page body definition
   * @return The body attributes parsed from the body definition
   */
  private static BodyAttributes createBodyAttributes(Optional<Body> pBody) {
    float lMarginLeft = parseAttributeOrDefault(pBody, Body::getMarginLeft);
    float lMarginRight = parseAttributeOrDefault(pBody, Body::getMarginRight);
    float lMarginTop = parseAttributeOrDefault(pBody, Body::getMarginTop);
    float lMarginBottom = parseAttributeOrDefault(pBody, Body::getMarginBottom);

    return new BodyAttributes(lMarginLeft, lMarginRight, lMarginTop, lMarginBottom);
  }

  /**
   * Create header or footer attributes from the header or footer definition
   * @param pHeaderFooter The header or footer definition
   * @param <T> The header or footer type
   * @return The header or footer attributes parsed from the header/footer definition
   */
  private static <T extends HeaderFooter> HeaderFooterAttributes createHeaderFooterAttributes(Optional<T> pHeaderFooter) {
    float lHeight = parseAttributeOrDefault(pHeaderFooter, HeaderFooter::getHeight);
    return new HeaderFooterAttributes(lHeight);
  }

  /**
   * Parse unit attributes to points. Units may be in any format allowed by
   * {@link com.itextpdf.tool.xml.css.CssUtils#parsePxInCmMmPcToPt}. If the attribute is not specified in the attribute
   * definitions then the default 0 is returned.
   * @param pAttributeDefinitions The object with dimension attributes
   * @param pAttributeSupplier Supplies the dimension attribute from the attribute definition object
   * @param <T> The type of object with dimension attribute definitions
   * @return The parsed units as points, or 0 if the attribute is not defined
   */
  private static <T> float parseAttributeOrDefault(Optional<T> pAttributeDefinitions, Function<T, String> pAttributeSupplier) {
    return pAttributeDefinitions.map(pAttributeSupplier)
                                .map(CssUtils.getInstance()::parsePxInCmMmPcToPt)
                                .orElse(0f);
  }


  private final float mPageWidth;
  private final float mPageHeight;
  private final float mMarginLeft;
  private final float mMarginRight;
  private final float mMarginTop;
  private final float mMarginBottom;
  private final BodyAttributes mBodyAttributes;
  private final HeaderFooterAttributes mHeaderAttributes;
  private final HeaderFooterAttributes mFooterAttributes;

  /**
   * Create the attributes of a page
   * @param pPageWidth The width of the page
   * @param pPageHeight The height of the page
   * @param pMarginLeft The left margin of a page, all content including headers and footers is within this margin
   * @param pMarginRight The right margin of a page, all content including headers and footers is within this margin
   * @param pMarginTop The top margin of a page, all content including headers and footers is within this margin
   * @param pMarginBottom The bottom margin of a page, all content including headers and footers is within this margin
   * @param pBodyAttributes The attributes of the page body
   * @param pHeaderAttributes The attributes of the page header
   * @param pFooterAttributes The attributes of the page footer
   */
  public PageAttributes(float pPageWidth, float pPageHeight, float pMarginLeft, float pMarginRight, float pMarginTop,
                        float pMarginBottom, BodyAttributes pBodyAttributes, HeaderFooterAttributes pHeaderAttributes,
                        HeaderFooterAttributes pFooterAttributes) {
    mPageWidth = pPageWidth;
    mPageHeight = pPageHeight;
    mMarginLeft = pMarginLeft;
    mMarginRight = pMarginRight;
    mMarginTop = pMarginTop;
    mMarginBottom = pMarginBottom;
    mBodyAttributes = pBodyAttributes;
    mHeaderAttributes = pHeaderAttributes;
    mFooterAttributes = pFooterAttributes;
  }

  public float getPageWidth() {
    return mPageWidth;
  }

  public float getPageHeight() {
    return mPageHeight;
  }

  public float getMarginLeft() {
    return mMarginLeft;
  }

  public float getMarginRight() {
    return mMarginRight;
  }

  public float getMarginTop() {
    return mMarginTop;
  }

  public float getMarginBottom() {
    return mMarginBottom;
  }

  public BodyAttributes getBodyAttributes() {
    return mBodyAttributes;
  }

  public HeaderFooterAttributes getHeaderAttributes() {
    return mHeaderAttributes;
  }

  public HeaderFooterAttributes getFooterAttributes() {
    return mFooterAttributes;
  }
}
