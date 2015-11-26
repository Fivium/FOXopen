package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.dom.DOM;
import nu.xom.Element;

import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Namespace context support for a DOM with arbitrary namespaces defined. This should only be used for XPath processing
 * of externally provided XML within the FOX engine. <br><br>
 *
 * For FOX internal DOMs the {@link DefaultNamespaceContext} should suffice as it does not incur the overhead of reading
 * namespaces from a document or maintaining Maps.
 */
public class DynamicNamespaceContext
implements NamespaceContext {

  private Map<String, String> mPrefixToURIMap = new HashMap<String, String>();
  private Map<String, String> mURIToPrefixMap = new HashMap<String, String>();

  public void addNamespace(String pPrefix, String pURI){
    mPrefixToURIMap.put(pPrefix, pURI);
    mURIToPrefixMap.put(pURI, pPrefix);
  }

  public String getPrefix(String pURI){
    return mURIToPrefixMap.get(pURI);
  }

  public String getNamespaceURI(String pPrefix){
    return mPrefixToURIMap.get(pPrefix);
  }

  public Iterator getPrefixes(String namespaceURI) {
    return null;
  }

  /**
   * Creates a new DynamicNamespaceContext for the given Element DOM. Note that the underlying XOM implentation
   * will only expose namespaces defined on or above this node. A full-traversal search through the whole DOM
   * would be possible but has not been implemented.
   * @param pDOM The element to read namespace definitions from.
   */
  public DynamicNamespaceContext(DOM pDOM){
    Element lElement = (Element) pDOM.getNode();

    int n = lElement.getNamespaceDeclarationCount();
    for(int i = 0; i < n; i++){
      String lPrefix = lElement.getNamespacePrefix(i);
      String lURI = lElement.getNamespaceURI(lPrefix);

      addNamespace(lPrefix, lURI);
    }
  }
}
