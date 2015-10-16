package net.foxopen.fox.page;

/**
 * Defines the properties of a DOM attribute
 */
public class AttributeDefinition {
  private final String mName;
  private final boolean mIsRequired;

  /**
   * Creates an attribute definition with the given properties
   * @param pName The attribute name
   * @param pIsRequired True if the attribute is required, false if optional
   */
  public AttributeDefinition(String pName, boolean pIsRequired) {
    mName = pName;
    mIsRequired = pIsRequired;
  }

  /**
   * Returns the attribute name
   * @return The attribute name
   */
  public String getName() {
    return mName;
  }

  /**
   * Returns true if the attribute is required, false if optional
   * @return True if the attribute is required, false if optional
   */
  public boolean isRequired() {
    return mIsRequired;
  }
}
