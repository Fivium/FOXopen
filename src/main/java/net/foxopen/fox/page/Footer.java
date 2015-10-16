package net.foxopen.fox.page;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExModule;

import java.util.EnumMap;
import java.util.Map;

/**
 * Contains the page definition footer properties, and methods to create the footer properties from the DOM
 */
public class Footer extends HeaderFooter {
  /**
   * The name of the page definition footer element, namespace prefixed
   */
  public static final String ELEMENT_NAME = "fm:footer";

  /**
   * Returns a page definition footer constructed from the page definition footer DOM
   * @param pBodyDOM The footer DOM
   * @return A footer constructed from the footer DOM
   * @throws ExModule If the footer DOM is invalid, for example required attributes are missing
   */
  public static Footer createFromDOM(DOM pBodyDOM)
  throws ExModule {
    AttributeResolver<Attribute> lAttributeResolver = new AttributeResolver(ATTRIBUTE_DEFINITIONS);
    Map<Attribute, String> lAttributes = lAttributeResolver.resolveAttributes(pBodyDOM, () -> new EnumMap<>(Attribute.class));

    return new Footer(lAttributes.get(Attribute.HEIGHT));
  }

  /**
   * Creates a page definition footer with the given height. Content may overflow upwards outside of this height.
   * @param pHeight The height of the footer
   */
  public Footer(String pHeight) {
    super(pHeight);
  }
}
