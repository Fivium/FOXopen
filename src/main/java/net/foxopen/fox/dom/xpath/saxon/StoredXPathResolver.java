package net.foxopen.fox.dom.xpath.saxon;

/**
 * Implementors of this interface are able to provide XPath strings based on an XPath name when required by a
 * {@link StoredXPathTranslator}.
 */
public interface StoredXPathResolver {

  /**
   * Attempts to resolve the given XPath name to an XPath string. Returns null if the XPath cannot be found.
   * @param pXPathName Name of XPath string required.
   * @return XPath string or null.
   */
  String resolveXPath(String pXPathName);

}
