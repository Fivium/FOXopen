package net.foxopen.fox.page;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExModule;

import java.util.EnumMap;
import java.util.Map;

/**
 * Contains the page definition header properties, and methods to create the header properties from the DOM
 */
public class Header extends HeaderFooter {
  /**
   * The name of the page definition header element, namespace prefixed
   */
  public static final String ELEMENT_NAME = "fm:header";

  /**
   * Returns a page definition header constructed from the page definition header DOM
   * @param pBodyDOM The header DOM
   * @return A header constructed from the header DOM
   * @throws ExModule If the header DOM is invalid, for example required attributes are missing
   */
  public static Header createFromDOM(DOM pBodyDOM)
  throws ExModule {
    AttributeResolver<Attribute> lAttributeResolver = new AttributeResolver(ATTRIBUTE_DEFINITIONS);
    Map<Attribute, String> lAttributes = lAttributeResolver.resolveAttributes(pBodyDOM, () -> new EnumMap<>(Attribute.class));

    return new Header(lAttributes.get(Attribute.HEIGHT));
  }

  /**
   * Creates a page definition header with the given height. Content may overflow downwards outside of this height.
   * @param pHeight The height of the header
   */
  public Header(String pHeight) {
    super(pHeight);
  }
}
