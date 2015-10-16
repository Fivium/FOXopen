package net.foxopen.fox.page;


import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExModule;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contains the module page definition properties, and methods for creating the page definition from the DOM
 */
public class PageDefinition {
  /**
   * The possible attributes of a page definition
   */
  private enum Attribute {
    NAME,
    PAGE_WIDTH,
    PAGE_HEIGHT,
    MARGIN_LEFT,
    MARGIN_RIGHT,
    MARGIN_TOP,
    MARGIN_BOTTOM
  }

  /**
   * The possible child elements of a page definition
   */
  private enum ChildElement {
    BODY,
    HEADER,
    FOOTER
  }

  /**
   * The name of the page definition element, namespace prefixed
   */
  public static final String ELEMENT_NAME = "fm:page-definition";
  private static final Map<Attribute, AttributeDefinition> ATTRIBUTE_DEFINITIONS = new EnumMap<>(Attribute.class);
  private static final Map<ChildElement, ChildElementDefinition> CHILD_ELEMENT_DEFINITIONS = new EnumMap<>(ChildElement.class);

  static {
    ATTRIBUTE_DEFINITIONS.put(Attribute.NAME, new AttributeDefinition("name", true));
    ATTRIBUTE_DEFINITIONS.put(Attribute.PAGE_WIDTH, new AttributeDefinition("page-width", true));
    ATTRIBUTE_DEFINITIONS.put(Attribute.PAGE_HEIGHT, new AttributeDefinition("page-height", true));
    ATTRIBUTE_DEFINITIONS.put(Attribute.MARGIN_LEFT, new AttributeDefinition("margin-left", true));
    ATTRIBUTE_DEFINITIONS.put(Attribute.MARGIN_RIGHT, new AttributeDefinition("margin-right", true));
    ATTRIBUTE_DEFINITIONS.put(Attribute.MARGIN_TOP, new AttributeDefinition("margin-top", true));
    ATTRIBUTE_DEFINITIONS.put(Attribute.MARGIN_BOTTOM, new AttributeDefinition("margin-bottom", true));

    CHILD_ELEMENT_DEFINITIONS.put(ChildElement.BODY, new ChildElementDefinition(Body.ELEMENT_NAME, false));
    CHILD_ELEMENT_DEFINITIONS.put(ChildElement.HEADER, new ChildElementDefinition(Header.ELEMENT_NAME, false));
    CHILD_ELEMENT_DEFINITIONS.put(ChildElement.FOOTER, new ChildElementDefinition(Footer.ELEMENT_NAME, false));
  }

  /**
   * Returns a page definition constructed from the page definition DOM
   * @param pPageDefinitionDOM The page definition DOM
   * @return A page definition constructed from the page definition DOM
   * @throws ExModule If the page definition DOM is invalid, for example required attributes are missing
   */
  public static PageDefinition createFromDOM(DOM pPageDefinitionDOM)
  throws ExModule {
    AttributeResolver<Attribute> lAttributeResolver = new AttributeResolver(ATTRIBUTE_DEFINITIONS);
    ChildElementResolver<ChildElement> lChildElementResolver = new ChildElementResolver<>(CHILD_ELEMENT_DEFINITIONS);

    Map<Attribute, String> lAttributes = lAttributeResolver.resolveAttributes(pPageDefinitionDOM, () -> new EnumMap<>(Attribute.class));
    Map<ChildElement, DOM> lChildElements = lChildElementResolver.resolveChildElements(pPageDefinitionDOM, () -> new EnumMap<>(ChildElement.class));

    // Must manually check for presence of the child element DOMs instead of mapping via optional, as the createFromDOM
    // methods throw an exception, which means they can't be used in consumers
    Optional<DOM> lBodyDOM = Optional.ofNullable(lChildElements.get(ChildElement.BODY));
    Optional<DOM> lHeaderDOM = Optional.ofNullable(lChildElements.get(ChildElement.HEADER));
    Optional<DOM> lFooterDOM = Optional.ofNullable(lChildElements.get(ChildElement.FOOTER));

    Optional<Body> lBody = Optional.ofNullable(lBodyDOM.isPresent() ? Body.createFromDOM(lBodyDOM.get()) : null);
    Optional<Header> lHeader = Optional.ofNullable(lHeaderDOM.isPresent() ? Header.createFromDOM(lHeaderDOM.get()) : null);
    Optional<Footer> lFooter = Optional.ofNullable(lFooterDOM.isPresent() ? Footer.createFromDOM(lFooterDOM.get()) : null);

    return new PageDefinition(lAttributes.get(Attribute.NAME),
                              lAttributes.get(Attribute.PAGE_WIDTH),
                              lAttributes.get(Attribute.PAGE_HEIGHT),
                              lAttributes.get(Attribute.MARGIN_LEFT),
                              lAttributes.get(Attribute.MARGIN_RIGHT),
                              lAttributes.get(Attribute.MARGIN_TOP),
                              lAttributes.get(Attribute.MARGIN_BOTTOM),
                              lBody, lHeader, lFooter);
  }

  private final String mName;
  private final String mPageWidth;
  private final String mPageHeight;
  private final String mMarginLeft;
  private final String mMarginRight;
  private final String mMarginTop;
  private final String mMarginBottom;
  private final Optional<Body> mBody;
  private final Optional<Header> mHeader;
  private final Optional<Footer> mFooter;

  /**
   * Creates a page definition with the given properties. Body, header and footer properties can optionally be
   * specified.
   * @param pName The name of the page definition, used to reference it
   * @param pPageWidth The page width
   * @param pPageHeight The page height
   * @param pMarginLeft The left margin of the page
   * @param pMarginRight The right margin of the page
   * @param pMarginTop The top margin of the page
   * @param pMarginBottom The bottom margin of the page
   * @param pBody The properties of the page body
   * @param pHeader The properties of the page header
   * @param pFooter The properties of the page footer
   */
  public PageDefinition(String pName, String pPageWidth, String pPageHeight, String pMarginLeft, String pMarginRight,
                        String pMarginTop, String pMarginBottom, Optional<Body> pBody, Optional<Header> pHeader,
                        Optional<Footer> pFooter) {
    mName = pName;
    mPageWidth = pPageWidth;
    mPageHeight = pPageHeight;
    mMarginLeft = pMarginLeft;
    mMarginRight = pMarginRight;
    mMarginTop = pMarginTop;
    mMarginBottom = pMarginBottom;
    mBody = pBody;
    mHeader = pHeader;
    mFooter = pFooter;
  }

  /**
   * Returns the name of the page definition that can be used to reference it
   * @return The name of the page definition that can be used to reference it
   */
  public String getName() {
    return mName;
  }

  /**
   * Returns the page width
   * @return The page width
   */
  public String getPageWidth() {
    return mPageWidth;
  }

  /**
   * Returns the page height
   * @return The page height
   */
  public String getPageHeight() {
    return mPageHeight;
  }

  /**
   * Returns the left margin of the page
   * @return The left margin of the page
   */
  public String getMarginLeft() {
    return mMarginLeft;
  }

  /**
   * Returns the right margin of the page
   * @return The right margin of the page
   */
  public String getMarginRight() {
    return mMarginRight;
  }

  /**
   * Returns the top margin of the page
   * @return The top margin of the page
   */
  public String getMarginTop() {
    return mMarginTop;
  }

  /**
   * Returns the bottom margin of the page
   * @return The bottom margin of the page
   */
  public String getMarginBottom() {
    return mMarginBottom;
  }

  /**
   * Returns the properties of the page body if they have been specified
   * @return The properties of the page body if they have been specified
   */
  public Optional<Body> getBody() {
    return mBody;
  }

  /**
   * Returns the properties of the page header if they have been specified
   * @return The properties of the page header if they have been specified
   */
  public Optional<Header> getHeader() {
    return mHeader;
  }

  /**
   * Returns the properties of the page footer if they have been specified
   * @return The properties of the page footer if they have been specified
   */
  public Optional<Footer> getFooter() {
    return mFooter;
  }
}
