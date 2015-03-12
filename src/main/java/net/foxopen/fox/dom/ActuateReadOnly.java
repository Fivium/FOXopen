/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


*/
package net.foxopen.fox.dom;


import com.sun.org.apache.xerces.internal.dom.DOMImplementationImpl;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.xpath.FoxXPathEvaluator;
import net.foxopen.fox.dom.xpath.FoxXPathEvaluatorFactory;
import net.foxopen.fox.dom.xpath.FoxXPathResultType;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.ex.ExValidation;
import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.ParentNode;
import nu.xom.ProcessingInstruction;
import nu.xom.Serializer;
import nu.xom.Text;
import nu.xom.canonical.Canonicalizer;
import nu.xom.converters.DOMConverter;
import org.xml.sax.SAXException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Actuator for a read-only XML document. This class contains implementations for simple path navigation, XPath navigation,
 * element and attribute value retrieval and node/document serialisation.<br/><br/>
 */
public class ActuateReadOnly
extends ActuateNoAccess
{
  private static final FoxXPathEvaluator gFoxXPathEvaluator = FoxXPathEvaluatorFactory.createEvaluator(FoxGlobals.gXPathBackwardsCompatibility);

  public static final String XML_DECLARATION_UTF_8 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

  ActuateReadOnly(String pAccessViolationInfo, DocControl pDocControl)
  {
   super(pAccessViolationInfo, pDocControl);
  }

  /**
   * Get a single element for the given simple path.<br/>
   * Simple path supports the following XPath tokens:
   * <ul>
   *  <li>/</li>
   *  <li>.</li>
   *  <li>..</li>
   *  <li>*</li>
   *  <li>ELEMENT_NAME</li>
   * </ul>
   * This method returns pNode if pSimplePath is empty.
   * @param pNode The context node of the expression ('.')
   * @param pSimplePath The simple path to evaluate.
   * @return A single matched Element.
   * @throws ExTooFew If no nodes match pSimplePath.
   * @throws ExTooMany If any level of pSimplePath resolves to more than one node.
   */
  public Node get1Element(Node pNode, String pSimplePath)
  throws ExTooFew, ExTooMany {
    return get1Element(pNode, pSimplePath, false, false);
  }

  @Override
  public Node get1ElementByLocalName(Node pNode, String pSimplePath) throws ExInternal, ExTooFew, ExTooMany {
    return get1Element(pNode, pSimplePath, false, true);
  }

  /**
   * Internal method which allows overloading so elements can be created.
   */
  protected Node get1Element(Node pNode, String pSimplePath, boolean pAllowCreate, boolean pUseLocalName)
  throws ExTooFew, ExTooMany {

    // No path return self
    if(pSimplePath.length()==0) return pNode;

    StringBuffer buf = new StringBuffer(pSimplePath);
    Node lNode = pNode;

    // Test for root
    if(buf.charAt(0)=='/') {
      lNode = pNode.getDocument();
      buf.deleteCharAt(0);

      if(lNode == null) {
        throw new ExInternal("Failed to evaluate path '" + pSimplePath + "': relative node " + DOM.getFullNameSafe(pNode) + " is not attached to a document");
      }
    }

    while(buf.length() != 0) {
      // Check for recurse expression //
      if(buf.length() !=0 && buf.charAt(0)=='/') {
        throw new ExInternal("get1E does not support '//' path token: "+pSimplePath,new DOM(pNode));
      }

      String nav = XFUtil.pathPopHead(buf,false);
      if(buf.length() !=0 && buf.charAt(0)=='/') {
        buf.deleteCharAt(0);
      }

      // Skip self reference
      if(nav.equals(".")) continue;

      // Process parent reference
      if(nav.equals("..")) {
        Node lparent = lNode.getParent();
        if(lparent != null) {
          lNode = (ParentNode) lparent;
        }
        continue;
      }

      //note: for some reason this is faster than looping through child elements (element.getChildElements())
      Node lFoundNode = null;
      for(int i=0; i<lNode.getChildCount();i++){
        Node lCurNode = lNode.getChild(i);
        if(lCurNode instanceof Element) {
          boolean lFound;
          if(nav.equals("*")) {
            lFound = true;
          }
          else if(pUseLocalName) {
            lFound = ((Element) lCurNode).getLocalName().equals(nav);
          }
          else {
            lFound = ((Element) lCurNode).getQualifiedName().equals(nav);
          }

          if(lFound){
            if(lFoundNode != null) {
              throw new ExTooMany("UElem get1E getting "+pSimplePath,new DOM(pNode));
            }
            lFoundNode = lCurNode;
          }
        }
      }
      if(lFoundNode == null) {
        //In this actuator this throws ExTooFew. In ActuateReadWrite we overload to create a new element.
        if(pAllowCreate){
          lFoundNode = get1ElementNotMatched(lNode, pSimplePath, nav);
        }
        else {
          throw new ExTooFew("UElem get1E getting "+pSimplePath,new DOM(pNode));
        }
      }
      lNode = lFoundNode;
    }

    return lNode;
  } // end get1Element

  /**
   * Overloadable method for potentially creating a new element under pParentNode if needed.
   * @param pParentNode
   * @param pSimplePath
   * @param pNav
   * @return Nothing.
   * @throws ExInternal In this class, always.
   */
  protected Node get1ElementNotMatched(Node pParentNode, String pSimplePath, String pNav)
  throws ExInternal {
    throwAccessViolation(pParentNode);
    return null;
    //throw new ExInternal("ReadOnly actuator cannot create nodes.");
  }

  /**
   * Gets a single element according to the rules defined for get1Element and evaluates it to a String.
   * @param pNode
   * @param pSimplePath
   * @return
   * @throws ExTooFew
   * @throws ExTooMany
   */
  public String get1String(Node pNode, String pSimplePath)
  throws ExTooFew, ExTooMany
  {
    return value(get1Element(pNode, pSimplePath), false);
  }

  /**
   * Gets a simple path representation of the absolute path to this node from the root of its owning document.
   * Element names will include namespace prefixes.
   * E.g. /ROOT/LEVEL1/LEVEL2
   * @param pNode The Element to get a path for.
   * @return String representation of the Element's path.
   */
  public String getAbsolute(Node pNode) {
    StringBuilder path = new StringBuilder();
    Node node = pNode;
    while ((node != null) && (node instanceof Element)) {
      path.insert(0, ((Element) node).getQualifiedName());
      path.insert(0,"/");
      node = node.getParent();
    }
    return path.toString();
  }

  /**
   * Gets a simple path representation of the path to pNestedNode from pNode.
   * @param pNode The context (parent) node.
   * @param pNestedNode The node to get the path for, relative to pNode.
   * @return String representation of the Element's path.
   */
  public String getRelativeDownToOrNull(Node pNode, Node pNestedNode) {

    if(pNode==pNestedNode) {
      return ".";
    }

    if(!(pNestedNode instanceof Element) || !(pNode instanceof Element) ) {
      return null;
    }

    // Loop for each nested level (bottom up)
    StringBuffer path = new StringBuffer();
    Node node = pNestedNode;
    String lSep = "";
    TRAVERSE_LOOP: do{
      // Record nested node name
      path.insert(0,lSep);
      path.insert(0,((Element) node).getQualifiedName());
      lSep="/";

      // Advance to next level
      node = node.getParent();
      //We reached the top without finding pNode; assume pNestedNode is not a child of pNode and return null
      if(node instanceof Document){
        return null;
      }
      // When node not a true nested node
      if(node==null) {
        return null;
      }

    // Exit loop when parent located
    } while(node!=pNode);

    // Return relatice path
    return path.toString();

  }

  /**
   * Gets the position of this Node within its parent.
   * @param pNode
   * @return The node's position within its parent.
   */
  public int getSiblingIndex(Node pNode) {
    if(pNode.getParent() != null){
      return pNode.getParent().indexOf(pNode);
    } else {
      throw new ExInternal("pNode does not have a parent in getSiblingIndex");
    }
  }

  /**
   * Convenience method for getting an attribute. If the attribute has a prefix (i.e. is in a namespace), the namespace
   * URI is looked up on pElement.
   * @param pElement Element with the Attribute to retrieve.
   * @param pAttrName Name of the Attribute to retrieve.
   * @return An Attribute, or null if one could not be found.
   */
  protected Attribute getAttributeInternal(Element pElement, String pAttrName){
    //Get attribute
    String lNsURI = DOM.getNamespaceURIForNodeName(pElement, pAttrName);
    Attribute lAttribute;
    if(lNsURI != null){
      lAttribute = pElement.getAttribute(DOM.getLocalNameFromNodeName(pAttrName), lNsURI);
    } else {
      lAttribute = pElement.getAttribute(pAttrName);
    }
    return lAttribute;
  }

  @Override
  public String getAttributeNamespaceURI (Node pNode, String pAttrName) {
    Attribute lAttr = getAttributeInternal((Element) pNode, pAttrName);
    if(lAttr == null){
      throw new ExInternal("Attribute called '" + pAttrName + "' not found on this element.");
    }
    else {
      return lAttr.getNamespaceURI();
    }
  }

  /**
   * Gets the value of the requested attribute, or an empty string if it is not set.
   * @param pNode The Element to retrieve the Attribute from.
   * @param pAttrName
   * @return Attribute value or empty string.
   */
  public String getAttribute(Node pNode, String pAttrName) {
    // Cast to Element (checks is an Element)
    Element lElement;
    try {
      lElement = (Element) pNode;
    }
    catch (ClassCastException x) {
      throw new ExInternal("Not an Element", x);
    }

    //Get attribute
    Attribute lAttribute = getAttributeInternal(lElement, pAttrName);

    if(lAttribute != null) {
      return lAttribute.getValue();
    } else {
      return "";
    }
  }

  public final Map<String, String> getAttributes (Node pNode)
  throws ExInternal
  {
    return getAttributes(pNode, null, false);
  }

  public final Map<String, String> getAttributes (Node pNode, String pNamespaceURI)
  throws ExInternal
  {
    return getAttributes(pNode, pNamespaceURI, false);
  }

  /**
   * Builds a Map of attribute names to attribute values for the given Element.
   * @param pNode The Element to retrieve Attributes from.
   * @param pNamespaceURI If not null, only retrieve Attributes which have this Namespace URI.
   * @param pLocalNames If true, the Map keys will be the local attribute names (i.e. without the namespace). If false, the Map keys will be the full attribute name.
   * @return A Map of names to attribute values.
   * @throws ExInternal
   */
  public final Map<String, String> getAttributes (Node pNode, String pNamespaceURI, boolean pLocalNames) {
    // Cast to Element (checks is an Element)
    Element lElement;
    try {
      lElement = (Element)pNode;
    }
    catch (ClassCastException x) {
      throw new ExInternal("Not an Element", x);
    }
    // Build HashMap
    Map<String, String> lAttrMap = new HashMap<String, String>();
    //NamedNodeMap lAttrList = lElement.getAttributes();
    int lAttrCount = lElement.getAttributeCount();
    Attribute lAttr;
    for (int i = 0; i < lAttrCount; i++) {
      lAttr = lElement.getAttribute(i);
      if (pNamespaceURI == null || (pNamespaceURI != null && pNamespaceURI.equals(lAttr.getNamespaceURI()))) {
        lAttrMap.put(pLocalNames ? lAttr.getLocalName() : lAttr.getQualifiedName(), lAttr.getValue());
      }
    }
    return lAttrMap;
  }

  /**
   * Builds an ArrayList of fully-qualified attribute names for the given Element node.
   * @param pNode An Element node.
   * @return A list of fully-qualified attribute names in an arbitrary order.
   */
  public ArrayList<String> getAttrNames (Node pNode) {
    return new ArrayList<String>(getAttributes(pNode).keySet());
  }


  public Node getElemByRef(Node pNode, String pRefString) {
    Node found = mDocControl.getNodeByRefOrNull(pRefString, false);
    if(found == null) {
      throw new ExInternal("GetElemByRef failed to locate ref ("+pRefString+") in"
      , new DOM(getRootElement(pNode))
      );
    }
    return found;
  }

  public Node getElemByRefOrNull(Node pNode, String pRefString) {
    Node found = mDocControl.getNodeByRefOrNull(pRefString, false);
    return found;
  }

  /**
   * Returns the last child element of the given Node.
   * @param pNode The node to get the last child from.
   * @return a single Element.
   * @throws ExTooFew If pNode has no children.
   */
  public Node getLastChildElement(Node pNode)
  throws ExTooFew {
    Elements lElements = ((Element) pNode).getChildElements();
    if(lElements.size()==0){
      throw new ExTooFew("getLastChildElem(): XML node at \""+getAbsolute(pNode)+"\" has no child Element!");
    }
    return lElements.get(lElements.size()-1);
  }


  /**
   * Gets the local name (i.e. without namespace prefix) of pNode, if pNode is a named node type.
   * For unnamed nodes, returns an empty string.
   * @param pNode The target node.
   * @return pNode's local name.
   */
  public String getLocalName(Node pNode) {
    return DOM.getLocalNameSafe(pNode);
  }

  /**
   * Gets the fully qualified name (including namespace prefix), if pNode is a named node type.
   * For unnamed nodes, returns an empty string.
   * @param pNode The target node.
   * @return pNode's fully qualified name.
   */
  public String getName(Node pNode) {
    return DOM.getFullNameSafe(pNode);
  }

  /**
   * Get the next sibling of pNode, in document order. Returns null if pNode has no following siblings.<br/>
   * XPath equivalent: <code>./following-sibling::*[1]</code> or <code>./following-sibling::node()[1]</code>
   * @param pNode The target Element.
   * @param pElementsOnly If true, only considers Elements. If false, considers all nodes.
   * @return pNode's next sibling, or null.
   */
  public Node getNextSiblingOrNull(Node pNode, boolean pElementsOnly){
    ParentNode lParent = pNode.getParent();
    if(lParent == null){
      return null;
    }
    int lIndex = lParent.indexOf(pNode);
    while(lIndex < lParent.getChildCount() - 1){
      Node lNextSibling = lParent.getChild(++lIndex);
      //avoid non-Element siblings
      if(lNextSibling instanceof Element || !pElementsOnly) {
        return lNextSibling;
      }
    }
    return null;
  }

  /**
   * Gets the parent Element of pNode. If pNode is a root element or unattached, this method returns null.
   * @param pNode The target Node.
   * @return pNode's parent Element, or null.
   */
  public Node getParentOrNull(Node pNode) {
    Node lParent = pNode.getParent();
    if(lParent==null || lParent instanceof Document) {
      return null;
    } else {
      return lParent;
    }
  }

  /**
   * Gets the parent Element of pNode. If pNode is a root element or unattached, pNode is returned.
   * @param pNode The target Node.
   * @return pNode's parent Element, or pNode if it has none.
   */
  public Node getParentOrSelf(Node pNode) {
    Node lParent = pNode.getParent();
    if(lParent==null || lParent instanceof Document) {
      return pNode;
    }
    else {
      return lParent;
    }
  }

  /**
   * Get the previous sibling of pNode, in document order. Returns null if pNode has no previous siblings.<br/>
   * XPath equivalent: <code>./preceding-sibling::*[1]</code> or <code>./preceding-sibling::node()[1]</code>
   * @param pNode The target Element.
   * @param pElementsOnly If true, only considers Elements. If false, considers all nodes.
   * @return pNode's previous sibling, or null.
   */
  public Node getPreviousSiblingOrNull(Node pNode, boolean pElementsOnly) {
    ParentNode lParent = pNode.getParent();
    if(lParent == null){
      return null;
    }
    int lIndex = lParent.indexOf(pNode);
    while(lIndex > 0){
      Node lNextSibling = lParent.getChild(--lIndex);
      //Avoid non-Element siblings
      if(lNextSibling instanceof Element || !pElementsOnly) {
        return lNextSibling;
      }
    }
    return null;
  }

  /**
   * Gets the foxid attribute for pNode.
   * @param pNode The target node.
   * @return The value of pNode's foxid attribute.
   * @throws ExInternal If the foxid is not set for this node.
   */
  public String getRef(Node pNode)
  throws ExInternal {
    String lRef = getAttribute(pNode, FOXID);
    if(lRef.length()==0) {
      throw new ExInternal("getRef() called on Read Only DOM Node that did not have a foxid: "+getName(pNode));
    }
    return lRef;
  }

  public final String getPerfectRef(final Node pNode)
    throws ExInternal
  {
    StringBuffer lStringBuffer= new StringBuffer();
    if(!(pNode instanceof Element)) {
      throw new ExInternal("non-Element passed to getPefectRef()");
    }
    Node lNode = pNode;
    while(lNode != null && DOM.NodeType.getNodeType(lNode) == DOM.NodeType.ELEMENT) {
      lStringBuffer.append(getRef(pNode)).append("/");
      lNode=lNode.getParent();
    }
    if(lNode != null && DOM.NodeType.getNodeType(lNode) == DOM.NodeType.DOCUMENT) {
      lStringBuffer.append("*DOC*");
    }
    return lStringBuffer.toString();
  }

  /**
   * Gets the root Element of pNode's current Document.
   * @param pNode The target node.
   * @return The root Element of pNode's Document.
   */
  public Node getRootElement(Node pNode) {
    if(pNode instanceof Document) {
      return ((Document)pNode).getRootElement();
    }
    return pNode.getDocument().getRootElement();
  }

  @Override
  public DOMList getUL(Node pNode, String pSimplePath) {
    return getUL(pNode, pSimplePath, false);
  }

  @Override
  public DOMList getULByLocalName(Node pNode, String pSimplePath) {
    return getUL(pNode, pSimplePath, true);
  }

  /**
   * Get an unconnected list of all elements which match the lowest level of the given simple path.<br/>
   * For instance, a path of /ITEM_LIST/ITEM/SUBITEM_LIST/SUBITEM would return all the SUBITEMs within all ITEMs.<br/>
   * Simple path supports the following XPath tokens:
   * <ul>
   * <li>/</li>
   * <li>.</li>
   * <li>..</li>
   * <li>*</li>
   * <li>ELEMENT_NAME</li>
   * </ul>
   * This method returns a DOMList containing pNode if pSimplePath is empty.<br/>
   * If the path matches no nodes, an empty DOMList is returned.
   * @param pNode The context node of the expression ('.')
   * @param pSimplePath The simple path to evaluate.
   * @param pUseLocalNames If true, the element's local name is used for the search. Otherwise the path should contain
   * fully qualified names.
   * @return A DOMList containing all matched elements.
   */
  protected DOMList getUL(Node pNode, String pSimplePath, boolean pUseLocalNames) {

    ArrayList forwardList;
    int si, sls;
    String nav;
    Node lParent;
    Node lNode = pNode;

    // No path return self
    if(pSimplePath.length()==0){
      forwardList = new ArrayList(1);
      forwardList.add(new DOM(pNode));
      return new DOMList(forwardList);
    }
    // Buffer to pop pathname words from
    StringBuffer buf = new StringBuffer(pSimplePath);

    // Test for root doc pathname
    if(buf.charAt(0)=='/') {
      lNode = pNode.getDocument();
      buf.deleteCharAt(0);
    }

    // Initialise source list to current node
    ArrayList sourceList = new ArrayList();
    sourceList.add(lNode);

    // Loop to process each word in pathname
    PATHNAME_LOOP: while(buf.length() != 0) {

      // Check for recurse expression // in pathname
      if(buf.length() !=0 && buf.charAt(0)=='/') {
        throw new ExInternal(
          XFUtil.getJavaStackMethods(1)[0]+"get1E does not support // as yet: "+pSimplePath
        , new DOM(pNode)
        );
      }

      // Pop next navigation word from head of pathname
      nav = XFUtil.pathPopHead(buf,false);
      if(buf.length() !=0 && buf.charAt(0)=='/') {
        buf.deleteCharAt(0);
      }

      // Initialise next carry forward results list
      forwardList = new ArrayList();

      // Loop to process each element for the source list
      sls=sourceList.size();
      LISTNODE_LOOP: for(si=0; si<sls; si++) {
        lNode = (Node)sourceList.get(si);

        // Skip self reference pathword
        if(nav.equals(".")) {
          forwardList.add(lNode);
          continue;
        }

        // Process parent reference pathword
        if(nav.equals("..")) {
          lParent = lNode.getParent();
          if(lParent instanceof Element) {
            forwardList.add(lParent);
          }
          continue;
        }

        // Loop through child nodes looking for Element
        for(int i=0; i<lNode.getChildCount();i++){
          Node lCurNode = lNode.getChild(i);
          if(lCurNode instanceof Element) {
            boolean lFound;
            if(nav.equals("*")) {
              lFound = true;
            }
            else if(pUseLocalNames) {
              lFound = ((Element) lCurNode).getLocalName().equals(nav);
            }
            else {
              lFound = ((Element) lCurNode).getQualifiedName().equals(nav);
            }

            if(lFound){
              forwardList.add(lCurNode);
            }
          }
        }

      } // end LISTNOE_LOOP

      // Carry forward into new source list for next pathname word
      sourceList = forwardList;

    } // end PATHNAME_LOOP

    // Return proper UElemList
    return new DOMList(sourceList);
  }

  /**
   * Concatenates the text nodes of pNode and appends them to pStringBuffer. If pNode is a complex element and pDeep
   * is true, recurses down into pNode and concatenates all text nodes at all levels below and including pNode.
   * @param pNode The target node.
   * @param pStringBuffer The StringBuffer to append the result to.
   * @param pDeep If true, recurse down tree.
   */
  private void _value(Node pNode, StringBuffer pStringBuffer, boolean pDeep) {
    for(int i=0; i < pNode.getChildCount(); i++){
      Node c = pNode.getChild(i);
      if(c instanceof Text){
        pStringBuffer.append(((Text)c).getValue());
      } else if (c instanceof Element && pDeep){
        _value(c, pStringBuffer, pDeep);
      }
    }
  } //  _value


  /**
   * Gets the concatenated text nodes of pNode. If pNode is a complex element and pDeep is true, recurses
   * down into pNode and concatenates all text nodes at all levels below and including pNode.
   * @param pNode The target Node.
   * @param pDeep If true, recurse into child nodes to find text nodes.
   * @return The concatenated text nodes as a String, or empty string if none exist.
   */
  public String value(Node pNode, boolean pDeep) {
    //Note: previous version had more complicated logic here to short-circuit if this node was an element with
    //a single child text node. Removed for code simplicity: performance should be monitored but is unlikely to be
    //affected.

    // Attribute or Text value
    if(pNode instanceof Attribute) {
      return ((Attribute) pNode).getValue();
    } else if(pNode instanceof Text){
      return ((Text) pNode).getValue();
    }

    // Standard recursive processing to construct text value
    StringBuffer sb = new StringBuffer();
    _value(pNode, sb, pDeep);
    return sb.toString();
  }

  /** Return immediate text content of node but process it to remove whitespace that may have been added
   *  by some XML transformation processes (such as Oracle XML SQL). Whitespace is NOT just trimed away -
   *  the whitespace is actually looked at critically to identify leading/trailing NL SP SP* sequences.
   *  E.g:  String: [\n] [ ] [ ] [ ] [ ][R] [e] [f] [ ] [\n] [ ] [ ] [ ] [ ] [\n] [ ] [ ] [ ] [ ]
   *       becomes: [R] [e] [f] [ ]
   */
  public String valueWhitespaceIntelligent(Node pNode)
  throws ExInternal
  {
    // Get value in using standard implemenation
    String lValue = DOM.toJavaString(value(pNode, false));
    // When value not comming from text or element - return unchanged
    if(!(pNode instanceof Element) && !(pNode instanceof Text)) {
      return lValue;
    }
    // When zero length - return unchanged
    int len = lValue.length();
    if(len==0) {
      return lValue;
    }
    // When value do
    if(len!=1 && lValue.charAt(0)!='\n' && lValue.charAt(len-1)!=' ') {
      return lValue;
    }
    char[] c = lValue.toCharArray();
    int to=len;
    int from=0;
    // Removing trailing sequences NL SP SP (but not !NL SP SP)
    int i;
    TAIL_LINE_LOOP: while(true) {
      i=to-1;
      while(i>-1 && c[i]==' ') {
        i--;
      }
      if(i==-1 || c[i]!='\n') {
        break TAIL_LINE_LOOP;
      }
      to=i;
    } // TAIL_LINE_LOOP
    // Remove leading sequences NL SP SP (but not !NL SP SP)
    LEAD_LINE_LOOP: while(true) {
      i=from;
      if(i==to || c[i]!='\n') {
        break LEAD_LINE_LOOP;
      }
      i++;
      while(i<to && c[i]==' ') {
        i++;
      }
      from=i;
    } // LEAD_LINE_LOOP
    // Return middle part  of string
    lValue = lValue.substring(from, to);
    return lValue;
  }


  public DOMList xpathUL(Node pNode, String pXPath, ContextUElem pContextUElem)
  throws ExBadPath {
    return gFoxXPathEvaluator.evaluate(pXPath, new DOM(pNode), pContextUElem, FoxXPathResultType.DOM_LIST).asDOMList();
  }

  @Override
  public DOMList xpathUL(Node pNode, String pXPath)
  throws ExInternal, ExBadPath {
    return gFoxXPathEvaluator.evaluate(pXPath, new DOM(pNode), null, FoxXPathResultType.DOM_LIST).asDOMList();
  }

  /**
  * Returns whether the xpath is true or false.
  * The xpath is false if the path returns a nodelist of size 0 .
  * The xpath is true if the path returns a nodelist of size 1+.
  */
  @Override
  public synchronized boolean xpathBoolean(Node pNode, String pXPath)
  throws ExBadPath {


    // Short circuit true expressions "." "" "true()" "1" (this does not inc XPATH usage count)
    if(pXPath.length()==0
    || pXPath.equals(".")
    || pXPath.equals("1")
    || pXPath.equals("true()")
    )
    {
      return true;
    }

    // Short circuit false expressions "false()" (this does not inc XPATH usage count)
    if(pXPath.equals("0")
    ||  pXPath.equals("false()")
    )
    {
      return false;
    }

    //Otherwise delegate to standard xpath processing
    return gFoxXPathEvaluator.evaluate(pXPath, new DOM(pNode), null, FoxXPathResultType.BOOLEAN).asBoolean();
  }

  @Override
  public synchronized String xpathString(Node pNode, String pXPath)
  throws ExInternal, ExBadPath
  {

    // Short circuit self node xpath
    if ( pXPath.equals("") || pXPath.equals(".") ) {
       return value(pNode, false);
    }

    return gFoxXPathEvaluator.evaluate(pXPath, new DOM(pNode), null, FoxXPathResultType.STRING).asString();
  }

  @Override
  public Node xpath1Element(Node pNode, String pXPath)
  throws ExTooFew, ExTooMany, ExBadPath {
    return xpath1Element(pNode, pXPath,  null);
  }

  public Node xpath1Element(Node pNode, String pXPath, ContextUElem pContextUElem)
  throws ExTooFew, ExTooMany, ExBadPath {


    // Evaluate xpath expression
    DOMList lDOMList = xpathUL(pNode, pXPath, pContextUElem);

    // Check for cardinality exceptions
    int l = lDOMList.getLength();
    if(l==0) {
      throw new ExTooFew("xpath1Element expected 1 element, got 0 for XPath " + pXPath);
    }
    if(l>1) {
      throw new ExTooMany("xpath1Element expected 1 element, got " + l + " for XPath " + pXPath);
    }

    // Check for datatype exceptions
    DOM lDOM = lDOMList.get(0);
    if(!(lDOM.getNode() instanceof Element)) {
      throw new ExBadPath("XPATH lead to a node which was not of type Element\nBad XPATH: "+pXPath+"\nOrigin: "+pNode);
    }

    // Return element node
    return lDOM.getNode();

  }

  /**
   * Seeks the furthest available node in the given path by stepping through each node step and evaluating the step relative
   * to the last node found (starting with pFromNode). If any part of the path does not exist, it is appended to pPathNeedsCreating.
   * @param pPathToReduce XPath to step through.
   * @param pFromNode Starting node.
   * @param pContextUElem For label evaluation.
   * @param pPathNeedsCreating Supply an empty buffer for appending to in the method.
   * @return Furthest node found.
   * @throws ExTooMany If more than one node matches a node step.
   * @throws ExBadPath If XPath evaluation fails.
   */
  protected Node seekFurthestNodeInPath(StringBuffer pPathToReduce, Node pFromNode, ContextUElem pContextUElem, StringBuffer pPathNeedsCreating)
  throws ExTooMany, ExBadPath {

    Node lFurthestNode = pFromNode;

    SEEK_LOOP: while(pPathToReduce.length() != 0) {
      try {
        Node lMatchedNode = xpath1Element(lFurthestNode, pPathToReduce.toString(), pContextUElem);
        lFurthestNode = lMatchedNode;
        break SEEK_LOOP;
      }
      catch (ExTooFew tf) {}

      String lCurSegName = XFUtil.pathPopTail(pPathToReduce);
      if(pPathNeedsCreating.length()==0) {
        pPathNeedsCreating.append(lCurSegName);
      }
      else {
        pPathNeedsCreating.insert(0, lCurSegName.concat("/"));
      }
    }

    return lFurthestNode;
  }

  /**
   * See {@link DOM#getAbsolutePathForCreateableXPath}.
   */
  @Override
  public String getAbsolutePathForCreateableXPath(Node pNode, String pXPath, ContextUElem pContextUElem)
  throws ExTooMany, ExBadPath, ExDOMName {

    StringBuffer lPathToReduce = new StringBuffer(pXPath);
    StringBuffer lPathNeedsCreating = new StringBuffer();

    //Find the furthest node along the path which exists. This will allow any predicates to run for existing nodes.
    Node lCurrentNode = seekFurthestNodeInPath(lPathToReduce, pNode, pContextUElem, lPathNeedsCreating);

    //Get the absolute path for the node we know exists
    StringBuilder lAbsolutePath = new StringBuilder(new DOM(lCurrentNode).absolute());

    //Now loop through the nodes which don't exist in the path, check they have valid names and append the names to the final path
    while(lPathNeedsCreating.length() != 0) {
      String lNodeName = XFUtil.pathPopHead(lPathNeedsCreating, true);

      if(lNodeName.startsWith("@") || lNodeName.startsWith("attribute::")) {
        lNodeName = XFUtil.replace("@", "", lNodeName);
        lNodeName = XFUtil.replace("attribute::", "", lNodeName);
      }

      if(!DOM.isValidXMLName(lNodeName)){
        throw new ExDOMName("No valid absolute path can be generated for path " + pXPath + " as '" + lNodeName + "' is not a valid XML node name.");
      }

      lAbsolutePath.append("/" + lNodeName);
    }

    return lAbsolutePath.toString();
  }

  /**
   * Gets all child Elements for pNode, wrapped as a DOMList.
   * @param pNode The target Node.
   * @return DOMList of all pNodes' child Elements.
   */
  public DOMList getChildElements(Node pNode) {

    Elements lElements = ((Element) pNode).getChildElements();
    DOMList lDOMList = new DOMList();
    for(int i=0; i < lElements.size(); i++){
      lDOMList.add(new DOM(lElements.get(i)));
    }
    return lDOMList;
  }

  /**
   * Gets all child Nodes for pNode, wrapped as a DOMList.
   * @param pNode The target Node.
   * @return DOMList of all pNodes' child Nodes.
   */
  public DOMList getChildNodes(Node pNode) {

    DOMList lDOMList = new DOMList();
    for(int i=0; i < pNode.getChildCount(); i++){
      lDOMList.add(new DOM(pNode.getChild(i)));
    }

    return lDOMList;
  }


  /**
   * Returns true if the element contains an attribute with the name of the value passed
   */
  public boolean hasAttribute(Node pNode, String pAttrName) {
    return getAttributeInternal((Element) pNode, pAttrName) != null;
  }

  /**
   * Returns true if the element contains any nodes
   */
  public boolean hasChildNodes(Node pNode) {
    return (pNode.getChildCount() > 0);
  }

  /**
   * Elem's hashcode is now the underlying DOM element's hashcode value.
   */
  public int hashCode(Node pNode) {
    return pNode.hashCode();
  }

  /** assignAllRefs() dones nothing for Read Only Actuator (required for xpath merge contexts) */
  public void assignAllRefs(Node pNode)
  throws ExInternal
  {
  }

  /** Compare to DOM Trees contents (not passed element names) to see if one is a subset of the other.
   * Note that in this implementation DOM B is a subset of DOM A when:
   *      [1] the same element nodes are identified in the same order at each level
   *          (additional nodes in DOM-A are allowed before, between, and after matched nodes)
   *  and [2] the local concatinated text content of matched elements (including passed elements)
   *          is identical (with whitepsace trimmed if pTrimWhitespace is true) or matching white space.
   */
  public final boolean contentEqualsOrSuperSetOf(
    Node pSupSetNode
  , Node pSubSetNode
  , boolean pTrimWhitespace
  )
  throws ExInternal
  {

    // First compare local text content (must equal to be subset)
    String lSupText = value(pSupSetNode, false);
    String lSubText = value(pSubSetNode, false);

    if(pTrimWhitespace){
      lSupText = lSupText.trim();
      lSubText = lSubText.trim();
    }

    if(!lSupText.equals(lSubText) && (!XFUtil.isWhiteSpace(lSupText) || !XFUtil.isWhiteSpace(lSubText))){
      return false;
    }

    // Extract subset elements
    DOMList lSubChilds = getChildElements(pSubSetNode);
    int lSubChildsLen = lSubChilds.getLength();

    // Shortcut empty sub child elements (regardless of number of sup children)
    if(lSubChildsLen==0) {
      return true;
    }

    // Extract superset elements
    DOMList lSupChilds = getChildElements(pSupSetNode);
    int lSupChildsLen = lSupChilds.getLength();

    // This loop changes start position for compare tests
    Node lSup, lSub;
    int lOffsetStart, lSupIndex, lSubIndex;
    int lOffsetEnd = lSupChildsLen - lSubChildsLen + 1;
    OFFSET_LOOP: for(lOffsetStart=0; lOffsetStart < lOffsetEnd; lOffsetStart++) {

      lSupIndex = lOffsetStart;

      // Loop through subset children
      SUB_LOOP: for(lSubIndex=0; lSubIndex < lSubChildsLen; lSubIndex++) {
        lSub = lSubChilds.item(lSubIndex).getNode();

        // Loop through subset children
        SUP_LOOP: while(lSupIndex < lSupChildsLen) {
          lSup = lSupChilds.item(lSupIndex).getNode();
          lSupIndex++;

          // When super/sub-elements match advance to next sub-node test
          if(DOM.getFullNameSafe(lSub).equals(DOM.getFullNameSafe(lSup)) && contentEqualsOrSuperSetOf(lSup, lSub, pTrimWhitespace)) {
            continue SUB_LOOP;
          }

        } // end SUP_LOOP

        // When here, not all sub-nodes are matched so try next offset
        continue OFFSET_LOOP;

      } // end SUB_LOOP

      // When here, all sub-nodes have been matched to successful match
      return true;

    } // end OFFSET_LOOP

    // When here, each offset match try failed so return false
    return false;

  }

  public final String getURIForNamespacePrefix(Node pNode, String pPrefix){
    if(pNode instanceof Element){
      return ((Element) pNode).getNamespaceURI(pPrefix);
    } else {
      return pNode.getDocument().getRootElement().getNamespaceURI(pPrefix);
    }
  }

  /**
   * Generate a String List of all the text nodes of pNode, in document order.
   * @param pNode The target node.
   * @param pDeep If true, recurse through child nodes adding their text nodes too.
   * @return A List of Strings representing text nodes.
   */
  @Override
  public final List<String> childTextNodesAsStringList(Node pNode, boolean pDeep){
    List<String> lList = new ArrayList<String>();
    for(int i=0; i < pNode.getChildCount(); i++){
      if(pNode.getChild(i) instanceof Text){
        lList.add(((Text)pNode.getChild(i)).getValue());
      }
      else if (pDeep) {
        lList.addAll(childTextNodesAsStringList(pNode.getChild(i), true));
      }
    }
    return lList;
  }

  /**
   * Generate a DOMList of all the text nodes of pNode, in document order.
   * @param pNode The target node.
   * @param pDeep If true, recurse through child nodes adding their text nodes too.
   * @return A DOMList of text nodes.
   */
  @Override
  public final DOMList childTextNodesAsDOMList(Node pNode, boolean pDeep){
    DOMList lList = new DOMList();
    for(int i=0; i < pNode.getChildCount(); i++){
      if(pNode.getChild(i) instanceof Text){
        //Wrap text nodes in DOMs
        lList.add(new DOM(pNode.getChild(i)));
      }
      else if (pDeep) {
        //Recurse through non-text children
        lList.addAll(childTextNodesAsDOMList(pNode.getChild(i), true));
      }
    }
    return lList;
  }


  /**
   * Extension to the XOM serializer which is capable of serialising a single target node (and a node's contents)
   * in addition to a whole document.
   */
  private static class XOMNodeSerializer
  extends Serializer {

    private final boolean mWriteXMLDeclaration;
    private final boolean mPrettyPrint;

    public XOMNodeSerializer(OutputStream pOut, boolean pPrettyPrint, boolean pWriteXMLDeclaration) {
      super(pOut);
      mWriteXMLDeclaration = pWriteXMLDeclaration;
      mPrettyPrint = pPrettyPrint;
      if(mPrettyPrint) {
        setIndent(2);
      }

      setLineSeparator("\n");
    }

    private void startSerialiser() throws IOException {
      if(mWriteXMLDeclaration) {
        writeXMLDeclaration();
      }
    }

    public void serializeNode(Node pNode) {
      try {
        startSerialiser();
        writeChild(pNode);
        flush();
      }
      catch (IOException e) {
        throw new ExInternal("XML serialisation failed", e);
      }
    }

    public void serializeNodeContents(Node pNode) {
      try {
        startSerialiser();
        for(int i=0; i < pNode.getChildCount(); i++) {
          writeChild(pNode.getChild(i));
          //Place line breaks between elements
          if(mPrettyPrint && (pNode instanceof Element || pNode instanceof Comment || pNode instanceof ProcessingInstruction || pNode instanceof DocType) && i < pNode.getChildCount() - 1) {
            breakLine();
          }
        }
        flush();
      }
      catch (IOException e) {
        throw new ExInternal("XML serialisation failed", e);
      }
    }
  }

  @Override
  public void outputCanonicalDocument(Document pNode, OutputStream pOutputStream) {
    Canonicalizer lCanonicalizer = new Canonicalizer(pOutputStream, true);
    try {
      lCanonicalizer.write(pNode);
    }
    catch (IOException e) {
      throw new ExInternal("Failed to serialize XML document", e);
    }
  }


  @Override
  public void outputDocument(Document pNode, OutputStream pOutputStream, boolean pPrettyPrint) {
    //Use the standard XOM Serializer to serialize document nodes because we don't need any special features
    try {
      Serializer lSerializer = new Serializer(pOutputStream);

      if(pPrettyPrint) {
        lSerializer.setIndent(2);
      }

      lSerializer.setLineSeparator("\n");
      lSerializer.write(pNode);
    }
    catch (IOException e) {
      throw new ExInternal("Failed to serialize XML document", e);
    }
  }

  @Override
  public void outputNode(Node pNode, OutputStream pOutputStream, boolean pPrettyPrint, boolean pWriteXMLDeclaration) {
    XOMNodeSerializer lSerializer = new XOMNodeSerializer(pOutputStream, pPrettyPrint, pWriteXMLDeclaration);
    lSerializer.serializeNode(pNode);
  }

  @Override
  public void outputNodeContents(Node pNode, OutputStream pOutputStream, boolean pPrettyPrint, boolean pWriteXMLDeclaration) {
    XOMNodeSerializer lSerializer = new XOMNodeSerializer(pOutputStream, pPrettyPrint, pWriteXMLDeclaration);
    lSerializer.serializeNodeContents(pNode);
  }

  /**
   * Checks that pNode is still attached to a document. If it is not this indicates the node has been removed from its
   * original tree, or was never attached to one.
   * @param pNode
   * @return True if still attached, false if not.
   */
  @Override
  public boolean isAttached(Node pNode){
    return pNode.getDocument() != null;
  }

  /**
   * Perform schema validation using Xerces.
   * @param pNode Node to validate.
   * @param pSchemaDOM XSD schema.
   * @throws ExValidation If validation fails.
   */
  @Override
  public void validateAgainstSchema(Node pNode, DOM pSchemaDOM)
  throws ExValidation {

    //Copy the source and schema documents sideways so any unwanted FOXIDs can be removed.
    DOM lDataDOM = new DOM(pNode);
    lDataDOM = lDataDOM.createDocument();
    lDataDOM.removeRefsRecursive();

    DOM lSchemaDOM = pSchemaDOM.getRootElement().createDocument();
    lSchemaDOM.removeRefsRecursive();

    //Convert the XOM Documents to Xerces W3C DOM documents
    org.w3c.dom.Document lConvertedDataDOM = DOMConverter.convert(lDataDOM.getNode().getDocument(), DOMImplementationImpl.getDOMImplementation());
    org.w3c.dom.Document lConvertedSchemaDOM = DOMConverter.convert(lSchemaDOM.getNode().getDocument(), DOMImplementationImpl.getDOMImplementation());

    Validator lValidator;
    DOMSource lDataDOMSource;
    DOMSource lSchemaDOMSource;

    try {
      //Create a new XSD schema factory - reference the JDK's internal version's constructor directly.
      //Otherwise it's random due to classpath loading orders and some versions don't work properly.
      SchemaFactory lSchemaFactory = new com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory();
      lDataDOMSource = new DOMSource(lConvertedDataDOM);
      lSchemaDOMSource = new DOMSource(lConvertedSchemaDOM);
      //Parse the XSD
      Schema lSchema = lSchemaFactory.newSchema(lSchemaDOMSource);
      //Create a new validator from the XSD
      lValidator = lSchema.newValidator();
    }
    catch (SAXException ex) {
      throw new ExInternal("Failed to initialise DOM validator", ex);
    }

    try {
      //Perform the validation.
      //Note: problems have been observed using older versions of Xerces to build the DOMs and validate them.
      //If this method causes problems, check the classpath is free from any Xerces libraries - the internal Java
      //implementation is the correct version to use. (This should be mitigated above with the direct constructor)
      lValidator.validate(lDataDOMSource);
    }
    catch (SAXException ex) {
      throw new ExValidation("DOM failed XSD validation", ex);
    }
    catch (IOException ex) {
      throw new ExInternal("DOM could not be validated", ex);
    }
  }

  @Override
  public org.w3c.dom.Document convertToW3CDocument(Node pNode) {
    return DOMConverter.convert(pNode.getDocument(), DOMImplementationImpl.getDOMImplementation());
  }

}
