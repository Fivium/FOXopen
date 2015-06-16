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

$Id$

*/
package net.foxopen.fox.dom;


import com.sun.org.apache.xerces.internal.util.SecurityManager;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDOM;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExDOMParser;
import net.foxopen.fox.ex.ExGeneral;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.ex.ExValidation;
import net.foxopen.fox.plugin.api.dom.FxpDOM;
import net.sf.saxon.om.NodeInfo;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Comment;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.IllegalNameException;
import nu.xom.NamespaceConflictException;
import nu.xom.Node;
import nu.xom.ParsingException;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;
import nu.xom.ValidityException;
import org.apache.commons.io.output.WriterOutputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


/**
 * Wrapper class for a XOM Node. DOM provides convenient shortcuts for navigating and manipulating an XML document.
 * Additionally, it implements the Actuator pattern, allowing DOM access can be controlled on a document level.
 * Consequently, all manipulation of XML from within the FOX engine should be done using the DOM class. <br/><br/>
 *
 * A DOM may wrap any type of node, although usually only Element nodes require wrapping, as they form the core structure
 * of a document. Some methods of this class only work when the object is wrapping a certain node type. Usually an
 * exception will be thrown if an operation is attempted against an invalid node type, so consuming code should assert
 * that the node type is correct before invoking these methods.<br/><br/>
 *
 * When dealing with documents which use namespaces, the DOM class attempts to simplify access by resolving namespace
 * URIs automatically when a namespace prefix is used. For example, calling <code>lDOM.create1E("ns1:element")</code>
 * will resolve the "ns1" prefix to a URI by traversing up through the document and searching for the "ns1" namespace
 * declaration. This means that once a namespace prefix is declared, consuming code does not need to worry about which
 * URI it was declared for. The drawback is that if a document contains multiple URIs mapped to the same prefix then
 * elements may be created in the 'wrong' namespace, but this is unlikely - to prevent it, prefixes should be unique
 * within a document.
 *
 */
public class DOM
implements FxpDOM<DOM> {

  /**
   * Vendor-neutral enumeration for representing common XML node types.
   */
  public static enum NodeType{
    DOCUMENT,
    ELEMENT,
    ATTRIBUTE,
    TEXT,
    COMMENT,
    PROCESSING_INSTRUCTION,
    DOCTYPE;
    //Note: missing Namespace node type

    /**
     * Gets the generic NodeType of a XOM node.
     * @param pNode An arbitrary XOM node.
     * @return The NodeType of pNode.
     */
    public static NodeType getNodeType(Node pNode){
      //Note: order of tests is in likelihood of occurrence
      if (pNode instanceof Element){
        return ELEMENT;
      }
      else if (pNode instanceof Text){
        return TEXT;
      }
      else if (pNode instanceof Attribute){
        return ATTRIBUTE;
      }
      else if (pNode instanceof Comment){
        return COMMENT;
      }
      else if (pNode instanceof Document){
        return DOCUMENT;
      }
      else if (pNode instanceof ProcessingInstruction){
        return PROCESSING_INSTRUCTION;
      }
      else if (pNode instanceof DocType){
        return DOCTYPE;
      }
      else {
        return null;
      }
    }
  }

  /**
   * A stack of XOM Document Builders. Becomes larger as more concurrency is required.
   */
  private static final Stack<Builder> gStandardBuilderPoolStack = new Stack<Builder>();

  /** The XOM Node that this DOM object is wrapping */
  private Node mXOMNode;

  /**
   * Cast the given node to an Element. If it is a Dcoument, the root element is retrieved.
   * @param pNode The target Node.
   * @param pSafe If true, suppresses an exception if pNode is not a Document or Element (returns null instead)
   * @return an Element node, or null.
   */
  static Element nodeAsElement(Node pNode, boolean pSafe) {
    if(pNode instanceof Element){
      return (Element) pNode;
    }
    else if (pNode instanceof Document){
      return ((Document) pNode).getRootElement();
    }
    else if (!pSafe) {
      throw new ExInternal("Node is a " + pNode.getClass().getName() + ", must be an Element or Document");
    }
    return null;
  }

  /**
   * Gets the local name of pNode, or empty String if it is not a named Node type.
   * @param pNode The target node.
   * @return The node's local name.
   */
  static String getLocalNameSafe(Node pNode) {
    NodeType lType = NodeType.getNodeType(pNode);
    switch(lType){
      case ELEMENT: return ((Element) pNode).getLocalName();
      case ATTRIBUTE: return ((Attribute) pNode).getLocalName();
      default: return "";
    }
  }

  /**
   * Gets the fully qualified name of pNode, or empty String if it is not a named Node type.
   * @param pNode Node to get the name of.
   * @return The full name of pNode.
   */
  static String getFullNameSafe(Node pNode) {
    NodeType lType = NodeType.getNodeType(pNode);
    switch(lType){
      case ELEMENT: return ((Element) pNode).getQualifiedName();
      case ATTRIBUTE: return ((Attribute) pNode).getQualifiedName();
      default: return "";
    }
  }

  /**
   * Get a Builder from the relevant pool stack. If no Builders are available, a new Builder is created.
   * @return A XOM Builder.
   */
  private static final Builder getDocumentBuilder() {
    //PN TODO XOM - this could build up a big pool - better way of doing this?
    try {
      // Pop cached Builder and return - no need to synchronize
      return gStandardBuilderPoolStack.pop();
    }
    catch (EmptyStackException x) {
      try{
        //Explicitly ask for the Java built-in SAXParser to mitigate against potentially loading an untested parser
        XMLReader lXercesReader = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");

        // Attempt to turn off external DTD entity features for security
        // Note: XOM overrides the http://xml.org/sax/features/external-general-entities and
        // http://xml.org/sax/features/external-parameter-entities parameters, so the custom entity resolver below
        // is required to prevent external URIs being resolved.
        lXercesReader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        lXercesReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        // Stop external URIs being resolved by overloading the default EntityResolver to always return an empty byte array
        lXercesReader.setEntityResolver(
          new EntityResolver(){
            public InputSource resolveEntity(String pPublicId, String pSystemId) {
              InputSource lInputSource = new InputSource();
              lInputSource.setByteStream(new ByteArrayInputStream(new byte[0]));
              return lInputSource;
            }
          }
        );

        SecurityManager lSecurityManager = new SecurityManager();
        lSecurityManager.setEntityExpansionLimit(1);

        lXercesReader.setProperty("http://apache.org/xml/properties/security-manager", lSecurityManager);

        return new Builder(lXercesReader);
      }
      catch (SAXException e){
        throw new ExInternal("SAX exception encountered when getting DocumentBuilder", e);
      }
    }
  }

  /**
   * Return a DocumentBuilder to its relevant pool stack.
   * @param pDocumentBuilder The DocumentBuilder to return.
   */
  private static final void returnDocumentBuilder(Builder pDocumentBuilder) {
    gStandardBuilderPoolStack.push(pDocumentBuilder);
  }

  /**
   * Create a new DOM which wraps the given XOM Node. The Node may be of any type but typically only Elements need to be
   * wrapped. Some methods on this class will raise errors if executed against non-Element nodes - you may need to assert
   * that a node is of the desired type before calling such a method on it.
   * @param pNode The XOM Node to be wrapped.
   */
  public DOM(Node pNode) {
    mXOMNode = pNode;
  }

  /**
   * Get the local name (i.e. without ns: prefix) for the given node name String.
   * @param pName A fully qualified name.
   * @return The local node name of pName.
   */
  static String getLocalNameFromNodeName(String pName){
    String lLocalName = pName;
    if(pName.indexOf(':') > -1){
      lLocalName = pName.substring(pName.indexOf(':') + 1);
    }
    return lLocalName;
  }

  /**
   * Examines the proposed node name and if it has a prefix, looks up the URI for the prefix from the document associated
   * with pNode.
   * @param pNode A node used to perform the namespace lookup, if required.
   * @param pName The proposed fully-qualified node name.
   * @return The URI for the prefix of pName, or null if there is no prefix or no URI definition.
   */
  static String getNamespaceURIForNodeName(Node pNode, String pName){

    String lPrefix = null;
    if(pName.indexOf(':') > -1){
      lPrefix = pName.substring(0, pName.indexOf(':'));
    }
    //Short-circuit if this element name has no prefix.
    if (lPrefix == null) {
      return null;
    }

    return ((Element) pNode).getNamespaceURI(lPrefix);
  }

  /** Forces class to load and initialise */
  public static boolean init() {
    return true;
  }

  /**
   * Convert pString to have Windows style new lines.
   * Note: JB/CB took decided that XML should have Windows style new lines for now as TOAD seems to store them this way
   * when editing XML Types, see JB
   * @param pString Unix-style newline string.
   * @return Windows-style newline String.
   */
  private static final String toXmlString(String pString) {
    if(pString==null) {
      return null;
    }
    return XFUtil.replace("\n",  "\r\n",  pString);
  }

  /**
   * Convert pString to Java/Unix/Oracle SQL style new line - See Oracle SQL Reference "Well-Formed Strings"
   * @param pString Windows-style newline String.
   * @return Unix-style newline string.
   */
  static final String toJavaString(String pString) {
    if(pString==null) {
      return null;
    }
    return XFUtil.replace("\r\n",  "\n",  pString);
  }

  /**
   * Creates a new DOM from the given input stream.
   * @param pInputStream
   * @param pNamespaceAware
   * @return The DOM wrapper of the new document's root element.
   */
  private static DOM createDocumentInternal(InputStream pInputStream, boolean pNamespaceAware){
    Builder lDocBuilder = getDocumentBuilder();
    Document lDocument = null;
    try {
      lDocument = lDocBuilder.build(pInputStream);
    }
    catch (ValidityException e) {
      throw new ExInternal("Invalid XML file.", e);
    }
    catch (ParsingException e) {
      throw new ExInternal("Error parsing XML file.", e);
    }
    catch (IOException e) {
      throw new ExInternal("Error accessing file.", e);
    }
    catch (NullPointerException e) {
      throw new ExInternal("NullPointerException encountered while creating document", e);
    }
    returnDocumentBuilder(lDocBuilder);
    new DocControl(lDocument, pNamespaceAware);
    return new DOM(lDocument.getRootElement());
  }

  /**
   * Creates a new DOM from the given reader.
   * @param pReader
   * @param pNamespaceAware
   * @return The DOM wrapper of the new document's root element.
   */
  private static DOM createDocumentInternal(Reader pReader, boolean pNamespaceAware){
    Builder lDocBuilder = getDocumentBuilder();
    Document lDocument = null;
    try {
      lDocument = lDocBuilder.build(pReader);
    }
    catch (ValidityException e) {
      throw new ExDOMParser("Invalid XML file.", e);
    }
    catch (ParsingException e) {
      throw new ExDOMParser("Error parsing XML file.", e);
    }
    catch (IOException e) {
      throw new ExInternal("Error accessing file.", e);
    }
    catch (NullPointerException e) {
      throw new ExInternal("NullPointerException encountered while creating document", e);
    }
    returnDocumentBuilder(lDocBuilder);
    new DocControl(lDocument, pNamespaceAware);
    return new DOM(lDocument.getRootElement());
  }

  /**
   * Creates a standard XOM Document from the specified file and wraps its root in a DOM object.
   * Also creates the associated DocControl for the new Document.
   * @param pFile An XML file.
   * @param pNamespaceAware Set to true if the document has arbitrary, non-Fox namespaces defined in it.
   * @return The DOM wrapper of the new document's root element.
   */
  public static DOM createDocument(File pFile, boolean pNamespaceAware) {
    try {
      return createDocumentInternal(new FileInputStream(pFile), pNamespaceAware);
    }
    catch (FileNotFoundException e) {
      throw new ExInternal("File not found for createDocument. File path: " + pFile.getAbsolutePath(), e);
    }
    catch (ExInternal e){
      throw new ExInternal("ExInternal caught for file " + pFile.getAbsolutePath() + " during createDocument; see nested.", e);
    }
  }

  /**
   * Creates a standard XOM Document from the specified InputStream and wraps its root in a DOM object.
   * Also creates the associated DocControl for the new Document.
   * @param pInputStream An XML InputStream.
   * @param pNamespaceAware Set to true if the document has arbitrary, non-Fox namespaces defined in it.
   * @return The DOM wrapper of the new document's root element.
   */
  public static DOM createDocument(InputStream pInputStream, boolean pNamespaceAware) {
    return createDocumentInternal(pInputStream, pNamespaceAware);
  }

  /**
   * Creates a standard XOM Document from the specified Reader and wraps its root in a DOM object.
   * Also creates the associated DocControl for the new Document.
   * @param pReader A Reader for an XML document.
   * @return The DOM wrapper for the new XOM document.
   */
  public static DOM createDocument(Reader pReader) {
    return createDocumentInternal(pReader, false);
  }

  /**
   * Creates a standard XOM Document from the specified Reader and wraps its root in a DOM object.
   * Also creates the associated DocControl for the new Document.
   * @param pReader A Reader for an XML document.
   * @param pNamespaceAware Set to true if the document has arbitrary, non-Fox namespaces defined in it.
   * @return  The DOM wrapper of the new document's root element.
   */
  public static DOM createDocument(Reader pReader, boolean pNamespaceAware) {
    return createDocumentInternal(pReader, pNamespaceAware);
  }

  /**
   * Create a new DOM based on the String source.
   * @param pSourceXML String representation of an XML document.
   * @return The DOM wrapper of the new document's root element.
   */
  public static DOM createDocumentFromXMLString(String pSourceXML) {
    return DOM.createDocument(new StringReader(pSourceXML), false);
  }

  /**
   * Create a new DOM based on the String source.
   * @param pSourceXML String representation of an XML document.
   * @param pNamespaceAware Set to true if the document has arbitrary, non-Fox namespaces defined in it.
   * @return The DOM wrapper of the new document's root element.
   */
  public static DOM createDocumentFromXMLString(String pSourceXML, boolean pNamespaceAware) {
    return DOM.createDocument(new StringReader(pSourceXML), pNamespaceAware);
  }

  /**
   * Create a new document with a single root element of the given name.
   * @param pRootElementName The root element name.
   * @param pNamespaceAware Set to true if the document has arbitrary, non-Fox namespaces defined in it.
   * @return DOM containing the root element of the new document.
   */
  public static DOM createDocument(String pRootElementName, boolean pNamespaceAware){
    // Initialise document and root element
    Element lRootElement = new Element(pRootElementName);
    Document lNewDocument = new Document(lRootElement);

    // Create associated DocControl
    new DocControl(lNewDocument, pNamespaceAware);
    return new DOM(lRootElement);
  }

  /**
   * Create a new document with a single root element of the given name.
   * @param pRootElementName The root element name.
   * @param pRootElementNamespaceURI The root element's namespace URI.
   * @param pNamespaceAware Set to true if the document has arbitrary, non-Fox namespaces defined in it.
   * @return DOM containing the root element of the new document.
   */
  public static DOM createDocument(String pRootElementName, String pRootElementNamespaceURI, boolean pNamespaceAware){
    // Initialise document and root element
    Element lRootElement = new Element(pRootElementName, pRootElementNamespaceURI);
    Document lNewDocument = new Document(lRootElement);

    // Create associated DocControl
    new DocControl(lNewDocument, pNamespaceAware);
    return new DOM(lRootElement);
  }

  /**
   * Create a new document with a single root element of the given name.
   * @param pRootElementName The root element name.
   * @return the new DOM.
   */
  static public DOM createDocument(String pRootElementName) {
    return createDocument(pRootElementName, false);
  }

  /**
   * Convenience method to create a shallow clone of a node. If pNode is an Element, its Attributes are also cloned.
   * @param pNode The node to be cloned.
   * @return A clone of pNode including its attributes if applicable.
   */
  static Node shallowCloneIncludingAttrs(Node pNode){

    // Previous Xerces implementation cloned attributes even on a shallow copy, XOM does not, so we do it ourselves.
    // Also XOM only provides deep copy functionality so we need to do it all ourselves.
    if(pNode instanceof Element){
      Element lNodeAsElement = (Element) pNode;
      Element lNewElement;
      if(!"".equals(lNodeAsElement.getNamespaceURI())){
        //Create new element in a namespace
        lNewElement = new Element(lNodeAsElement.getQualifiedName(), lNodeAsElement.getNamespaceURI());
      }
      else {
        //Create new element in no namespace
        lNewElement = new Element(lNodeAsElement.getLocalName());
      }
      //Copy the attributes - sadly XOM doesn't give us a way of doing this in a one-r
      for(int i=0; i<lNodeAsElement.getAttributeCount(); i++){
        Attribute lAttr = lNodeAsElement.getAttribute(i);
        lNewElement.addAttribute((Attribute) lAttr.copy());
      }
      return lNewElement;
    }
    else if (pNode instanceof Comment){
      return ((Comment) pNode).copy();
    }
    else if (pNode instanceof ProcessingInstruction){
      return ((ProcessingInstruction) pNode).copy();
    }
    else {
      throw new ExInternal("Failed to clone pNode as it was an unsupported type: " + pNode.getClass().getName());
    }
  }

  /**
   * Convenience method for getting the wrapped Document or Element Node as an Element.
   * @return the Element, if this is an Element wrapper, or the root Element, if this is a Document wrapper
   */
  private Element getNodeAsElement() {
    return nodeAsElement(mXOMNode, false);
  }

  /**
   * Get the underlying XOM node being wrapped by this DOM object.<br/><br/>
   * INTERNAL USE ONLY - if you need to interface with XOM, you should add extra methods to DOM/Actuators.
   * @return The XOM node.
   */
  public Node getNode() {
    return mXOMNode;
  }

  /**
   * Creates a new DOM from a XOM document which has been constructed externally.
   * @param pNode XOM Document node.
   * @return New DOM for given document.
   */
  public static DOM createDocumentFromNode(Document pNode) {
    new DocControl(pNode, false);
    return new DOM(pNode.getRootElement());
  }

  /**
   * Creates a new Document with a deep copy of this node as its root. Also creates a new DocControl for the new Document.
   * Works for Element and Document nodes. Warning: this method does not reassign internal FOXIDs.
   * @return A a reference to the root element deep clone of this DOM.
   */
  public DOM createDocument() {

    Document lNewDocument;
    if(mXOMNode instanceof Element){
      Element lNewElement = (Element) mXOMNode.copy();
      //Create a new Document with the copied Element as its root.
      lNewDocument = new Document(lNewElement);
    }
    else if (mXOMNode instanceof Document){
      lNewDocument = new Document((Document) mXOMNode);
    }
    else {
      throw new ExInternal("Unexpected: mXOMNode is not an Element or Document");
    }
    new DocControl(lNewDocument, getDocControl().isNamespaceAware());
    return new DOM(lNewDocument.getRootElement());
  }

  /**
   * Create a new Document with a copy of this DOM's wrapped XOM Node as its root. Works for Element and Document nodes.
   * Warning: this method does not reassign internal FOXIDs.
   * @param pDeepCopy Specifies whether the copy should be deep (true) or shallow (false). A shallow copy will still copy attributes.
   * @return A clone of this DOM.
   */
  public DOM createDocument(boolean pDeepCopy) {
    if(pDeepCopy){
      return createDocument();
    }
    else {
      //Create a shallow copy of the Element which includes attributes. Sadly XOM does not provide a built-in method for this.
      Element lNewElement = (Element) shallowCloneIncludingAttrs(getNodeAsElement());

      //Create the new document and return
      Document lNewDocument = new Document(lNewElement);
      new DocControl(lNewDocument, getDocControl().isNamespaceAware());
      return new DOM(lNewElement);
    }
  }

   /**
   * Get the unique reference (i.e. FOXID) of this element. If the element does not have a FOXID, an exception is thrown.
   * @return The unique reference string.
   */
  public String getRef() {
    return DocControl.getDocControl(mXOMNode).mActuate.getRef(mXOMNode);
  }

  /**
   * Get a unique, path-like reference for this element within its DOM tree, using FOXIDs to provide the uniqueness.
   * If a node in the path does not have a FOXID, an exception is thrown.
   * I.e. foxid1/foxid2/foxid3... etc
   * @return A unique path reference for this element.
   */
  public String getPerfectRef(){
    return DocControl.getDocControl(mXOMNode).mActuate.getPerfectRef(mXOMNode);
  }

  /**
   * Convenience method for converting a serialized XML document stored in a BAOS to a String. The bytes should be UTF-8
   * as that is what the XML serializer uses.
   * @param pBAOS BAOS to convert.
   * @return XML String.
   */
  private String byteArrayOutputStreamToString(ByteArrayOutputStream pBAOS) {
    try {
      return pBAOS.toString("UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new ExInternal("Failed to convert XML document to UTF-8", e);
    }
  }

  /**
   * Outputs the Document associated with this node to an XML string and writes it to the given Writer. An OutputStream
   * is preferred when writing XML as the XML serialiser can control exactly which bytes are written as characters and
   * which should be escaped.
   * @param pWriter Writer for serialization destination.
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   */
  public void outputDocumentToWriter(Writer pWriter, boolean pPrettyPrint) {
    outputDocumentToOutputStream(new WriterOutputStream(pWriter, "UTF-8"), pPrettyPrint);
  }

  /**
   * Outputs this node to an XML string and writes it to the given Writer. An OutputStream is preferred when writing XML
   * as the XML serialiser can control exactly which bytes are written as characters and  which should be escaped.
   *
   * @param pWriter Writer for serialization destination.
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   */
  public void outputNodeToWriter(Writer pWriter, boolean pPrettyPrint) {
    DocControl.getDocControl(mXOMNode).mActuate.outputNode(mXOMNode, new WriterOutputStream(pWriter, "UTF-8"), pPrettyPrint, false);
  }

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
  public void outputNodeToOutputStream(OutputStream pOutputStream, boolean pPrettyPrint, boolean pWriteXMLDeclaration) {
    DocControl.getDocControl(mXOMNode).mActuate.outputNode(mXOMNode, pOutputStream, pPrettyPrint, pWriteXMLDeclaration);
  }

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
  public void outputNodeContentsToOutputStream(OutputStream pOutputStream, boolean pPrettyPrint, boolean pWriteXMLDeclaration) {
    DocControl.getDocControl(mXOMNode).mActuate.outputNodeContents(mXOMNode, pOutputStream, pPrettyPrint, pWriteXMLDeclaration);
  }

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
  public void outputDocumentToOutputStream(OutputStream pOutputStream, boolean pPrettyPrint) {
    DocControl.getDocControl(mXOMNode).mActuate.outputDocument(mXOMNode.getDocument(), pOutputStream, pPrettyPrint);
  }

  /**
   * Outputs this node's document to Canonical XML (with comments). No pretty-printing is applied and no XML declaration
   * is added.
   * @see <a href="http://www.w3.org/TR/2001/REC-xml-c14n-20010315">W3C spec</a>
   * @param pOutputStream Destination for the serialisation.
   */
  public void outputCanonicalDocumentToOutputStream(OutputStream pOutputStream) {
    DocControl.getDocControl(mXOMNode).mActuate.outputCanonicalDocument(mXOMNode.getDocument(), pOutputStream);
  }

  /**
   * Outputs this node's document to Canonical XML (with comments). No pretty-printing is applied and no XML declaration
   * is added. Do not use this for large DOMs as the whole DOM string must be stored in memory.
   * @see <a href="http://www.w3.org/TR/2001/REC-xml-c14n-20010315">W3C spec</a>
   * @return String containing the XML in canonical form.
   */
  public String outputCanonicalDocumentToString() {
    ByteArrayOutputStream lBAOS = new ByteArrayOutputStream();
    outputCanonicalDocumentToOutputStream(lBAOS);
    return byteArrayOutputStreamToString(lBAOS);
  }

  /**
   * Recursively serialises this node's Document to a String, pretty-printed. Do not use this for large DOMs as the whole
   * DOM string must be stored in memory.
   * @deprecated You should explicitly define if you require pretty-printed output. Use {@link #outputDocumentToString(boolean)}.
   * @return Document serialised to an XML String.
   */
  @Deprecated
  public String outputDocumentToString() {
    return outputDocumentToString(true);
  }

  /**
   * Outputs this node's document to String. Note that outputting to an OutputStream is preferred when writing XML as
   * the XML serialiser can control exactly which bytes are written as characters and which should be escaped. This method
   * always adds an XML declaration to the top of the document. If this is not desired, use {@link #outputNodeToString(boolean)}.
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
  public String outputDocumentToString(boolean pPrettyPrint) {
    ByteArrayOutputStream lBAOS = new ByteArrayOutputStream();
    outputDocumentToOutputStream(lBAOS, pPrettyPrint);
    return byteArrayOutputStreamToString(lBAOS);
  }

  /**
   * Recursively serializes this node to a String, pretty-printed.
   * @return This node represented as an XML String.
   * @deprecated You should explicitly define if you require pretty-printed output. Use {@link #outputNodeToString(boolean)}.
   */
  @Deprecated
  public String outputNodeToString() {
    return outputNodeToString(true);
  }

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
  public String outputNodeToString(boolean pPrettyPrint) {
    return outputNodeToString(pPrettyPrint, false);
  }

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
  public String outputNodeToString(boolean pPrettyPrint, boolean pWriteXMLDeclaration) {
    ByteArrayOutputStream lBAOS = new ByteArrayOutputStream();
    outputNodeToOutputStream(lBAOS, pPrettyPrint, pWriteXMLDeclaration);
    return byteArrayOutputStreamToString(lBAOS);
  }

  /**
   * Recursively serializes this node's contents to a String. Note that the node itself is not serialised so the result
   * of this method may not be a well-formed XML string.
   *
   * @param pPrettyPrint If true, the XML is pretty-printed. This should only be used when the XML is being output to a
   * location where it needs to be human-readable. Pretty-printing introduces extra whitespace, potentially corrupting a
   * document, so it should be used with care.
   * @return The node's contents as a String.
   */
  public String outputNodeContentsToString(boolean pPrettyPrint) {
    return outputNodeContentsToString(pPrettyPrint, false);
  }

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
  public String outputNodeContentsToString(boolean pPrettyPrint, boolean pWriteXMLDeclaration) {
    ByteArrayOutputStream lBAOS = new ByteArrayOutputStream();
    outputNodeContentsToOutputStream(lBAOS, pPrettyPrint, pWriteXMLDeclaration);
    return byteArrayOutputStreamToString(lBAOS);
  }

  /**
   * Outputs this element and sub-tree as XML to a String without exceptions - for diagnostic logging only.
   * @return String representation of this node, or an error string if an error occurred.
   * @deprecated Catch exceptions yourself externally. TODO PN remove this method
   */
  @Deprecated
  public String outputNodeToStringNoExInternal() {
    String str = "** INTERNAL ERROR **";
    try {
      str = outputNodeToString();
    }
    catch (ExInternal p){
      // No Action
    }
    return str;
  }

  /**
   * Gets the shallow string value of this node.
   * For Elements or Documents, gets the value of all the text nodes of the node concatenated together in document order.
   * For attributes or text nodes, returns the string value of the node.
   * @return The text value of this node (shallow).
   */
  public String value() {
    return toJavaString(DocControl.getDocControl(mXOMNode).mActuate.value(mXOMNode, false));
  }

  /**
   * Gets the string value of this node.
   * For element or documents, gets the value of all the text nodes of this node concatenated together in document order.
   * For attributes or text nodes, returns the string value of the node.
   * @param pDeep If true, text nodes from all children of this node are retrieved recursively. If false, just examines
   * the text nodes at this level (only applies to Element nodes).
   * @return The text value of this node.
   */
  public String value(boolean pDeep) {
    return toJavaString(DocControl.getDocControl(mXOMNode).mActuate.value(mXOMNode, pDeep));
  }

  /**
   * Gets the shallow value of this node, processed to remove whitespace added by a pretty-printer.
   * See {@link ActuateReadOnly#valueWhitespaceIntelligent(Node)}.
   * @return Shallow node value without dross whitespace.
   */
  public String valueWhitespaceIntelligent() {
    // Note: toJavaString done in implementation
    return DocControl.getDocControl(mXOMNode).mActuate.valueWhitespaceIntelligent(mXOMNode);
  }

  /**
   * Evaluates the given XPath using this node as the initial context node, and returns the result as a DOMList.
   * This flavour supports an additional ContextUElem which can be supplied if the XPath contains :{contexts}, but the
   * preferred way of executing such XPaths is to use the XPath methods on {@link ContextUElem}.
   * @param pXPath The XPath to evaluate.
   * @param pContextUElem Optional.
   * @return The XPath result expressed as a DOM list.
   * @throws ExBadPath If the XPath is syntactically incorrect.
   * @see net.foxopen.fox.dom.xpath.XPathResult#asDOMList How XPath result types are converted to a DOMList
   */
  public DOMList xpathUL(String pXPath, ContextUElem pContextUElem)
  throws ExBadPath {
    return DocControl.getDocControl(mXOMNode).mActuate.xpathUL(mXOMNode, toXmlString(pXPath), pContextUElem);
  }

   /**
   * Evaluates the given XPath using this node as the initial context node, and returns the result as a Boolean.
   * Only use this method when XPath execution is required and a ContextUElem is not available.
   * @param pXPath The XPath to evaluate.
   * @return The XPath result expressed as a Boolean.
   * @throws ExBadPath If the XPath is syntactically incorrect.
   * @see net.foxopen.fox.dom.xpath.XPathResult#asBoolean How XPath result types are converted to a Boolean
   */
  public boolean xpathBoolean(String pXPath)
  throws ExBadPath {
    return DocControl.getDocControl(mXOMNode).mActuate.xpathBoolean(mXOMNode, toXmlString(pXPath));
  }

  /**
   * Evaluates the given XPath using this node as the initial context node, and returns a single DOM node.
   * Only use this method when XPath execution is required and a ContextUElem is not available.
   * @param pXPath The XPath to evaluate.
   * @return The node resolved by the XPath.
   * @throws ExTooFew If nothing matches the XPath.
   * @throws ExTooMany If too many nodes match the XPath.
   * @throws ExBadPath If the XPath is syntactically invalid.
   */
  public DOM xpath1E(String pXPath)
  throws ExTooFew, ExTooMany, ExBadPath {
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.xpath1Element(mXOMNode, toXmlString(pXPath)));
  }

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
  public String xpath1S(String pXPath)
  throws ExTooFew, ExTooMany, ExBadPath {
    return toJavaString(DocControl.getDocControl(mXOMNode).mActuate.xpathString(mXOMNode, toXmlString(pXPath)));
  }

  /**
   * Evaluates the given XPath using this node as the initial context node, and returns a String.
   * Exceptions are suppressed. In the event of a FOX exception occurring, the empty string is returned.
   * Only use this method when XPath execution is required and a ContextUElem is not available.
   * @param pXPath The XPath to evaluate.
   * @return The String result of the XPath, or an empty String.
   * @see net.foxopen.fox.dom.xpath.XPathResult#asString How XPath result types are converted to a String
   */
  public String xpath1SNoEx(String pXPath) {
    try {
      return toJavaString(DocControl.getDocControl(mXOMNode).mActuate.xpathString(mXOMNode, toXmlString(pXPath)));
    }
    catch(ExGeneral x){
      return "";
    }
  }

  /**
   * Adds a new child element to the current element.
   * These addElem's can be appended like this:
   * UElem doc = UDoc.create("LEVEL1").addElem("LEVEL2").addElem("LEVEL3");
   * @param pName The name of the new child element.
   * @return A reference to the newly-created DOM element.
   */
  public DOM addElem(String pName){
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.addElement(mXOMNode, pName));
  }

  /**
   * Adds a new child Element (in a namespace) to the current Element.
   * @param pName The fully-qualified name of the new child element.
   * @param pNamespaceURI The namespace URI of the new child element.
   * @return A reference to the newly-created DOM element.
   */
  public DOM addElemWithNamespace(String pName, String pNamespaceURI){
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.addElementWithNamespace(mXOMNode, pName, pNamespaceURI));
  }

  /**
   * Adds a child element to this element and sets its text content.
   * @param pName The name of the new child element.
   * @param pTextContent The text content of the new child element.
   * @return A reference to the newly-created child element.
   */
  public DOM addElem(String pName, String pTextContent) {
    return new DOM(
      DocControl.getDocControl(mXOMNode).mActuate
        .addElement(mXOMNode, pName, toXmlString(pTextContent))
    );
  }

  /**
   * Add a comment node to this node.
   * @param pCommentText The text of the comment.
   * @return Self-reference for method chaining.
   */
  public DOM addComment(String pCommentText) {
    DocControl.getDocControl(mXOMNode).mActuate.addComment(mXOMNode, toXmlString(pCommentText));
    return this;
  }

  /**
   * Adds a processing instruction node to this node.
   * @param pTarget The target (i.e. name) of the processing instruction.
   * I.e. for <?xsl-stylesheet .. ?> the name is "xsl-stylesheet".
   * @param pData The text contents of the processing instruction.
   * @return Self-reference for method chaining.
   */
  public DOM addPI(String pTarget, String pData) {
    DocControl.getDocControl(mXOMNode).mActuate.addPI(mXOMNode, pTarget, pData);
    return this;
  }

  /**
   * Set the text of this attribute or element. All existing text content is removed, even if pTextContent is null.
   * If pTextContent is not null, a new text node is created and inserted as the first child node of the element.
   * @param pTextContent The new text content.
   * @return Self-reference for method chaining.
   */
  public DOM setText(String pTextContent){
    DocControl.getDocControl(mXOMNode).mActuate.setText(mXOMNode, toXmlString(pTextContent));
    return this;
  }

  /**
   * Sets the contents of this node to text or child XML. If the argument is an XML string, it is parsed into XML
   * and appended as node content. If it is not an XML string or an invalid XML string, it is set as the text content
   * of the node.
   * @param pTextContent Text String or XML String.
   * @return Self-reference for method chaining.
   */
  public DOM setXMLOrText(String pTextContent){
    DocControl.getDocControl(mXOMNode).mActuate.setXMLOrText(mXOMNode, toXmlString(pTextContent));
    // Return self
    return this;
  }

  /**
   * Gets the namespace URI of this DOM node.
   */
   public String getNamespaceURI () {
    if(mXOMNode instanceof Element){
      return ((Element) mXOMNode).getNamespaceURI();
    }
    else if(mXOMNode instanceof Attribute){
      return ((Attribute) mXOMNode).getNamespaceURI();
    }
    else if(mXOMNode instanceof Document){
      return ((Document) mXOMNode).getRootElement().getNamespaceURI();
    }
    else {
      throw new ExInternal("Cannot get namespace URI, node is a " + mXOMNode.getClass().getName() + " but needs to be an Element, Attribute or Document.");
    }
  }

  /**
   * Get a single element by executing a simple path with this node as the context node.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath} for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Element resolved by the path.
   * @throws ExTooFew If no Elements are returned.
   * @throws ExTooMany If more than one Element is returned.
   */
  public DOM get1E(String pSimplePath)
  throws ExTooFew, ExTooMany {
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.get1Element(mXOMNode, pSimplePath));
  }

  /**
   * Get a single element by executing a simple path with this node as the context node. See {@link net.foxopen.fox.dom.xpath.FoxSimplePath}
   * for the specification of a simple path. Element's local names are used in the search. Use this method when the elements
   * you are searching for may have an unknown prefix.
   * @param pSimplePath The path to evaluate.
   * @return The Element resolved by the path.
   * @throws ExTooFew If no Elements are returned.
   * @throws ExTooMany If more than one Element is returned.
   */
  public DOM get1EByLocalName(String pSimplePath)
  throws ExTooFew, ExTooMany {
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.get1ElementByLocalName(mXOMNode, pSimplePath));
  }

  /**
   * Get a single element by executing a simple path with this node as the context node. If no nodes are matched, null is
   * returned. If too many nodes are matched an exception is thrown.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath} for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Element resolved by the path, or null if none matched.
   * @throws ExDOM If too many nodes are found.
   */
  public DOM get1EOrNull(String pSimplePath) {
    try {
      return new DOM(DocControl.getDocControl(mXOMNode).mActuate.get1Element(mXOMNode, pSimplePath));
    }
    catch (ExTooMany e) {
      throw new ExDOM("get1EOrNull would have returned ExTooMany for path '" + pSimplePath + "'", e);
    }
    catch (ExTooFew e) {
      return null;
    }
  }

  /**
   * Get a single element by executing a simple path with this node as the context node. If no nodes are matched, null is
   * returned. If too many nodes are matched an exception is thrown. Element's local names are used in the search. Use
   * this method when the elements you are searching for may have an unknown prefix.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath} for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Element resolved by the path, or null if none matched.
   * @throws ExDOM If too many nodes are found.
   */
  public DOM get1EByLocalNameOrNull(String pSimplePath) {
    try {
      return new DOM(DocControl.getDocControl(mXOMNode).mActuate.get1ElementByLocalName(mXOMNode, pSimplePath));
    }
    catch (ExTooMany e) {
      throw new ExDOM("get1EByLocalNameOrNull would have returned ExTooMany for path '" + pSimplePath + "'", e);
    }
    catch (ExTooFew e) {
      return null;
    }
  }

  /**
   * Get the shallow string value of an Element. pSimplePath is evaluated with this node as the context node, and the
   * shallow value of the matched Element is returned.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath} for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @throws ExTooFew If no Elements are returned.
   * @throws ExTooMany If more than one Element is returned.
   * @return The Element resolved by the path, or null if none matched.
   */
  public String get1S(String pSimplePath)
  throws ExTooFew, ExTooMany {
    return toJavaString(DocControl.getDocControl(mXOMNode).mActuate.get1String(mXOMNode, pSimplePath));
  }

  /**
   * Get the shallow string value of an Element, with exceptions suppressed.
   * pSimplePath is evaluated with this node as the context node, and the shallow value of the matched Element is returned.
   * If the path matches no elements or fails for any other reason, an empty string is returned.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath} for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @throws ExTooFew If no Elements are returned.
   * @throws ExTooMany If more than one Element is returned.
   * @return The Element resolved by the path, or empty string if none matched.
   */
  public String get1SNoEx(String pSimplePath) {
    try{
      return toJavaString(DocControl.getDocControl(mXOMNode).mActuate.get1String(mXOMNode, pSimplePath));
    }
    catch(ExGeneral x){
      return "";
    }
  }

  /**
   * Gets 0 or more Elements by execting a simple path with this node as the context node.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath} for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Elements resolved by the path, as a list.
   */
  public DOMList getUL(String pSimplePath) {
    return DocControl.getDocControl(mXOMNode).mActuate.getUL(mXOMNode, pSimplePath);
  }

  /**
   * Gets 0 or more Elements by execting a simple path with this node as the context node. Element's local names are used
   * in the search. Use this method when the elements you are searching for may have an unknown prefix.
   * See {@link net.foxopen.fox.dom.xpath.FoxSimplePath} for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Elements resolved by the path, as a list.
   */
  public DOMList getULByLocalName(String pSimplePath) {
    return DocControl.getDocControl(mXOMNode).mActuate.getULByLocalName(mXOMNode, pSimplePath);
  }

  /**
   * Get the next sibling of this node, in document order. Returns null if the node has no following siblings.<br/>
   * XPath equivalent: <code>./following-sibling::*[1]</code> or <code>./following-sibling::node()[1]</code>, depending
   * on the value of pElementsOnly.
   * @param pElementsOnly Only return elements.
   * @return pNode's next sibling, or null.
   * @see ActuateReadOnly#getNextSiblingOrNull(Node, boolean)
   */
  public final DOM getNextSiblingOrNull(boolean pElementsOnly) {
    Node lBro = DocControl.getDocControl(mXOMNode).mActuate.getNextSiblingOrNull(mXOMNode, pElementsOnly);
    if(lBro == null) {
      return null;
    }
    return new DOM(lBro);
  }

  /**
   * Get this node's parent Element. If the node does not have a parent, or it is a root element, null is returned.
   * @return The parent node or null.
   */
  public final DOM getParentOrNull(){
    Node lParent = DocControl.getDocControl(mXOMNode).mActuate.getParentOrNull(mXOMNode);
    return lParent!=null ? new DOM(lParent) : null;
  }

  /**
   * Get this node's parent Element. If the node does not have a parent, or it is a root element, a self-reference is
   * returned.
   * @return The parent node or this node..
   */
  public final DOM getParentOrSelf() {
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.getParentOrSelf(mXOMNode));
  }

  /**
   * Get this node's previous sibling in document order.
   * @param pElementsOnly If true, only considers Elements. If false, all nodes are considered.
   * @return The previous sibling.
   * @see ActuateReadOnly#getPreviousSiblingOrNull(Node,boolean)
   */
  public final DOM getPreviousSiblingOrNull(boolean pElementsOnly) {

    Node lBro = DocControl.getDocControl(mXOMNode).mActuate.getPreviousSiblingOrNull(mXOMNode, pElementsOnly);
    if(lBro == null) {
      return null;
    }
    return new DOM(lBro);
  }

  /**
   * Gets an element by execting a simple path with this node as the context node, creating nodes along the path that do
   * not exist. See {@link net.foxopen.fox.dom.xpath.FoxSimplePath} for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Element resolved or created by the path.
   * @throws ExInternal If more than one Element matches the path.
   */
  public DOM getCreate1ENoCardinalityEx(String pSimplePath)
  throws ExInternal {
    try {
      return getCreate1E(pSimplePath);
    }
    catch (ExTooMany ex) {
      throw new ExInternal("Too many elements returned for xpath=\""+ pSimplePath+"\" node="+absolute(), ex);
    }
  }

  /**
   * Gets an element by execting a simple path with this node as the context node, creating nodes along the path that do
   * not exist. See {@link net.foxopen.fox.dom.xpath.FoxSimplePath} for the specification of a simple path.
   * @param pSimplePath The path to evaluate.
   * @return The Element resolved or created by the path.
   * @throws ExTooMany If more than one Element matches the path.
   */
  public DOM getCreate1E(String pSimplePath)
  throws ExTooMany {
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.getCreate1Element(mXOMNode, pSimplePath));
  }


  /**
   * Creates an element by executing a simple path with this node as the context node. Intermediate nodes along the path
   * are also created. See {@link net.foxopen.fox.dom.xpath.FoxSimplePath} for the specification of a simple path. Note the
   * "." (self node) step does not cause an element to be created; instead the existing node is returned.
   * @param pSimplePath The path to evaluate.
   * @return The Element created by the path.
   * @throws ExTooMany If more than one Element is matched at any step in the path.
   */
  public DOM create1E(String pSimplePath)
  throws ExTooMany {
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.create1Element(mXOMNode, pSimplePath));
  }

  /**
   * Gets the absolute path for a potentially non-existent node which would be resolved by calling
   * {@link #getCreateXpath1E(String, ContextUElem)}. This works by using the same logic as the create method to walk the
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
  public String getAbsolutePathForCreateableXPath(String pXPath, ContextUElem pContextUElem)
  throws ExTooMany, ExBadPath, ExDOMName {
    return DocControl.getDocControl(mXOMNode).mActuate.getAbsolutePathForCreateableXPath(mXOMNode, toXmlString(pXPath), pContextUElem);
  }

  /**
   * Gets an element by execting an XPath with this node as the context node, creating nodes along the path that do
   * not exist. See {@link ActuateReadWrite#getCreateXpath1Element(Node,String)} for details on how the XPath is executed.
   * @param pXPath The XPath to evaluate.
   * @param pContextUElem Optional ContextUElem for resolving context labels.
   * @return The Element resolved or created by the path.
   * @throws ExTooMany If more than one Element matches the path.
   * @throws ExBadPath If the XPath is invalid.
   */
  public DOM getCreateXpath1E(String pXPath, ContextUElem pContextUElem)
  throws ExTooMany, ExBadPath {
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.getCreateXpath1Element(mXOMNode, toXmlString(pXPath), pContextUElem));
  }

  /**
   * Gets an element by executing an XPath with this node as the context node, creating nodes along the path that do
   * not exist. See {@link ActuateReadWrite#getCreateXpath1Element(Node,String)} for details on how the XPath is executed.
   * @param pXPath The XPath to evaluate.
   * @return The Element resolved or created by the path.
   * @throws ExTooMany If more than one Element matches the path.
   * @throws ExBadPath If the XPath is invalid.
   */
  public DOM getCreateXpath1E(String pXPath)
  throws ExTooMany, ExBadPath {
    return getCreateXpath1E(pXPath, null);
  }

  /**
   * Gets 0 or more elements by executing an XPath with this node as the context node, creating nodes along the path that do
   * not exist. See {@link ActuateReadWrite#getCreateXpath1Element(Node,String)} for details on how the XPath is executed.
   * @param pXPath The XPath to evaluate.
   * @return A list of all nodes which match the XPath or were created by it if none matched.
   * @throws ExBadPath If the XPath is invalid.
   */
  public DOMList getCreateXPathUL(String pXPath)
  throws ExBadPath {
    return DocControl.getDocControl(mXOMNode).mActuate.getCreateXPathUL(mXOMNode, toXmlString(pXPath));
  }

   /**
   * Gets 0 or more elements by executing an XPath with this node as the context node, creating nodes along the path that do
   * not exist. See {@link ActuateReadWrite#getCreateXpath1Element(Node,String)}for details on how the XPath is executed.
   * @param pXPath The XPath to evaluate.
   * @param pContextUElem Optional ContextUElem for resolving context labels.
   * @return A list of all nodes which match the XPath or were created by it if none matched.
   * @throws ExBadPath If the XPath is invalid.
   */
  public DOMList getCreateXPathUL(String pXPath, ContextUElem pContextUElem)
  throws ExBadPath {
    return DocControl.getDocControl(mXOMNode).mActuate.getCreateXPathUL(mXOMNode, toXmlString(pXPath), pContextUElem);
  }

   /**
   * Remove all the child Nodes from this Element.
   * @return Self-reference for method chaining.
   */
  public DOM removeAllChildren() {
    DocControl.getDocControl(mXOMNode).mActuate.removeAllChildren(mXOMNode);
    return this;
  }

  /**
   * Sets the value of an attribute on this Element. The name may include a namespace prefix as long as the prefix
   * is declared on or above this node.
   * @param pName The name of the attribute.
   * @param pValue The value to set.
   * @return Self-reference for method chaining.
   */
  public DOM setAttr(String pName, String pValue) {
    DocControl.getDocControl(mXOMNode).mActuate.setAttribute(mXOMNode, pName, toXmlString(pValue));
    return this;
  }

  /**
   * Returns the fully-qualified name of this node, or an empty string if it is not an Attribute or Element.
   */
  public String getName() {
    return DocControl.getDocControl(mXOMNode).mActuate.getName(mXOMNode);
  }

  /**
   * Gets the local name of this node, if it is an Element or Attribute node. All other node types return empty string.
   * E.g. the local name of "fm:action" is "action".
   * @return The nodes' local name.
   */
  public String getLocalName(){
    return DocControl.getDocControl(mXOMNode).mActuate.getLocalName(mXOMNode);
  }

  /**
   * Get the String value of the given attribute. If the attribute is not defined, an empty string is returned.
   * @param pName Name of the attribute to retrieve. This may include a namespace prefix.
   * @return The attribute's value.
   */
  public String getAttr(String pName){
    return toJavaString(
      DocControl.getDocControl(mXOMNode).mActuate.getAttribute(mXOMNode, pName)
    );
  }

  /**
   * Gets this node's FOXID attribute, or empty string if it doesn't have one.
   * @return
   */
  public String getFoxId() {
    return getAttr(Actuate.FOXID);
  }

  /**
   * Gets the 0-based index of this node within its parent.
   * @return This node's index.
   */
  public int getSiblingIndex() {
    return DocControl.getDocControl(mXOMNode).mActuate.getSiblingIndex(mXOMNode);
  }

  /**
   * Get the String value of the given attribute. If the attribute is not defined, null is returned.
   * @param pName Name of the attribute to retrieve.
   * @return The attribute's value, or null.
   */
  public String getAttrOrNull(String pName) {
    DocControl lDocControl = DocControl.getDocControl(mXOMNode);
    if(lDocControl.mActuate.hasAttribute(mXOMNode, pName)) {
      return toJavaString(lDocControl.mActuate.getAttribute(mXOMNode, pName));
    }
    else {
      return null;
    }
  }

  /**
   * Generates the absolute path to this DOM node. For instance given the following XML:
   * <pre>
   * {@code
   *<LEVEL_1>
   *  <LEVEL_2>
   *    <LEVEL_3/>
   *  </LEVEL_2>
   *</LEVEL_1>}</pre>
   * The absolute path to LEVEL_3 would be <code>/LEVEL_1/LEVEL_2/LEVEL_3</code>.
   * If this DOM is a non-Element node (i.e. Attribute, Text) then the absolute path is expressed to the nearest element.<br/>
   * Element names are fully-qualified so may contain namespace prefixes.
   * @return The absolute path to this node.
   */
  public final String absolute(){
    return DocControl.getDocControl(mXOMNode).mActuate.getAbsolute(mXOMNode);
  }

  /**
   * Recursively assigns FOXID attributes to all elements in this node's Document.
   */
  public final void assignAllRefs() {
    DocControl.getDocControl(mXOMNode).mActuate.assignAllRefs(mXOMNode);
  }

  /**
   * Gets a simple path representation of the path to pNestedDOM from this node.
   * If pNestedDOM is not a child of this node, this method returns null.
   * @param pNestedDOM The target node to seek.
   * @return String representation of path from pNestedDOM to this node.
   */
  public final String getRelativeDownToOrNull(DOM pNestedDOM) {
    return DocControl.getDocControl(mXOMNode).mActuate.getRelativeDownToOrNull(mXOMNode, pNestedDOM.mXOMNode);
  }

  /**
   * Tests if this Element has the given attribute.
   * @param pAttrName Name of the attribute to test for.
   * @return True if the attribute exists on this element, false otherwise.
   */
  public boolean hasAttr(String pAttrName) {
    return DocControl.getDocControl(mXOMNode).mActuate.hasAttribute(mXOMNode, pAttrName);
  }

  /**
   * Tests if this node contains any child nodes.
   * @return True if the node has children, false otherwise.
   */
  public boolean hasContent() {
    return DocControl.getDocControl(mXOMNode).mActuate.hasChildNodes(mXOMNode);
  }

  /**
   * Exposes the underlying node's hashCode. This means different DOM objects which wrap the same node will inherit the
   * same hashCode.
   * @return The wrapped node's hashCode.
   */
  public int hashCode() {
    return mXOMNode.hashCode();
  }

  /**
   * Tests if this DOM represents an Element node.
   * @return True if this DOM is an Element.
   */
  public boolean isElement() {
    return mXOMNode instanceof Element;
  }

  /**
   * Tests if this DOM represents an Attribute node.
   * @return True if this DOM is an Attribute.
   */
  public boolean isAttribute() {
    return mXOMNode instanceof Attribute;
  }

  /**
   * Tests if this DOM represents a Text node.
   * @return True if this DOM is a Text node.
   */
  public boolean isText() {
    return mXOMNode instanceof Text;
  }

  /**
   * Tests if this DOM represents a Comment node.
   * @return True if this DOM is a Comment.
   */
  public boolean isComment() {
    return mXOMNode instanceof Comment;
  }

  /**
   * Tests if this DOM represents a ProcessingInstruction node.
   * @return True if this DOM is a Processing Instruction.
   */
  public boolean isProcessingInstruction() {
    return mXOMNode instanceof ProcessingInstruction;
  }

  /**
   * Tests if this DOM is 'simple', i.e. does not contain any child elements.
   * @return True if simple.
   */
  public boolean isSimpleElement() {
    return getChildElements().size() == 0;
  }

  /**
   * Gets the vendor-neutral NodeType of this node.
   * @return The NodeType of this node.
   */
  public NodeType nodeType() {
    return NodeType.getNodeType(mXOMNode);
  }

  /**
   * Builds an ArrayList containing the fully qualified attribute names for Attributes defined on this Element.
   * @return An ArrayList of attribute names.
   */
  //TODO change to List<String>
  public ArrayList<String> getAttrNames(){
    return DocControl.getDocControl(mXOMNode).mActuate.getAttrNames(mXOMNode);
  }

  /**
   * Get the namespace URI of the attribute with pAttrName.
   * @param pAttrName Attribute name.
   * @return This attribute's namespace URI, or empty string if it is not in a namespace.
   */
  public String getAttributeNamespaceURI(String pAttrName) {
    return DocControl.getDocControl(mXOMNode).mActuate.getAttributeNamespaceURI(mXOMNode, pAttrName);
  }

  /**
   * Gets a Map of attribute names to attribute values for this Element.
   * @return A Map of attribute names to attribute values.
   */
  public final Map<String, String> getAttributeMap() {
    return getAttributeMap(null, false);
  }

  /**
   * Get a Map of attribute names to attribute values for this Element.
   * @param pNamespaceURI optional, filters the attributes to a single namespace, null means all namespaces
   * @return A Map of attribute names to attribute values.
   */
  public final Map<String, String> getAttributeMap(String pNamespaceURI) {
    return getAttributeMap(pNamespaceURI, false);
  }

  /**
   * Get a Map of attribute names to attribute values for this Element.
   * @param pNamespaceURI Optional. Filters the attributes to a single namespace, null means all namespaces.
   * @param pLocalNames Return local names only (remove namespace prefix) - should be  used carefully if not
   * filtering by namespace URI, as there could be name clashes, in which case the value returned will be arbitrary.
   * @return A Map of attribute names to attribute values.
   */
  public final Map<String, String> getAttributeMap(String pNamespaceURI, boolean pLocalNames)
  throws ExInternal {
    Map<String, String> lMap = DocControl.getDocControl(mXOMNode).mActuate.getAttributes(mXOMNode, pNamespaceURI, pLocalNames);

    Iterator i = lMap.entrySet().iterator();
    Map.Entry e;
    while(i.hasNext()) {
      e = (Map.Entry) i.next();
      e.setValue(toJavaString((String) e.getValue()));
    }
    return lMap;
  }

  public final NamespaceAttributeTable getNamespaceAttributeTable() {
    //This method uses actuators to get attributes which will suffice for the access check
    return NamespaceAttributeTable.createFromDOM(this);
  }

  /**
   * Removes the given Attribute from this Element. If no attribute of this name exists, then no action is taken.
   * @param pAttrName The name of the attribute.
   * @return Self-reference for method chaining.
   */
  public DOM removeAttr(String pAttrName) {
    DocControl.getDocControl(mXOMNode).mActuate.removeAttribute(mXOMNode, pAttrName);
    return this;
  }

  /**
   * Gets a DOMList containing all the child nodes of this node (excluding attributes). If the node has no children,
   * an empty list is returned.
   * @return A DOMList of all child nodes.
   */
  public DOMList getChildNodes() {
    return DocControl.getDocControl(mXOMNode).mActuate.getChildNodes(mXOMNode);
  }

  /**
   * Get the last child element of this node. This is equivelant to the XPath: <code>./*[last()]</code>.
   * @return The last child element of this node.
   * @throws ExTooFew If the node has no children.
   */
  public DOM getLastChildElem()
  throws ExTooFew {
    return new DOM(
      DocControl.getDocControl(mXOMNode).mActuate.getLastChildElement(mXOMNode)
    );
  }

  /**
   * Gets a DOMList containing all the direct child elements of this node.
   * If the node has no child elements, an empty list is returned.
   * @return A DOMList of all child elements.
   */
  public DOMList getChildElements() {
    return DocControl.getDocControl(mXOMNode).mActuate.getChildElements(mXOMNode);
  }


  /**
   * Recursively gets all the nested elements of this node. This is equivelant to the XPath: <code>.//*</code>.
   * @return List of all nested Elements.
   */
  public DOMList getAllNestedElements() {
    try {
      return DocControl.getDocControl(mXOMNode).mActuate.xpathUL(mXOMNode, ".//*", null);
    }
    catch(ExBadPath x) {
      throw x.toUnexpected("getAllNestedElements");
    }
  }

  /**
   * Gets the root element of this node's document.
   * @return The root element.
   */
  public final DOM getRootElement() {
    return new DOM(
      DocControl.getDocControl(mXOMNode).mActuate.getRootElement(mXOMNode)
    );
  }

  /**
   * Moves the complete contents of this node to a new parent.
   * @param pNewParent The destination of this node's child nodes.
   */
  public void moveContentsTo(DOM pNewParent) {
    DocControl.getDocControl(mXOMNode).mActuate.moveContentsTo(mXOMNode, pNewParent.mXOMNode);
  }

  /**
   * Copies the complete contents of this node to the specified parent. If the copied nodes have FOXIDs, they are
   * reassigned to maintain uniqueness.
   * @param pNewParent The destination of the copy of this node's child nodes.
   */
  public void copyContentsTo(DOM pNewParent) {
    DocControl.getDocControl(pNewParent.mXOMNode).mActuate.copyContentsTo(mXOMNode, pNewParent.mXOMNode, true);
  }

  /**
   * Copies the complete contents of this node to the specified parent. This variant does NOT assign new FOXIDs to the cloned
   * nodes and should only be used by internal code when it can be asserted that this behaviour will not cause problems.
   * @param pNewParent The destination of the copy of this node's child nodes.
   */
  public void copyContentsToPreserveFoxIDs(DOM pNewParent) {
    DocControl.getDocControl(pNewParent.mXOMNode).mActuate.copyContentsTo(mXOMNode, pNewParent.mXOMNode, false);
  }


  /**
   * Recursively copies this node and its complete contents to the specified new parent.
   * @param pNewParent The destination of the cloned node.
   * @return A reference to the copied node, under its new parent.
   */
  public DOM copyToParent(DOM pNewParent) {
    // Check source dom is not No Access
    if(!(DocControl.getDocControl(mXOMNode).mActuate instanceof ActuateReadOnly)) {
      DocControl.getDocControl(mXOMNode).mActuate.throwAccessViolation(mXOMNode);
    }
    // Process using targets actuator
    return new DOM(
      DocControl.getDocControl(pNewParent.mXOMNode).mActuate.copyToParent(mXOMNode, pNewParent.mXOMNode, true)
    );
  }

  /**
   * Recursively copies this node and its complete contents to the specified new parent. This variant does NOT assign
   * new FOXIDs to the cloned nodes and should only be used by internal code when it can be asserted that this behaviour
   * will not cause problems.
   * @param pNewParent The destination of the cloned node.
   * @return A reference to the copied node, under its new parent.
   */
  public DOM copyToParentInternalUseOnly(DOM pNewParent) {
    // Check source dom is not No Access
    if(!(DocControl.getDocControl(mXOMNode).mActuate instanceof ActuateReadOnly)) {
      DocControl.getDocControl(mXOMNode).mActuate.throwAccessViolation(mXOMNode);
    }
    // Process using targets actuator
    return new DOM(
      DocControl.getDocControl(pNewParent.mXOMNode).mActuate.copyToParent(mXOMNode, pNewParent.mXOMNode, false)
    );
  }

  /**
   * Recursively moves this node and its complete contents to the specified new parent. If the new parent already has
   * children, this node will become the last child of the parent node.
   * @param pNewParent The desired new parent of this node.
   */
  public void moveToParent(DOM pNewParent) {
    // Check source dom is not No Access
    if(!(DocControl.getDocControl(mXOMNode).mActuate instanceof ActuateReadOnly)) {
      DocControl.getDocControl(mXOMNode).mActuate.throwAccessViolation(mXOMNode);
    }
    // Process using targets actuator

    // (its not needed to check for different document as pNewParent already has a DocControl)
    mXOMNode = DocControl.getDocControl(pNewParent.mXOMNode).mActuate.moveToParent(mXOMNode, pNewParent.mXOMNode);
  }

  /**
   * Recursively moves this node and its complete contents to a certain child position within a new parent.
   * @param pTargetParent The desired new parent of this node.
   * @param pPositionBeforeTargetParentsChild The location within the parent to insert this node.
   * The node is inserted before this position.
   */
  public void moveToParentBefore(DOM pTargetParent, DOM pPositionBeforeTargetParentsChild) {
    // Check source dom is not No Access
    if(!(DocControl.getDocControl(mXOMNode).mActuate instanceof ActuateReadOnly)) {
      DocControl.getDocControl(mXOMNode).mActuate.throwAccessViolation(mXOMNode);
    }

    mXOMNode = DocControl.getDocControl(pTargetParent.mXOMNode).mActuate.moveToParentBefore(
      pTargetParent.mXOMNode // pTargetParentNode
    , mXOMNode // pMovingNode
    , pPositionBeforeTargetParentsChild.mXOMNode // pPositionBeforeTargetParentsChildNode
    );
  }

  /**
   * Detaches this element from its parent, effectively removing it from the DOM tree. Attempting operations (such as
   * XPath evaluation) on unattached nodes can cause errors.
   */
  public void remove() {
    DocControl.getDocControl(mXOMNode).mActuate.remove(mXOMNode);
  }

  /**
   * Recursively removes FOXID attributes from this Element and all its children.
   * @return Self-reference for method chaining.
   */
  public DOM removeRefsRecursive() {
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.removeRefsRecursive(mXOMNode));
  }

  /**
   * Renames an Element.
   * @param pNewName Fully-qualified new element name. If this contains a prefix the prefix must be defined in the current
   * document.
   */
  public void rename(String pNewName) {
    DocControl.getDocControl(mXOMNode).mActuate.renameElement(mXOMNode, pNewName);
  }

  /**
   * Creates a completely free element which is not attached to any document or DOM.
   * Use this variant if the element has a namespace prefix which is already known (i.e. "ns1:name").
   * @param pName Name of the new element.
   * @param pNamespaceURI URI of the new element's namespace.
   * @return A new element.
   */
  public static DOM createUnconnectedElement(String pName, String pNamespaceURI) {
    try {
      return new DOM(new Element(pName, pNamespaceURI));
    }
    catch (NamespaceConflictException e){
      throw new ExDOMName("Invalid element name: '" + pName + "'", e);
    }
    catch(IllegalNameException e){
      throw new ExDOMName("Invalid element name '" + pName + "'", e);
    }
  }

  /**
   * Creates a completely free element which is not attached to any document or DOM.
   * Use this variant if the element has no namespace prefix.
   * @param pName Name of the new element.
   * @return A new element.
   */
  public static DOM createUnconnectedElement(String pName) {
    try {
      return new DOM(new Element(pName));
    }
    catch (NamespaceConflictException e){
      throw new ExDOMName("Invalid element name: '" + pName + "'", e);
    }
    catch(IllegalNameException e){
      throw new ExDOMName("Invalid element name '" + pName + "'", e);
    }
  }

   /**
   * Convenience method for creating an element which may or may not be in a namespace. If a namespace prefix is found
   * in pName, pRelatedNode is used to look up the namespace URI for the prefix.
   * @param pName Fully qualified name of the new element (i.e. including prefix if required).
   * @param pRelatedNode Optional node for namespace lookup.
   * @return A new unattached element.
    */
  static Element createElementInternal(String pName, Node pRelatedNode){
    try {
      String lURI = DOM.getNamespaceURIForNodeName(pRelatedNode, pName);
      if(lURI != null){
        //Create new element in a namespace
        return new Element(pName, lURI);
      }
      else {
        //Create new element in no namespace
        return new Element(pName);
      }
    }
    catch(NamespaceConflictException e){
      throw new ExDOMName("Invalid element name '" + pName + "'", e);
    }
    catch(IllegalNameException e){
      throw new ExDOMName("Invalid element name '" + pName + "'", e);
    }
  }

  /**
   * Creates a completely free element which is not attached to any document or DOM.
   * Use this variant if the element has a namespace prefix which is not directly known but is defined on or above pRelatedElement.
   * @param pName Name of the new element.
   * @param pRelatedElement Element to use to retrieve a namespace declaration.
   * @return A new element.
   */
  public static DOM createUnconnectedElement(String pName, DOM pRelatedElement) {
    return new DOM(createElementInternal(pName, pRelatedElement.getNode()));
  }

  /**
   * Creates a completely free text node which is not attached to any document or DOM.
   * @param pText The text the new text node will contain.
   * @return A new text node.
   */
  public static DOM createUnconnectedText(String pText) {
    return new DOM(new Text(pText));
  }

  /**
   * Creates a clone of this node for a new document. The cloned node (and its children, if applicable) are indexed by
   * the DocControl associated with pRelatedDOM.
   * @param pDeep If true, recursively clone the subtree under the specified node; if false, clone only the node itself
   * and its attributes.
   * @param pRelatedDOM An element from the DOM tree which the newly cloned element will be added to.
   */
  public DOM clone(boolean pDeep, DOM pRelatedDOM) {
    // Check source dom is not No Access
    if(!(DocControl.getDocControl(mXOMNode).mActuate instanceof ActuateReadOnly)) {
      DocControl.getDocControl(mXOMNode).mActuate.throwAccessViolation(mXOMNode);
    }
    // Process using targets actuator
    return new DOM(
      DocControl.getDocControl(pRelatedDOM.mXOMNode).mActuate.clone(mXOMNode, pRelatedDOM.mXOMNode, pDeep)
    );
  }

  /**
   * Creates a clone of this node.
   * @param pDeep If true, recursively clone the subtree under the specified node; if false, clone only the node itself
   * and its attributes.
   * @return A reference to the cloned node.
   */
  public DOM clone(boolean pDeep) {
    // Process using targets actuator
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.clone(mXOMNode, mXOMNode, pDeep));
  }

  /**
   * Replaces this node with the given replacement node. The current node is removed from the document tree so
   * @param pNewReplacementDOM The node to replace the current node.
   * @return Reference to the replacement node.
   */
  public DOM replaceThisWith(DOM pNewReplacementDOM) {
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.replaceWith(mXOMNode, pNewReplacementDOM.mXOMNode));
  }

  /**
   * Overloaded method to ensure node comparisons are based on the underlying wrapped node (in the case that a node
   * is wrapped by multiple DOM objects).
   * @param pOtherDOM The DOM to compare.
   * @return True if the same node, otherwise false.
   */
  public boolean equals(DOM pOtherDOM) {
    return mXOMNode.equals(pOtherDOM.mXOMNode);
  }

  /**
   * Gets the DocControl for this node's document.
   * @return The DocControl for this node's document.
   */
  public final DocControl getDocControl() {
    return DocControl.getDocControl(mXOMNode);
  }

  /**
   * Checks whether the document is namespace aware.
   * @return True if it namespace aware, false otherwise.
   */
  public boolean isDocumentNamespaceAware() {
    return this.getDocControl().isNamespaceAware();
  }

  /**
   * Uses the current Document's DocControl to resolve an element by its FOXID reference.
   * @param pRefString The FOXID reference to lookup.
   * @return The resolved Element.
   * @throws ExInternal If the Element could be located.
   */
  public DOM getElemByRef(String pRefString)
  throws ExInternal {
    return new DOM(DocControl.getDocControl(mXOMNode).mActuate.getElemByRef(mXOMNode, pRefString));
  }

  /**
   * Uses the current Document's DocControl to resolve an element by its FOXID reference. If no Element can be found,
   * null is returned.
   * @param pRefString The FOXID reference to lookup.
   * @return The resolved Element, or null.
   */
  public DOM getElemByRefOrNull(String pRefString)
  throws ExInternal
  {
    Node n = DocControl.getDocControl(mXOMNode).mActuate.getElemByRefOrNull(mXOMNode, pRefString);
    if(n == null) {
      return null;
    }
    return new DOM(n);
  }

  /**
   * Get the full URI for the given prefix, as defined by an xmlns: attribute on or above this node, or by an explict
   * namespace definition within the document.
   * @param pPrefix The namespace prefix.
   * @return The URI corresponding to pPrefix, or null if pPrefix is not a defined namespace prefix.
   */
  public final String getURIForNamespacePrefix(String pPrefix){
    return DocControl.getDocControl(mXOMNode).mActuate.getURIForNamespacePrefix(mXOMNode, pPrefix);
  }

  /**
   * Explicitly declare a new namespace definition on this element, mapping the given prefix to the given URI.
   * This is equivelant to declaring an "xmlns" attribute on the element.
   * @param pPrefix The namespace prefix.
   * @param pURI The namespace URI.
   * @return This DOM object.
   */
  public final DOM addNamespaceDeclaration(String pPrefix, String pURI){
    DocControl.getDocControl(mXOMNode).mActuate.addNamespaceDeclaration(mXOMNode, pPrefix, pURI);
    return this;
  }

  /**
   * Sets the default namespace for this element. Unprefixed elements at and below this position in the tree will be in
   * the default namespace.
   * @param pURI The default namespace URI.
   * @return This DOM object.
   */
  public final DOM setDefaultNamespace(String pURI){
    DocControl.getDocControl(mXOMNode).mActuate.setDefaultNamespace(mXOMNode, pURI);
    return this;
  }


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
  public boolean contentEqualsOrSuperSetOf(
    DOM pSubSetDOM
  )
  throws ExInternal
  {

    return
    DocControl.getDocControl(mXOMNode)
    .mActuate
    .contentEqualsOrSuperSetOf(mXOMNode, pSubSetDOM.mXOMNode, true);
  }

  /**
   * See {@link #contentEqualsOrSuperSetOf(DOM)}.
   * @param pSubSetDOM
   * @param pTrimWhitespace
   * @return True if the argument is a subset of this node, false otherwise.
   * @throws ExInternal
   */
  public boolean contentEqualsOrSuperSetOf(
    DOM pSubSetDOM
  , boolean pTrimWhitespace
  )
  throws ExInternal
  {
    return DocControl.getDocControl(mXOMNode).mActuate.contentEqualsOrSuperSetOf(mXOMNode, pSubSetDOM.mXOMNode, pTrimWhitespace);
  }

  /**
   * Gets the current modification counter for this node's Document.
   * @return The current Document modified count.
   */
  public int getDocumentModifiedCount() {
    return DocControl.getDocControl(mXOMNode).getDocumentModifiedCount();
  }

  /**
   * Marks this node's document as modified. This only needs to be invoked in special cases as the Actuator classes
   * usually make sure this property is maintained correctly.
   */
  public void setDocumentModified() {
    DocControl.getDocControl(mXOMNode).setDocumentModifiedCount();
  }

  /**
   * Return the namespace prefix of this node if it is an element otherwise throw an error
   * @return String containing the prefix of the namespace for this element node
   */
  public String getNamespacePrefix() {
    if(mXOMNode instanceof Element){
      return ((Element) mXOMNode).getNamespacePrefix();
    } else {
      throw new ExInternal("Attempted to get a namespace prefix on an element that is not a node");
    }
  }

  /**
   * Gets all the text nodes that are children of this node, as a String List.
   * The list is ordered according to the document order of the text nodes.
   * This is functionally equivalent to the "/text()" XPath step.
   * If the node has no text children, an empty list is returned.
   * @return String List of text nodes.
   */
  public List<String> childTextNodesAsStringList(){
    return DocControl.getDocControl(mXOMNode).mActuate.childTextNodesAsStringList(mXOMNode, false);
  }

   /**
   * Gets all the text nodes that are children of this node, as a String List.
   * The list is ordered according to the document order of the text nodes.
   * If pDeep is true, this logic recurses through the DOM tree.
   * This is functionally equivalent to the "/text()" XPath step, or "//text()" if pDeep is true.
   * If the node has no text children, an empty list is returned.
   * @param pDeep If true, gets all text nodes recursively. If false, get only the text nodes which are the children of this node.
   * @return String List of text nodes.
    */
  public List<String> childTextNodesAsStringList(boolean pDeep){
    return DocControl.getDocControl(mXOMNode).mActuate.childTextNodesAsStringList(mXOMNode, pDeep);
  }

  /**
  * Gets all the text nodes that are children of this node, as a DOMList.
  * The list is ordered according to the document order of the text nodes.
  * If pDeep is true, this logic recurses through the DOM tree.
  * This is functionally equivalent to the "/text()" XPath step, or "//text()" if pDeep is true.
  * If the node has no text children, an empty DOMList is returned.
  * @param pDeep If true, gets all text nodes recursively. If false, get only the text nodes which are the children of this node.
  * @return DOMList of text nodes.
   */
  public DOMList childTextNodesAsDOMList(boolean pDeep){
    return DocControl.getDocControl(mXOMNode).mActuate.childTextNodesAsDOMList(mXOMNode, pDeep);
  }

  /**
   * Checks that this node is still attached to a document. If it is not this indicates the node has been removed from its
   * original tree, or was never attached to one.
   * @return True if still attached, false if not.
   */
  public boolean isAttached(){
    return DocControl.getDocControl(mXOMNode).mActuate.isAttached(mXOMNode);
  }

  /**
   * Set the Document Type for this Node's Document.
   * @param pRootElementName DocType root element.
   * @param pPublicID DocType public ID.
   * @param pSystemID DocType system ID.
   * @return Self reference.
   */
  public DOM setDocType(String pRootElementName, String pPublicID, String pSystemID){
    DocControl.getDocControl(mXOMNode).mActuate.setDocType(mXOMNode, pRootElementName, pPublicID, pSystemID);
    return this;
  }

  /**
   * Determines whether the specified XPath expression is a simple expression, consisting of only slash-seperated (/)
   * valid XML element names, such as /EMPLOYEES/EMPLOYEE or /notes/a-note/note_id.
   * @param pXPathExpr The path expression to test.
   * @return True if the specified XPath expression is simple, false otherwise.
   */
  public static final boolean isSimpleXPath(String pXPathExpr) {
    boolean lIsSimpleXPath = true;
    StringBuffer lXPathBuffer = new StringBuffer(pXPathExpr.trim());

    while (lXPathBuffer.length() > 0 && lIsSimpleXPath) {
      String headPathElement = XFUtil.pathPopHead(lXPathBuffer, true);
      lIsSimpleXPath = isValidXMLName(headPathElement);
    }
    return lIsSimpleXPath;
  }

   /**
    * Determines whether the specified XML Name is valid.
    *
    * <p>An XML name is valid if the following are true:
    * <ol>
    * <li>The initial character is a letter, underscore or colon.
    * <li>Subsequent characters are either alphanumeric, underscore,
    * colon, hyphen or period (full-stop).
    * </ol>
    *
   * @param pXMLName The proposed name to validate.
   * @return True if the specified XML Name is valid, false otherwise.
    */
  public static final boolean isValidXMLName(String pXMLName) {
    boolean lIsValidXMLName = true;

    for (int c=0; c < pXMLName.length() && lIsValidXMLName; c++) {
      char ch = pXMLName.charAt(c);

      lIsValidXMLName = Character.isLetter(ch) || ch == '_' ||ch == ':';
      if (c > 0) { // check other name characters, if not the first character
        lIsValidXMLName = lIsValidXMLName || Character.isDigit(ch) ||ch == '-' ||ch == '.';
      }
    }

    return lIsValidXMLName;
  }

  /**
   * Gets a list of elements which match the path and attribute criteria. This node is used as the context item for
   * evaluating the path. Equivalent XPath expression: <code>./a/b/c[@pAttrName="pAttrValue"]</code>. If no matches are
   * found, an empty list is returned.
   * @param pElementSimplePath Simple path, relative to this node.
   * @param pAttrName The name of the attribute to check.
   * @param pAttrValue The value the attribute should be.
   * @return A list of matched Elements.
   */
  public final DOMList getElementsByAttrValue(String pElementSimplePath, String pAttrName, String pAttrValue) {
    DOMList lResult = getUL(pElementSimplePath);
    int l = lResult.size();
    DOM lNode;
    for(int i=l-1; i>=0; i--) {
      lNode = lResult.get(i);
      if(!toXmlString(pAttrValue).equals(lNode.getAttrOrNull(pAttrName))) {
        lResult.remove(i);
      }
    }
    return lResult;
  }

  /**
   * Overloaded toString implementation which provides the name, type and hashCode of this node.
   * @return String representation of this node.
   */
  public String toString() {
    //Note: avoid using actuator based methods in case DocControl is null
    return DOM.getFullNameSafe(mXOMNode) +"("+ getClass().getName() + ") #" + String.valueOf(hashCode());
  }

  /**
   * Creates an unattached XML fragment which represents the given path. E.g. passing <code>A/B/C</code> will create a
   * DOM tree of the following structure:
   * <pre>
   * {@code
   *<A>
   *  <B>
   *    <C/>
   *  </B>
   *</A>}</pre>
   * @param pSimplePath The path to use to create Elements.
   * @return The root element of the new fragment.
   */
  public static DOM createUnconnectedSimplePath(String pSimplePath) {
    if(XFUtil.isNull(pSimplePath)) {
      throw new ExInternal("createUnconnectedSimplePath(): Null simple path passed");
    }

    // Create new record node when missing
    StringBuffer lNodePath = new StringBuffer(pSimplePath);
    String lNodeName = XFUtil.pathPopHead(lNodePath, false);
    DOM lReturnNode = DOM.createUnconnectedElement(lNodeName);
    DOM lLastNode = lReturnNode;
    while(lNodePath.length() != 0 ) {
      lNodeName = XFUtil.pathPopHead(lNodePath, false);
      lLastNode = lLastNode.addElem(lNodeName);
    }
    return lReturnNode;
  }

  /**
   * Recurisvely removes unimportant whitespace text nodes from the given node.
   * @param pNode The node to remove whitespace from.
   */
  public static void stripElementContentWhiteSpace(DOM pNode) {

    DOM lNode = pNode;

    if(lNode.isText() && lNode.getNextSiblingOrNull(false) != null &&  lNode.getNextSiblingOrNull(false).isElement() && lNode.value().trim().length() == 0){
      DOM lNextNode = lNode.getNextSiblingOrNull(false);
      DOM lCurrentSibling = lNode;
      lNode = lNextNode;
      DOM lPrevSibling;
      do {
        lPrevSibling = lCurrentSibling.getPreviousSiblingOrNull(false);
        lCurrentSibling.remove();
        lCurrentSibling = lPrevSibling;
      }
      while (lPrevSibling != null && lPrevSibling.isText() && lPrevSibling.value().trim().length() == 0);
    }

    if(lNode.isText() && lNode.getPreviousSiblingOrNull(false) != null &&  lNode.getPreviousSiblingOrNull(false).isElement() && lNode.value().trim().length() == 0){
      DOM lNextSibling;
      do {
        lNextSibling = lNode.getNextSiblingOrNull(false);
        lNode.remove();
        lNode = lNextSibling;
      }
      while (lNextSibling != null && lNextSibling.isText() && lNextSibling.value().trim().length() == 0);

      if ( lNode == null ) return;
    }

    DOMList childDOMs = lNode.getChildNodes();
    for (int d = 0; d < childDOMs.getLength(); d++) {
      stripElementContentWhiteSpace(childDOMs.item(d));
    }
  }

  /**
   * Prunes this element's contents using an XPath. Only nodes targeted by XPath and their supporting branches are
   * retained. This node is used as the context node for XPath evaluation.
   * @param pPruneXPath XPath identifying nodes to retain.
   * @return Self-reference for method chaining.
   */
  public final DOM pruneDocumentXPath(String pPruneXPath) {

    // Get list of nodes to retain
    DOMList lDOMList;
    try {
      lDOMList = xpathUL(pPruneXPath, null);
    }
    catch (ExBadPath e) {
      throw new ExInternal("Prune XPath error: "+pPruneXPath, e);
    }

    // Build hash map of node foxids to retain
    Set lRetainSet = new HashSet();
    DOM lRetainDOM;
    while((lRetainDOM=lDOMList.popHead()) != null) {
      lRetainSet.add(lRetainDOM.getRef());
    }

    // Recurse through document pruning nodes
    pruneInternal(lRetainSet);

    return this;
  }

  /**
   * Internal prune recursive implemetation.
   * @param pRetainSet Set of FOXIDs identifying nodes to retain.
   * @return Count of retained nodes.
   */
  private final int pruneInternal(Set pRetainSet) {

    int lKeepCount = 0;

    // Recursively prune child nodes
    DOMList lChildDOMList = getChildElements();
    DOM lChildDOM;
    while((lChildDOM=lChildDOMList.popHead()) != null) {
      lKeepCount += lChildDOM.pruneInternal(pRetainSet);
    }

    // Remove current nodes when node not required or a supporting branch
    if(lKeepCount==0 && !pRetainSet.contains(getRef())) {
      remove();
    }
    else {
      lKeepCount++;
    }
    // Return prune node count
    return lKeepCount;

  }

  /**
   * Wraps this node in a Saxon NodeInfo wrapper for XPath execution.
   * @return The XOM node, wrapped as a Saxon NodeInfo object.
   */
  public NodeInfo wrap(){
    //This method could be expanded to return different wrap types in the future.
    return DocControl.getDocControl(mXOMNode).getOrCreateDocumentWrapper(mXOMNode).wrap(mXOMNode);
  }

  /**
   * Appends pText as a child text node of this DOM. Unlike setText, this does not remove existing child nodes.
   * @param pText The text content to append.
   * @return This DOM.
   */
  public DOM appendText(String pText){
    DocControl.getDocControl(mXOMNode).mActuate.appendText(mXOMNode, pText);
    return this;
  }

  /**
   * Toggles whether this element should be preserving whitespace, using the xml:space attribute. This method only modifies
   * the document if it needs to.
   * @param pPreserveWhitespace True if this element should preserve whitespace in all sub-elements.
   */
  public void setPreserveWhitespace(boolean pPreserveWhitespace) {
    if(pPreserveWhitespace && !"preserve".equals(getAttr("xml:space"))) {
      setAttr("xml:space", "preserve");
    }
    else if(!pPreserveWhitespace && "preserve".equals(getAttr("xml:space"))) {
      removeAttr("xml:space");
    }
  }

  /**
   * Validate this DOM using the XSD provided by pSchemaDOM. This DOM's node is used to construct a new document, so
   * this node will be treated as the root element for validation purposes, regardless of its position in the document
   * tree.<br><br>
   * Note this method is expensive as it involves converting the documents to W3C Xerces DOMs in order to perform
   * validation. It is not recommended for use on large documents.
   * @param pSchemaDOM An XSD document.
   * @throws ExValidation If the DOM fails XSD schema validation.
   */
  public void validateAgainstSchema(DOM pSchemaDOM)
  throws ExValidation {
    DocControl.getDocControl(mXOMNode).mActuate.validateAgainstSchema(mXOMNode, pSchemaDOM);
  }

  /**
   * Converts this node's containing document into a W3C DOM document. This should only be used for interacting with
   * third party APIs which require a W3C DOM.
   * @return The Document node of the new W3C DOM document.
   */
  public org.w3c.dom.Document convertToW3CDocument() {
    return DocControl.getDocControl(mXOMNode).mActuate.convertToW3CDocument(mXOMNode);
  }

  @Override
  public DOMList xpathUL(String pXPathString) throws ExBadPath {
    return xpathUL(pXPathString, null);
  }

  /* Document access methods so plugin API doesn't have to expose DocControl. */

  @Override
  public void setDocumentReadOnly() {
    getDocControl().setDocumentReadOnly();
  }

  @Override
  public void setDocumentReadWrite() {
    getDocControl().setDocumentReadWrite();
  }

  @Override
  public void setDocumentReadWriteAutoIds() {
    getDocControl().setDocumentReadWriteAutoIds();
  }

  @Override
  public void setDocumentNoAccess() {
    getDocControl().setDocumentNoAccess();
  }

}
