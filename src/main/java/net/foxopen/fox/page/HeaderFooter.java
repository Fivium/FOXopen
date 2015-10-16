package net.foxopen.fox.page;

import java.util.EnumMap;
import java.util.Map;

/**
 * Contains the properties of a page definition header or footer
 */
public abstract class HeaderFooter {
  /**
   * The possible attributes of a page definition header or footer
   */
  protected enum Attribute {
    HEIGHT
  }

  protected static final Map<Attribute, AttributeDefinition> ATTRIBUTE_DEFINITIONS = new EnumMap<>(Attribute.class);
  static {
    ATTRIBUTE_DEFINITIONS.put(Attribute.HEIGHT, new AttributeDefinition("height", true));
  }

  private final String mHeight;

  /**
   * Creates a page definition header or footer the given height
   * @param pHeight The height of the header or footer
   */
  public HeaderFooter(String pHeight) {
    mHeight = pHeight;
  }

  /**
   * Returns the height of the header or footer
   * @return The height of the header or footer
   */
  public String getHeight() {
    return mHeight;
  }
}
