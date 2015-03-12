package net.foxopen.fox.module.parsetree.presentationnode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.dom.DOM;

//public interface AttributesProvider {
//  public Map<String, String> getAttributes();
//}

public abstract class GenericAttributesPresentationNode extends PresentationNode {
  private final Map<String, String> mAttributes = new HashMap<>();

  /**
   * Take in the current DOM node to store the attributes on it if it's an element
   *
   * @param pCurrentNode
   */
  protected GenericAttributesPresentationNode(DOM pCurrentNode) {
    if (pCurrentNode.isElement()) {
      // TODO - NP - Was looking at the hyphenated attribute idea here. Commented out while we think about implementation
//      for (Map.Entry<String, String> lAttributeEntry : pCurrentNode.getAttributeMap().entrySet()) {
//        mAttributes.put(StringUtil.hyphenateInitCappedString(lAttributeEntry.getKey(), true), lAttributeEntry.getValue());
//      }
      mAttributes.putAll(pCurrentNode.getAttributeMap());
    }
  }

  /**
   * Returns an immutable copy of the attributes map
   *
   * @return Immutable Map
   */
  public Map<String, String> getAttributes() {
    return Collections.unmodifiableMap(mAttributes);
  }

  /**
   * This is a slightly hacky method for PresentationNode implementations to add in an attribute after they have already
   * been grabbed off the underlying DOM node
   *
   * @param pAttributeName Attribute Name (including namespace, e.g. fox:attr)
   * @param pAttributeValue Attribute value
   * @return The previous value associated with pAttributeName, or
   *         <tt>null</tt> if there was no mapping for pAttributeName.
   */
  protected String addAttribute(String pAttributeName, String pAttributeValue) {
    return mAttributes.put(pAttributeName, pAttributeValue);
  }

  public String getAttrOrNull(String pAttributeName) {
    return mAttributes.get(pAttributeName);
  }
}
