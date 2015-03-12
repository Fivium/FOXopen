package net.foxopen.fox.plugin.api.dom;

import java.io.OutputStream;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Map;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DocControl;
import net.foxopen.fox.dom.NamespaceAttributeTable;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.ex.ExValidation;

import net.sf.saxon.om.NodeInfo;

import nu.xom.Node;

import org.w3c.dom.Document;


public interface FxpDOM<D extends FxpDOM> {
  /**
   * Get the underlying XOM node being wrapped by this FxpDOM object.<br/><br/>
   * INTERNAL USE ONLY - if you need to interface with XOM, you should add extra methods to DOM/Actuators.
   * @return The XOM node.
   */
  Node getNode();

  /**
   * Creates a new Document with a deep copy of this node as its root. Also creates a new DocControl for the new Document.
   * Works for Element and Document nodes. Warning: this method does not reassign internal FOXIDs.
   * @return A a reference to the root element deep clone of this DOM.
   */
  FxpDOM createDocument();

  /**
   * Create a new Document with a copy of this DOM's wrapped XOM Node as its root. Works for Element and Document nodes.
   * Warning: this method does not reassign internal FOXIDs.
   * @param pDeepCopy Specifies whether the copy should be deep (true) or shallow (false). A shallow copy will still copy attributes.
   * @return A clone of this DOM.
   */
  FxpDOM createDocument(boolean pDeepCopy);

  /**
   * Get the unique reference (i.e. FOXID) of this element. If the element does not have a FOXID, an exception is thrown.
   * @return The unique reference string.
   */
  String getRef();

  /**
   * Get a unique, path-like reference for this element within its FxpDOM tree, using FOXIDs to provide the uniqueness.
   * If a node in the path does not have a FOXID, an exception is thrown.
   * I.e. foxid1/foxid2/foxid3... etc
   * @return A unique path reference for this element.
   */
  String getPerfectRef();

  /**
   * Outputs the Document associated with this node to an XML string and writes it to the given Writer. An OutputStream
   * is preferred when writing XML as the XML serialiser can control exactly which bytes are written as characters and
   * which should be escaped.
   * @param pWriter Writer for serialization destination.
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   */
  void outputDocumentToWriter(Writer pWriter, boolean pPrettyPrint);

  /**
   * Recursively serialises this node to an OutputStream. An OutputStream is preferred when writing XML as the XML serialiser
   * can control exactly which bytes are written as characters and which should be escaped.
   *
   * @param pOutputStream Destination for the serialisation.
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   * @param pWriteXMLDeclaration If true, the XML declaration is written to the top of the output.
   */
  void outputNodeToOutputStream(OutputStream pOutputStream, boolean pPrettyPrint, boolean pWriteXMLDeclaration);

  /**
   * Recursively serialises this node's contents to an OutputStream. An OutputStream is preferred when writing XML as the XML serialiser
   * can control exactly which bytes are written as characters and which should be escaped.
   *
   * @param pOutputStream Destination for the serialisation.
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   * @param pWriteXMLDeclaration If true, the XML declaration is written to the top of the output.
   */
  void outputNodeContentsToOutputStream(OutputStream pOutputStream, boolean pPrettyPrint, boolean pWriteXMLDeclaration);

  /**
   * Outputs this node's document to an output stream. An OutputStream is preferred when writing XML as the XML serialiser
   * can control exactly which bytes are written as characters and which should be escaped. This method always adds an XML
   * declaration to the top of the document. If this is not desired, use {@link #outputNodeToOutputStream}.
   * <br/><br>
   *
   * Note: this will output the node's entire document, regardless of whether the current node is a document or not.
   *
   * @param pOutputStream Destination for the serialisation.
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   */
  void outputDocumentToOutputStream(OutputStream pOutputStream, boolean pPrettyPrint);

  /**
   * Outputs this node's document to Canonical XML (with comments). No pretty-printing is applied and no XML declaration
   * is added.
   * @see <a href="http://www.w3.org/TR/2001/REC-xml-c14n-20010315">W3C spec</a>
   * @param pOutputStream Destination for the serialisation.
   */
  void outputCanonicalDocumentToOutputStream(OutputStream pOutputStream);

  /**
   * Outputs this node's document to Canonical XML (with comments). No pretty-printing is applied and no XML declaration
   * is added. Do not use this for large DOMs as the whole DOM string must be stored in memory.
   * @see <a href="http://www.w3.org/TR/2001/REC-xml-c14n-20010315">W3C spec</a>
   * @return String containing the XML in canonical form.
   */
  String outputCanonicalDocumentToString();

  /**
   * Outputs this node's document to String. Note that outputting to an OutputStream is preferred when writing XML as
   * the XML serialiser can control exactly which bytes are written as characters and which should be escaped. This method
   * always adds an XML declaration to the top of the document. If this is not desired, use {@link #outputNodeToString}.
   * Do not use this method for large DOMs as the whole DOM string must be stored in memory.
   * <br/><br>
   *
   * Note: this will output the node's entire document, regardless of whether the current node is a document or not.
   *
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   * @return Document serialised to an XML String.
   */
  String outputDocumentToString(boolean pPrettyPrint);

  /**
   * Outputs this node to String. Note that outputting to an OutputStream is preferred when writing XML as
   * the XML serialiser can control exactly which bytes are written as characters and which should be escaped.
   * Do not use this method for large DOMs as the whole DOM string must be stored in memory. The output string will not
   * have an XML declaration.
   *
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   * @return Document serialised to an XML String.
   */
  String outputNodeToString(boolean pPrettyPrint);

  /**
   * Outputs this node to String. Note that outputting to an OutputStream is preferred when writing XML as
   * the XML serialiser can control exactly which bytes are written as characters and which should be escaped.
   * Do not use this method for large DOMs as the whole DOM string must be stored in memory.
   *
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   * @param pWriteXMLDeclaration If true, the XML declaration is written to the top of the output.
   * @return Document serialised to an XML String.
   */
  String outputNodeToString(boolean pPrettyPrint, boolean pWriteXMLDeclaration);

  /**
   * Recursively serializes this node's contents to a String. Note that the node itself is not serialised so the result
   * of this method may not be a well-formed XML string.
   *
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   * @return The node's contents as a String.
   */
  String outputNodeContentsToString(boolean pPrettyPrint);

  /**
   * Recursively serializes this node's contents to a String. Note that the node itself is not serialised so the result
   * of this method may not be a well-formed XML string.
   *
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   * @param pWriteXMLDeclaration If true, the XML declaration is written to the top of the output.
   * @return The node's contents as a String.
   */
  String outputNodeContentsToString(boolean pPrettyPrint, boolean pWriteXMLDeclaration);

  /**
   * Gets the shallow string value of this node.
   * For Elements or Documents, gets the value of all the text nodes of the node concatenated together in document order.
   * For attributes or text nodes, returns the string value of the node.
   * @return The text value of this node (shallow).
   */
  String value();

  /**
   * Gets the string value of this node.
   * For element or documents, gets the value of all the text nodes of this node concatenated together in document order.
   * For attributes or text nodes, returns the string value of the node.
   * @param pDeep If true, text nodes from all children of this node are retrieved recursively. If false, just examines
   * the text nodes at this level (only applies to Element nodes).
   * @return The text value of this node.
   */
  String value(boolean pDeep);

  /**
   * Gets the shallow value of this node, processed to remove whitespace added by a pretty-printer.
   * See {@link ActuateReadOnly#valueWhitespaceIntelligent(Node)}.
   * @return Shallow node value without dross whitespace.
   */
  String valueWhitespaceIntelligent();

  /**
   * Evaluates the given XPath using this node as the initial context node, and returns the result as a DOMList.
   * This flavour supports an additional ContextUElem which can be supplied if the XPath contains :{contexts}, but the
   * preferred way of executing such XPaths is to use the XPath methods on {@link ContextUElem}.
   * @param pXPath The XPath to evaluate.
   * @param pContextUElem Optional.
   * @return The XPath result expressed as a FxpDOM list.
   * @throws ExBadPath If the XPath is syntactically incorrect.
   * @see net.foxopen.fox.dom.xpath.XPathResult#asDOMList How XPath result types are converted to a DOMList
   */
//  DOMList xpathUL(String pXPath, ContextUElem pContextUElem) throws ExBadPath;
    FxpDOMList xpathUL(String pXPathString) throws ExBadPath;

  /**
   * Evaluates the given XPath using this node as the initial context node, and returns the result as a Boolean.
   * Only use this method when XPath execution is required and a ContextUElem is not available.
   * @param pXPath The XPath to evaluate.
   * @return The XPath result expressed as a Boolean.
   * @throws ExBadPath If the XPath is syntactically incorrect.
   * @see net.foxopen.fox.dom.xpath.XPathResult#asBoolean How XPath result types are converted to a Boolean
   */
  boolean xpathBoolean(String pXPath) throws ExBadPath;

  /**
   * Evaluates the given XPath using this node as the initial context node, and returns a single FxpDOM node.
   * Only use this method when XPath execution is required and a ContextUElem is not available.
   * @param pXPath The XPath to evaluate.
   * @return The node resolved by the XPath.
   * @throws ExTooFew If nothing matches the XPath.
   * @throws ExTooMany If too many nodes match the XPath.
   * @throws ExBadPath If the XPath is syntactically invalid.
   */
  FxpDOM xpath1E(String pXPath) throws ExTooFew, ExTooMany, ExBadPath;

  /**
   * Evaluates the given XPath using this node as the initial context node, and returns a String.
   * Only use this method when XPath execution is required and a ContextUElem is not available.
   * @param pXPath The XPath to evaluate.
   * @return The String result of the XPath.
   * @throws ExTooFew If nothing matches the XPath.
   * @throws ExTooMany If too many nodes match the XPath.
   * @throws ExBadPath If the XPath is syntactically invalid.
   * @see net.foxopen.fox.dom.xpath.XPathResult#asString How XPath result types are converted to a String
   */
  String xpath1S(String pXPath) throws ExTooFew, ExTooMany, ExBadPath;

  /**
   * Evaluates the given XPath using this node as the initial context node, and returns a String.
   * Exceptions are suppressed. In the event of a FOX exception occurring, the empty string is returned.
   * Only use this method when XPath execution is required and a ContextUElem is not available.
   * @param pXPath The XPath to evaluate.
   * @return The String result of the XPath, or an empty String.
   * @see net.foxopen.fox.dom.xpath.XPathResult#asString How XPath result types are converted to a String
   */
  String xpath1SNoEx(String pXPath);

  /**
   * Adds a new child element to the current element.
   * These addElem's can be appended like this:
   * UElem doc = UDoc.create("LEVEL1").addElem("LEVEL2").addElem("LEVEL3");
   * @param pName The name of the new child element.
   * @return A reference to the newly-created FxpDOM element.
   */
  FxpDOM addElem(String pName);

  /**
   * Adds a new child Element (in a namespace) to the current Element.
   * @param pName The fully-qualified name of the new child element.
   * @param pNamespaceURI The namespace URI of the new child element.
   * @return A reference to the newly-created FxpDOM element.
   */
  FxpDOM addElemWithNamespace(String pName, String pNamespaceURI);

  /**
   * Adds a child element to this element and sets its text content.
   * @param pName The name of the new child element.
   * @param pTextContent The text content of the new child element.
   * @return A reference to the newly-created child element.
   */
  FxpDOM addElem(String pName, String pTextContent);

  /**
   * Add a comment node to this node.
   * @param pCommentText The text of the comment.
   * @return Self-reference for method chaining.
   */
  FxpDOM addComment(String pCommentText);

  /**
   * Adds a processing instruction node to this node.
   * @param pTarget The target (i.e. name) of the processing instruction.
   * I.e. for <?xsl-stylesheet .. ?> the name is "xsl-stylesheet".
   * @param pData The text contents of the processing instruction.
   * @return Self-reference for method chaining.
   */
  FxpDOM addPI(String pTarget, String pData);

  /**
   * Set the text of this attribute or element. All existing text content is removed, even if pTextContent is null.
   * If pTextContent is not null, a new text node is created and inserted as the first child node of the element.
   * @param pTextContent The new text content.
   * @return Self-reference for method chaining.
   */
  FxpDOM setText(String pTextContent);

  /**
   * Sets the contents of this node to text or child XML. If the argument is an XML string, it is parsed into XML
   * and appended as node content. If it is not an XML string or an invalid XML string, it is set as the text content
   * of the node.
   * @param pTextContent Text String or XML String.
   * @return Self-reference for method chaining.
   */
  FxpDOM setXMLOrText(String pTextContent);

  /**
   * Gets the namespace URI of this FxpDOM node.
   */
  String getNamespaceURI();

  /**
   * Get a single element by executing a simple path with this node as the context node.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath}for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Element resolved by the path.
   * @throws ExTooFew If no Elements are returned.
   * @throws ExTooMany If more than one Element is returned.
   */
  FxpDOM get1E(String pSimplePath) throws ExTooFew, ExTooMany;

  /**
   * Get a single element by executing a simple path with this node as the context node. If no nodes are matched, null is
   * returned. If too many nodes are matched an exception is thrown.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath}for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Element resolved by the path, or null if none matched.
   * @throws ExDOM If too many nodes are found.
   */
  FxpDOM get1EOrNull(String pSimplePath);

  /**
   * Get the shallow string value of an Element. pSimplePath is evaluated with this node as the context node, and the
   * shallow value of the matched Element is returned.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath}for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @throws ExTooFew If no Elements are returned.
   * @throws ExTooMany If more than one Element is returned.
   * @return The Element resolved by the path, or null if none matched.
   */
  String get1S(String pSimplePath) throws ExTooFew, ExTooMany;

  /**
   * Get the shallow string value of an Element, with exceptions suppressed.
   * pSimplePath is evaluated with this node as the context node, and the shallow value of the matched Element is returned.
   * If the path matches no elements or fails for any other reason, an empty string is returned.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath}for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @throws ExTooFew If no Elements are returned.
   * @throws ExTooMany If more than one Element is returned.
   * @return The Element resolved by the path, or null if none matched.
   */
  String get1SNoEx(String pSimplePath);

  /**
   * Gets 0 or more Elements by execting a simple path with this node as the context node.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath}for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Elements resolved by the path, as a list.
   */
  FxpDOMList getUL(String pSimplePath);

  /**
   * Get the next sibling of this node, in document order. Returns null if the node has no following siblings.<br/>
   * XPath equivalent: <code>./following-sibling::*[1]</code> or <code>./following-sibling::node()[1]</code>, depending
   * on the value of pElementsOnly.
   * @param pElementsOnly Only return elements.
   * @return pNode's next sibling, or null.
   * @see ActuateReadOnly#getNextSiblingOrNull(Node,boolean)
   */
  FxpDOM getNextSiblingOrNull(boolean pElementsOnly);

  /**
   * Get this node's parent Element. If the node does not have a parent, or it is a root element, null is returned.
   * @return The parent node or null.
   */
  FxpDOM getParentOrNull();

  /**
   * Get this node's parent Element. If the node does not have a parent, or it is a root element, a self-reference is
   * returned.
   * @return The parent node or this node..
   */
  FxpDOM getParentOrSelf();

  /**
   * Get this node's previous sibling in document order.
   * @param pElementsOnly If true, only considers Elements. If false, all nodes are considered.
   * @return The previous sibling.
   * @see ActuateReadOnly#getPreviousSiblingOrNull(Node,boolean)
   */
  FxpDOM getPreviousSiblingOrNull(boolean pElementsOnly);

  /**
   * Gets an element by execting a simple path with this node as the context node, creating nodes along the path that do
   * not exist. See {@link net.foxopen.fox.dom.xpath.FoxSimplePath}for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Element resolved or created by the path.
   * @throws ExInternal If more than one Element matches the path.
   */
  FxpDOM getCreate1ENoCardinalityEx(String pSimplePath) throws ExInternal;

  /**
   * Gets an element by execting a simple path with this node as the context node, creating nodes along the path that do
   * not exist. See {@link net.foxopen.fox.dom.xpath.FoxSimplePath}for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Element resolved or created by the path.
   * @throws ExTooMany If more than one Element matches the path.
   */
  FxpDOM getCreate1E(String pSimplePath) throws ExTooMany;

  /**
   * Creates an element by executing a simple path with this node as the context node. Intermediate nodes along the path
   * are also created. See {@link net.foxopen.fox.dom.xpath.FoxSimplePath}for the specification of a simple path. Note the
   * "." (self node) step does not cause an element to be created; instead the existing node is returned.
   * @param pSimplePath The path to evaluate.
   * @return The Element created by the path.
   * @throws ExTooMany If more than one Element is matched at any step in the path.
   */
  FxpDOM create1E(String pSimplePath) throws ExTooMany;

  /**
   * Gets the absolute path for a potentially non-existent node which would be resolved by calling
   * {@link #getCreateXpath1E(String,ContextUElem)}. This works by using the same logic as the create method to walk the
   * given path, but instead of creating nodes it builds up a logical absolute path. This means that an XPath containing
   * axes, predicates etc can be reduced to a simple absolute path, so long as the axes/predicate steps match a single node
   * (following the exact same rules as for create). Essentially, a node created by calling getCreateXpath1E with the same
   * arguments will have the path determined here. If the path steps match too many nodes, or a valid name cannot be
   * established for non-existent nodes, an exception is thrown.
   * @param pXPath XPath to process (note: must be a valid XPath for creating an element).
   * @param pContextUElem Optional ContextUElem for resolving context labels.
   * @return Absolute path to the node resolved by the XPath, which may not actually exist.
   * @throws ExTooMany If path walking matches more than one node in a step.
   * @throws ExBadPath If the XPath is invalid or node names cannot be established.
   */
//  String getAbsolutePathForCreateableXPath(String pXPath, ContextUElem pContextUElem) throws ExTooMany, ExBadPath, ExDOMName;

  /**
   * Gets an element by execting an XPath with this node as the context node, creating nodes along the path that do
   * not exist. See {@link ActuateReadWrite#getCreateXpath1Element(Node,String)}for details on how the XPath is executed.
   * @param pXPath The XPath to evaluate.
   * @param pContextUElem Optional ContextUElem for resolving context labels.
   * @return The Element resolved or created by the path.
   * @throws ExTooMany If more than one Element matches the path.
   * @throws ExBadPath If the XPath is invalid.
   */
//  FxpDOM getCreateXpath1E(String pXPath, ContextUElem pContextUElem) throws ExTooMany, ExBadPath;
//  FxpDOM getCreateXpath1E(String pXPath, String pDummyStringPNREMOVE) throws ExTooMany, ExBadPath;

  /**
   * Gets an element by executing an XPath with this node as the context node, creating nodes along the path that do
   * not exist. See {@link ActuateReadWrite#getCreateXpath1Element(Node,String)}for details on how the XPath is executed.
   * @param pXPath The XPath to evaluate.
   * @return The Element resolved or created by the path.
   * @throws ExTooMany If more than one Element matches the path.
   * @throws ExBadPath If the XPath is invalid.
   */
  FxpDOM getCreateXpath1E(String pXPath) throws ExTooMany, ExBadPath;

  /**
   * Gets 0 or more elements by executing an XPath with this node as the context node, creating nodes along the path that do
   * not exist. See {@link ActuateReadWrite#getCreateXpath1Element(Node,String)}for details on how the XPath is executed.
   * @param pXPath The XPath to evaluate.
   * @return A list of all nodes which match the XPath or were created by it if none matched.
   * @throws ExBadPath If the XPath is invalid.
   */
  FxpDOMList getCreateXPathUL(String pXPath) throws ExBadPath;

  /**
   * Gets 0 or more elements by executing an XPath with this node as the context node, creating nodes along the path that do
   * not exist. See {@link ActuateReadWrite#getCreateXpath1Element(Node,String)}for details on how the XPath is executed.
   * @param pXPath The XPath to evaluate.
   * @param pContextUElem Optional ContextUElem for resolving context labels.
   * @return A list of all nodes which match the XPath or were created by it if none matched.
   * @throws ExBadPath If the XPath is invalid.
   */
//  FxpDOMList getCreateXPathUL(String pXPath, ContextUElem pContextUElem) throws ExBadPath;
//    FxpDOMList getCreateXPathUL(String pXPath, String pDummyStringPNREMOVE) throws ExBadPath;

  /**
   * Remove all the child Nodes from this Element.
   * @return Self-reference for method chaining.
   */
  FxpDOM removeAllChildren();

  /**
   * Sets the value of an attribute on this Element. The name may include a namespace prefix as long as the prefix
   * is declared on or above this node.
   * @param pName The name of the attribute.
   * @param pValue The value to set.
   * @return Self-reference for method chaining.
   */
  FxpDOM setAttr(String pName, String pValue);

  /**
   * Returns the fully-qualified name of this node, or an empty string if it is not an Attribute or Element.
   */
  String getName();

  /**
   * Gets the local name of this node, if it is an Element or Attribute node. All other node types return empty string.
   * E.g. the local name of "fm:action" is "action".
   * @return The nodes' local name.
   */
  String getLocalName();

  /**
   * Get the String value of the given attribute. If the attribute is not defined, an empty string is returned.
   * @param pName Name of the attribute to retrieve. This may include a namespace prefix.
   * @return The attribute's value.
   */
  String getAttr(String pName);

  /**
   * Gets the 0-based index of this node within its parent.
   * @return This node's index.
   */
  int getSiblingIndex();

  /**
   * Get the String value of the given attribute. If the attribute is not defined, null is returned.
   * @param pName Name of the attribute to retrieve.
   * @return The attribute's value, or null.
   */
  String getAttrOrNull(String pName);

  /**
   * Generates the absolute path to this FxpDOM node. For instance given the following XML:
   * <pre>
   * {@code
   * <LEVEL_1>
   * <LEVEL_2>
   * <LEVEL_3/>
   * </LEVEL_2>
   * </LEVEL_1>}</pre>
   * The absolute path to LEVEL_3 would be <code>/LEVEL_1/LEVEL_2/LEVEL_3</code>.
   * If this FxpDOM is a non-Element node (i.e. Attribute, Text) then the absolute path is expressed to the nearest element.<br/>
   * Element names are fully-qualified so may contain namespace prefixes.
   * @return The absolute path to this node.
   */
  String absolute();

  /**
   * Recursively assigns FOXID attributes to all elements in this node's Document.
   */
  void assignAllRefs();

  /**
   * Gets a simple path representation of the path to pNestedDOM from this node.
   * If pNestedDOM is not a child of this node, this method returns null.
   * @param pNestedDOM The target node to seek.
   * @return String representation of path from pNestedDOM to this node.
   */
  String getRelativeDownToOrNull(D pNestedDOM);

  /**
   * Tests if this Element has the given attribute.
   * @param pAttrName Name of the attribute to test for.
   * @return True if the attribute exists on this element, false otherwise.
   */
  boolean hasAttr(String pAttrName);

  /**
   * Tests if this node contains any child nodes.
   * @return True if the node has children, false otherwise.
   */
  boolean hasContent();

  /**
   * Tests if this FxpDOM represents an Element node.
   * @return True if this FxpDOM is an Element.
   */
  boolean isElement();

  /**
   * Tests if this FxpDOM represents an Attribute node.
   * @return True if this FxpDOM is an Attribute.
   */
  boolean isAttribute();

  /**
   * Tests if this FxpDOM represents a Text node.
   * @return True if this FxpDOM is a Text node.
   */
  boolean isText();

  /**
   * Tests if this FxpDOM represents a Comment node.
   * @return True if this FxpDOM is a Comment.
   */
  boolean isComment();

  /**
   * Tests if this FxpDOM represents a ProcessingInstruction node.
   * @return True if this FxpDOM is a Processing Instruction.
   */
  boolean isProcessingInstruction();

  /**
   * Tests if this FxpDOM is 'simple', i.e. does not contain any child elements.
   * @return True if simple.
   */
  boolean isSimpleElement();

  /**
   * Gets the vendor-neutral NodeType of this node.
   * @return The NodeType of this node.
   */
  DOM.NodeType nodeType();

  /**
   * Builds an ArrayList containing the fully qualified attribute names for Attributes defined on this Element.
   * @return An ArrayList of attribute names.
   */
  //TODO change to List<String>
  ArrayList<String> getAttrNames();

  /**
   * Get the namespace URI of the attribute with pAttrName.
   * @param pAttrName Attribute name.
   * @return This attribute's namespace URI, or empty string if it is not in a namespace.
   */
  String getAttributeNamespaceURI(String pAttrName);

  /**
   * Gets a Map of attribute names to attribute values for this Element.
   * @return A Map of attribute names to attribute values.
   */
  Map<String, String> getAttributeMap();

  /**
   * Get a Map of attribute names to attribute values for this Element.
   * @param pNamespaceURI optional, filters the attributes to a single namespace, null means all namespaces
   * @return A Map of attribute names to attribute values.
   */
  Map<String, String> getAttributeMap(String pNamespaceURI);

  /**
   * Get a Map of attribute names to attribute values for this Element.
   * @param pNamespaceURI Optional. Filters the attributes to a single namespace, null means all namespaces.
   * @param pLocalNames Return local names only (remove namespace prefix) - should be  used carefully if not
   * filtering by namespace URI, as there could be name clashes, in which case the value returned will be arbitrary.
   * @return A Map of attribute names to attribute values.
   */
  Map<String, String> getAttributeMap(String pNamespaceURI, boolean pLocalNames) throws ExInternal;

  NamespaceAttributeTable getNamespaceAttributeTable();

  /**
   * Removes the given Attribute from this Element. If no attribute of this name exists, then no action is taken.
   * @param pAttrName The name of the attribute.
   * @return Self-reference for method chaining.
   */
  FxpDOM removeAttr(String pAttrName);

  /**
   * Gets a DOMList containing all the child nodes of this node. If the node has no children, an empty list is returned.
   * @return A DOMList of all child nodes.
   */
  FxpDOMList getChildNodes();

  /**
   * Get the last child element of this node. This is equivelant to the XPath: <code>./*[last()]</code>.
   * @return The last child element of this node.
   * @throws ExTooFew If the node has no children.
   */
  FxpDOM getLastChildElem() throws ExTooFew;

  /**
   * Gets a DOMList containing all the direct child elements of this node.
   * If the node has no child elements, an empty list is returned.
   * @return A DOMList of all child elements.
   */
  FxpDOMList getChildElements();

  /**
   * Recursively gets all the nested elements of this node. This is equivelant to the XPath: <code>.//*</code>.
   * @return List of all nested Elements.
   */
  FxpDOMList getAllNestedElements();

  /**
   * Gets the root element of this node's document.
   * @return The root element.
   */
  FxpDOM getRootElement();

  /**
   * Moves the complete contents of this node to a new parent.
   * @param pNewParent The destination of this node's child nodes.
   */
  void moveContentsTo(D pNewParent);

  /**
   * Copies the complete contents of this node to the specified parent. If the copied nodes have FOXIDs, they are
   * reassigned to maintain uniqueness.
   * @param pNewParent The destination of the copy of this node's child nodes.
   */
  void copyContentsTo(D pNewParent);

  /**
   * Copies the complete contents of this node to the specified parent. This variant does NOT assign new FOXIDs to the cloned
   * nodes and should only be used by internal code when it can be asserted that this behaviour will not cause problems.
   * @param pNewParent The destination of the copy of this node's child nodes.
   */
  void copyContentsToPreserveFoxIDs(D pNewParent);

  /**
   * Recursively copies this node and its complete contents to the specified new parent.
   * @param pNewParent The destination of the cloned node.
   * @return A reference to the copied node, under its new parent.
   */
  FxpDOM copyToParent(D pNewParent);

  /**
   * Recursively copies this node and its complete contents to the specified new parent. This variant does NOT assign
   * new FOXIDs to the cloned nodes and should only be used by internal code when it can be asserted that this behaviour
   * will not cause problems.
   * @param pNewParent The destination of the cloned node.
   * @return A reference to the copied node, under its new parent.
   */
  FxpDOM copyToParentInternalUseOnly(D pNewParent);

  /**
   * Recursively moves this node and its complete contents to the specified new parent. If the new parent already has
   * children, this node will become the last child of the parent node.
   * @param pNewParent The desired new parent of this node.
   */
  void moveToParent(D pNewParent);

  /**
   * Recursively moves this node and its complete contents to a certain child position within a new parent.
   * @param pTargetParent The desired new parent of this node.
   * @param pPositionBeforeTargetParentsChild The location within the parent to insert this node.
   * The node is inserted before this position.
   */
  void moveToParentBefore(D pTargetParent, D pPositionBeforeTargetParentsChild);

  /**
   * Detaches this element from its parent, effectively removing it from the FxpDOM tree. Attempting operations (such as
   * XPath evaluation) on unattached nodes can cause errors.
   */
  void remove();

  /**
   * Recursively removes FOXID attributes from this Element and all its children.
   * @return Self-reference for method chaining.
   */
  FxpDOM removeRefsRecursive();

  /**
   * Renames an Element.
   * @param pNewName Fully-qualified new element name. If this contains a prefix the prefix must be defined in the current
   * document.
   */
  void rename(String pNewName);

  /**
   * Creates a clone of this node for a new document. The cloned node (and its children, if applicable) are indexed by
   * the DocControl associated with pRelatedDOM.
   * @param pDeep If true, recursively clone the subtree under the specified node; if false, clone only the node itself
   * and its attributes.
   * @param pRelatedDOM An element from the FxpDOM tree which the newly cloned element will be added to.
   */
  FxpDOM clone(boolean pDeep, D pRelatedDOM);

  /**
   * Creates a clone of this node.
   * @param pDeep If true, recursively clone the subtree under the specified node; if false, clone only the node itself
   * and its attributes.
   * @return A reference to the cloned node.
   */
  FxpDOM clone(boolean pDeep);

  /**
   * Replaces this node with the given replacement node. The current node is removed from the document tree so
   * @param pNewReplacementDOM The node to replace the current node.
   * @return Reference to the replacement node.
   */
  FxpDOM replaceThisWith(D pNewReplacementDOM);

  /**
   * Overloaded method to ensure node comparisons are based on the underlying wrapped node (in the case that a node
   * is wrapped by multiple FxpDOM objects).
   * @param pOtherDOM The FxpDOM to compare.
   * @return True if the same node, otherwise false.
   */
  boolean equals(D pOtherDOM);

  /**
   * Checks whether the document is namespace aware.
   * @return True if it namespace aware, false otherwise.
   */
  boolean isDocumentNamespaceAware();

  /**
   * Return the namespace prefix of this node if it is an element otherwise throw an error
   * @return String containing the prefix of the namespace for this element node
   */
  public String getNamespacePrefix();

  /**
   * Uses the current Document's DocControl to resolve an element by its FOXID reference.
   * @param pRefString The FOXID reference to lookup.
   * @return The resolved Element.
   * @throws ExInternal If the Element could be located.
   */
  FxpDOM getElemByRef(String pRefString) throws ExInternal;

  /**
   * Uses the current Document's DocControl to resolve an element by its FOXID reference. If no Element can be found,
   * null is returned.
   * @param pRefString The FOXID reference to lookup.
   * @return The resolved Element, or null.
   */
  FxpDOM getElemByRefOrNull(String pRefString) throws ExInternal;

  /**
   * Get the full URI for the given prefix, as defined by an xmlns: attribute on or above this node, or by an explict
   * namespace definition within the document.
   * @param pPrefix The namespace prefix.
   * @return The URI corresponding to pPrefix, or null if pPrefix is not a defined namespace prefix.
   */
  String getURIForNamespacePrefix(String pPrefix);

  /**
   * Explicitly declare a new namespace definition on this element, mapping the given prefix to the given URI.
   * This is equivelant to declaring an "xmlns" attribute on the element.
   * @param pPrefix The namespace prefix.
   * @param pURI The namespace URI.
   * @return This FxpDOM object.
   */
  FxpDOM addNamespaceDeclaration(String pPrefix, String pURI);

  /**
   * Sets the default namespace for this element. Unprefixed elements at and below this position in the tree will be in
   * the default namespace.
   * @param pURI The default namespace URI.
   * @return This FxpDOM object.
   */
  FxpDOM setDefaultNamespace(String pURI);

  /**
   * Checks to see if a subset tree structure exists in current node.
   * Note that the immediate contents of this node must be a superset of the immediate
   * contents of the pSubSetDOM node (recursively). The top level nodes are not compared,
   * only their contents. Text and nested element content types are checked. Attributes
   * are NOT checked. Sequence is important, but additional nodes may exist inbetween
   * nodes being compared. To test top level nodes in addition to context, simply use
   * condition ( a.getName().equals(b.getName()) && a.contentEqualsOrSuperSet(b) )
   * <br/><br/>
   * This variant trims whitespace from text nodes before comparing them.
   * @param pSubSetDOM
   * @return True if the argument is a subset of this node, false otherwise.
   */
  boolean contentEqualsOrSuperSetOf(D pSubSetDOM) throws ExInternal;

  /**
   * See {@link #contentEqualsOrSuperSetOf(FxpDOM)}.
   * @param pSubSetDOM
   * @param pTrimWhitespace
   * @return True if the argument is a subset of this node, false otherwise.
   * @throws ExInternal
   */
  boolean contentEqualsOrSuperSetOf(D pSubSetDOM, boolean pTrimWhitespace) throws ExInternal;

  /**
   * Gets the current modification counter for this node's Document.
   * @return The current Document modified count.
   */
  int getDocumentModifiedCount();

  /**
   * Marks this node's document as modified. This only needs to be invoked in special cases as the Actuator classes
   * usually make sure this property is maintained correctly.
   */
  void setDocumentModified();

  /**
   * Gets all the text nodes that are children of this node, as a String List.
   * The list is ordered according to the document order of the text nodes.
   * This is functionally equivalent to the "/text()" XPath step.
   * If the node has no text children, an empty list is returned.
   * @return String List of text nodes.
   */
  java.util.List<String> childTextNodesAsStringList();

  /**
   * Gets all the text nodes that are children of this node, as a String List.
   * The list is ordered according to the document order of the text nodes.
   * If pDeep is true, this logic recurses through the FxpDOM tree.
   * This is functionally equivalent to the "/text()" XPath step, or "//text()" if pDeep is true.
   * If the node has no text children, an empty list is returned.
   * @param pDeep If true, gets all text nodes recursively. If false, get only the text nodes which are the children of this node.
   * @return String List of text nodes.
   */
  java.util.List<String> childTextNodesAsStringList(boolean pDeep);

  /**
   * Gets all the text nodes that are children of this node, as a DOMList.
   * The list is ordered according to the document order of the text nodes.
   * If pDeep is true, this logic recurses through the FxpDOM tree.
   * This is functionally equivalent to the "/text()" XPath step, or "//text()" if pDeep is true.
   * If the node has no text children, an empty DOMList is returned.
   * @param pDeep If true, gets all text nodes recursively. If false, get only the text nodes which are the children of this node.
   * @return DOMList of text nodes.
   */
  FxpDOMList childTextNodesAsDOMList(boolean pDeep);

  /**
   * Checks that this node is still attached to a document. If it is not this indicates the node has been removed from its
   * original tree, or was never attached to one.
   * @return True if still attached, false if not.
   */
  boolean isAttached();

  /**
   * Set the Document Type for this Node's Document.
   * @param pRootElementName DocType root element.
   * @param pPublicID DocType public ID.
   * @param pSystemID DocType system ID.
   * @return Self reference.
   */
  FxpDOM setDocType(String pRootElementName, String pPublicID, String pSystemID);

  /**
   * Gets a list of elements which match the path and attribute criteria. This node is used as the context item for
   * evaluating the path. Equivalent XPath expression: <code>./a/b/c[@pAttrName="pAttrValue"]</code>. If no matches are
   * found, an empty list is returned.
   * @param pElementSimplePath Simple path, relative to this node.
   * @param pAttrName The name of the attribute to check.
   * @param pAttrValue The value the attribute should be.
   * @return A list of matched Elements.
   */
  FxpDOMList getElementsByAttrValue(String pElementSimplePath, String pAttrName, String pAttrValue);

  /**
   * Prunes this element's contents using an XPath. Only nodes targeted by XPath and their supporting branches are
   * retained. This node is used as the context node for XPath evaluation.
   * @param pPruneXPath XPath identifying nodes to retain.
   * @return Self-reference for method chaining.
   */
  FxpDOM pruneDocumentXPath(String pPruneXPath);

  /**
   * Wraps this node in a Saxon NodeInfo wrapper for XPath execution.
   * @return The XOM node, wrapped as a Saxon NodeInfo object.
   */
  NodeInfo wrap();

  /**
   * Appends pText as a child text node of this DOM. Unlike setText, this does not remove existing child nodes.
   * @param pText The text content to append.
   * @return This DOM.
   */
  FxpDOM appendText(String pText);

  /**
   * Validate this FxpDOM using the XSD provided by pSchemaDOM. This DOM's node is used to construct a new document, so
   * this node will be treated as the root element for validation purposes, regardless of its position in the document
   * tree.<br><br>
   * Note this method is expensive as it involves converting the documents to W3C Xerces DOMs in order to perform
   * validation. It is not recommended for use on large documents.
   * @param pSchemaDOM An XSD document.
   * @throws ExValidation If the FxpDOM fails XSD schema validation.
   */
  void validateAgainstSchema(D pSchemaDOM) throws ExValidation;
  //<D extends FxpDOM>

  /**
   * Converts this node's containing document into a W3C FxpDOM document. This should only be used for interacting with
   * third party APIs which require a W3C DOM.
   * @return The Document node of the new W3C FxpDOM document.
   */
  Document convertToW3CDocument();

  void setDocumentReadOnly();

  void setDocumentReadWrite();

  void setDocumentReadWriteAutoIds();

  void setDocumentNoAccess();
}
