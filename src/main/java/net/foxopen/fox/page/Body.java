package net.foxopen.fox.page;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExModule;

import java.util.EnumMap;
import java.util.Map;

/**
 * Contains the page definition body properties, and methods to create the body properties from the DOM
 */
public class Body {
  /**
   * The possible attributes of a page definition body
   */
  private enum Attribute {
    MARGIN_LEFT,
    MARGIN_RIGHT,
    MARGIN_TOP,
    MARGIN_BOTTOM
  }

  /**
   * The name of the page definition body element, namespace prefixed
   */
  public static final String ELEMENT_NAME = "fm:body";
  private static final Map<Attribute, AttributeDefinition> ATTRIBUTE_DEFINITIONS = new EnumMap<>(Attribute.class);

  static {
    ATTRIBUTE_DEFINITIONS.put(Attribute.MARGIN_LEFT, new AttributeDefinition("margin-left", true));
    ATTRIBUTE_DEFINITIONS.put(Attribute.MARGIN_RIGHT, new AttributeDefinition("margin-right", true));
    ATTRIBUTE_DEFINITIONS.put(Attribute.MARGIN_TOP, new AttributeDefinition("margin-top", true));
    ATTRIBUTE_DEFINITIONS.put(Attribute.MARGIN_BOTTOM, new AttributeDefinition("margin-bottom", true));
  }

  /**
   * Returns a page definition body constructed from the page definition body DOM
   * @param pBodyDOM The body DOM
   * @return A body constructed from the body DOM
   * @throws ExModule If the body DOM is invalid, for example required attributes are missing
   */
  public static Body createFromDOM(DOM pBodyDOM)
  throws ExModule {
    AttributeResolver<Attribute> lAttributeResolver = new AttributeResolver(ATTRIBUTE_DEFINITIONS);
    Map<Attribute, String> lAttributes = lAttributeResolver.resolveAttributes(pBodyDOM, () -> new EnumMap<>(Attribute.class));

    return new Body(lAttributes.get(Attribute.MARGIN_LEFT),
                    lAttributes.get(Attribute.MARGIN_RIGHT),
                    lAttributes.get(Attribute.MARGIN_TOP),
                    lAttributes.get(Attribute.MARGIN_BOTTOM));
  }

  private final String mMarginLeft;
  private final String mMarginRight;
  private final String mMarginTop;
  private final String mMarginBottom;

  /**
   * Creates a page definition body with the specified margins
   * @param pMarginLeft The left margin of the body
   * @param pMarginRight The right margin of the body
   * @param pMarginTop The top margin of the body
   * @param pMarginBottom The bottom margin of the body
   */
  public Body(String pMarginLeft, String pMarginRight, String pMarginTop, String pMarginBottom) {
    mMarginLeft = pMarginLeft;
    mMarginRight = pMarginRight;
    mMarginTop = pMarginTop;
    mMarginBottom = pMarginBottom;
  }

  /**
   * Returns the left margin of the body
   * @return The left margin of the body
   */
  public String getMarginLeft() {
    return mMarginLeft;
  }

  /**
   * Returns the right margin of the body
   * @return The right margin of the body
   */
  public String getMarginRight() {
    return mMarginRight;
  }

  /**
   * Returns the top margin of the body
   * @return The top margin of the body
   */
  public String getMarginTop() {
    return mMarginTop;
  }

  /**
   * Returns the bottom margin of the body
   * @return The bottom margin of the body
   */
  public String getMarginBottom() {
    return mMarginBottom;
  }
}
