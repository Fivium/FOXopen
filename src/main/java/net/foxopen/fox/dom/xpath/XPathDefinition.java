package net.foxopen.fox.dom.xpath;

/**
 * Encapsulates an XPath string definition, which may have been modified from its original module definition before it can be executed.
 * E.g. XPath definitions containing stored XPaths will have the stored XPath placeholders translated out, but for debugging
 * purposes we need a copy of the XPath containing the placeholders.<br/><br/>
 *
 * This object may be used as a cache key.
 */
public class XPathDefinition {

  private final String mOriginalXPath;
  private final String mExecutableXPath;

  /**
   * Creates a new XPathDefinition representing a path which was modified from its original markup definition before
   * it can be executed.
   * @param pOriginalXPath Original XPath string from module markup.
   * @param pExecutableXPath Translated XPath String which should be used for XPath execution.
   * @return New XPathDefinition.
   */
  public static XPathDefinition forRewrittenXPath(String pOriginalXPath, String pExecutableXPath) {
    return new XPathDefinition(pOriginalXPath, pExecutableXPath);
  }

  /**
   * Creates a new XPathDefinition representing a path which has not been modified before it is executed.
   * @param pXPath XPath string to be executed.
   * @return New XPathDefinition.
   */
  public static XPathDefinition forUnmodifiedXPath(String pXPath) {
    return new XPathDefinition(pXPath, pXPath);
  }

  private XPathDefinition(String pOriginalXPath, String pExecutableXPath) {
    mOriginalXPath = pOriginalXPath;
    mExecutableXPath = pExecutableXPath;
  }

  /**
   * Gets the XPath String to actually be executed, after any rewrites have occurred.
   * @return Valid executable XPath String.
   */
  public String getExecutableXPath() {
    return mExecutableXPath;
  }

  /**
   * Gets the original String used to define this XPath, concatenated with the executable XPath string if the XPath was
   * translated before execution. This is not a valid XPath and should only be used for debugging and error messages etc.
   * @return Debug path information about this XPathDefinition.
   */
  public String getPathForDebug() {
    return !mExecutableXPath.equals(mOriginalXPath) ? mOriginalXPath + " <translated to " + mExecutableXPath + ">" : mExecutableXPath;
  }

  /** equals/hashCode overloaded as this object is used as a cache key */

  @Override
  public boolean equals(Object lOther) {
    if (this == lOther) {
      return true;
    }
    if (!(lOther instanceof XPathDefinition)) {
      return false;
    }

    XPathDefinition lThat = (XPathDefinition) lOther;
    return mOriginalXPath.equals(lThat.mOriginalXPath) && mExecutableXPath.equals(lThat.mExecutableXPath);
  }

  @Override
  public int hashCode() {
    int lResult = mOriginalXPath.hashCode();
    lResult = 31 * lResult + mExecutableXPath.hashCode();
    return lResult;
  }

  @Override
  public String toString() {
    return mExecutableXPath;
  }
}
