package net.foxopen.fox.page;

/**
 * Defines properties of a DOM child element
 */
public class ChildElementDefinition {
  private final String mName;
  private final boolean mIsRequired;

  /**
   * Creates a child element definition with the given properties
   * @param pName The child element name including namespace
   * @param pIsRequired True if the child element is required, false if optional
   */
  public ChildElementDefinition(String pName, boolean pIsRequired) {
    mName = pName;
    mIsRequired = pIsRequired;
  }

  /**
   * Returns the child element name including namespace
   * @return The child element name including namespace
   */
  public String getName() {
    return mName;
  }

  /**
   * Returns true if the child element is required, false if optional
   * @return True if the child element is required, false if optional
   */
  public boolean isRequired() {
    return mIsRequired;
  }
}
