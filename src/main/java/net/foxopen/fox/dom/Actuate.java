package net.foxopen.fox.dom;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.ex.ExValidation;
import nu.xom.Document;
import nu.xom.Node;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


abstract class Actuate
{

  public static final String FOXID = "foxid";

  protected final String mAccessViolationInfo;
  protected final DocControl mDocControl;

  Actuate(String pAccessViolationInfo, DocControl pDocControl)
  {
   mAccessViolationInfo = pAccessViolationInfo;
   mDocControl = pDocControl;
  }

  public void throwAccessViolation(Node pNode)
  throws ExInternal
  {
    throw new ExInternal("Access violation DOM: "+mAccessViolationInfo + " for root element " + (pNode != null && pNode.getDocument() != null ? pNode.getDocument().getRootElement().getLocalName() : "UNKNOWN"));
  }

  public DOMList getCreateXPathUL(Node pNode, String pSimplePath) throws ExInternal, ExBadPath
  {
    throwAccessViolation(pNode);
    return null;
  }

  public DOMList getCreateXPathUL(Node pNode, String pSimplePath, ContextUElem pContextUElem) throws ExInternal, ExBadPath
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node get1Element(Node pNode, String pSimplePath)
  throws ExInternal, ExTooFew, ExTooMany {
    throwAccessViolation(pNode);
    return null;
  }

  public Node get1ElementByLocalName(Node pNode, String pSimplePath)
  throws ExInternal, ExTooFew, ExTooMany {
    throwAccessViolation(pNode);
    return null;
  }

  public String get1String(Node pNode, String pSimplePath)
  throws ExInternal, ExTooFew, ExTooMany
  {
    throwAccessViolation(pNode);
    return null;
  }

  public String getAbsolute(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public String getRelativeDownToOrNull(Node pNode, Node pNestedNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public String getAttribute(Node pNode, String pAttrName)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public String getAttributeNamespaceURI (Node pNode, String pAttrName)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Map<String, String> getAttributes (Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Map<String, String> getAttributes (Node pNode, String pNamespaceURI)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Map<String, String> getAttributes (Node pNode, String pNamespaceURI, boolean pLocalNames)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public int getSiblingIndex(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return Integer.MIN_VALUE;
  }

  public ArrayList<String> getAttrNames (Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node getElemByRef(Node pNode, String pRefString)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node getElemByRefOrNull(Node pNode, String pRefString)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node getLastChildElement(Node pNode)
  throws ExInternal, ExTooFew
  {
    throwAccessViolation(pNode);
    return null;
  }

  public String getLocalName(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public String getName(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node getNextSiblingOrNull(Node pNode, boolean pElementsOnly)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node getParentOrNull(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node getParentOrSelf(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public String getPerfectRef(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node getPreviousSiblingOrNull(Node pNode, boolean pElementsOnly)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node getRootElement(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  /** Navigate simple paths (not xpath), supports / (root) . ..**/
  public DOMList getUL(Node pNode, String pSimplePath)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public DOMList getULByLocalName(Node pNode, String pSimplePath)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  /**
   * @return The contents text nodes concatenate together as a string
   */
  private void _value(Node pNode, StringBuffer sb, boolean deep)
  throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public void outputCanonicalDocument(Document pNode, OutputStream pOutputStream) {
    throwAccessViolation(pNode);
  }

  public void outputDocument(Document pNode, OutputStream pOutputStream, boolean pPrettyPrint) {
    throwAccessViolation(pNode);
  }

  public void outputNode(Node pNode, OutputStream pOutputStream, boolean pPrettyPrint, boolean pWriteXMLDeclaration) {
    throwAccessViolation(pNode);
  }

  public void outputNodeContents(Node pNode, OutputStream pOutputStream, boolean pPrettyPrint, boolean pWriteXMLDeclaration) {
    throwAccessViolation(pNode);
  }

  public String value(Node pNode, boolean deep)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public String valueWhitespaceIntelligent(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }


  public DOMList xpathUL(Node pNode, String pXPATH)
  throws ExInternal, ExBadPath
  {
    throwAccessViolation(pNode);
    return null;
  }

  public DOMList xpathUL(Node pNode, String pXPATH, ContextUElem pContextUElem)
  throws ExInternal, ExBadPath
  {
    throwAccessViolation(pNode);
    return null;
  }

  /**
  * Returns whether the xpath is true or false.
  * The xpath is false if the path returns a nodelist of size 0 .
  * The xpath is true if the path returns a nodelist of size 1+.
  */
  public boolean xpathBoolean(Node pNode, String pXPATH)
  throws ExInternal, ExBadPath
  {
    throwAccessViolation(pNode);
    return false;
  }

  public String  xpathString(Node pNode, String pXPATH)
  throws ExInternal, ExBadPath
  {
    throwAccessViolation(pNode);
    return null;
  }

  private Node xpath1Node(Node pNode, String pXPATH)
  throws ExInternal, ExTooFew, ExTooMany, ExBadPath
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node xpath1Element(Node pNode, String pXPATH)
  throws ExInternal, ExTooFew, ExTooMany, ExBadPath
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node addElement(Node pNode, String pName)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node addElementWithNamespace(Node pNode, String pName, String pNamespaceURI)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node addElement(Node pNode, String pName, String pTextContent)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public void addComment(Node pNode, String pCommentText)
  throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public void addPI(Node pNode, String pTarget, String pData)
  throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public void assignAllRefs(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public Node clone(Node pNode, Node pOtherDocumentNodeOptional, boolean pDeep)
    throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node externalCreateElement(Node pNode, String pName)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node externalCreateText(Node pNode, String pName)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  /** Copy all node contexts to another node */
  public void copyContentsTo(Node pNode, Node pNewParent, boolean pResetRefs)
  throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public Node copyToParent(Node pNode, Node pNewParent, boolean pResetRefs)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node create1Element(Node pNode, String pSimplePath)
  throws ExInternal, ExTooMany
  {
    throwAccessViolation(pNode);
    return null;
  }

  public DOMList getChildElements(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public DOMList getChildNodes(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
     return null;
 }

  public Node getCreate1Element(Node pNode, String pSimplePath)
  throws ExInternal, ExTooMany
  {
    throwAccessViolation(pNode);
    return null;
  }

  public String getAbsolutePathForCreateableXPath(Node pNode, String create_get_xpath, ContextUElem pContextUElem)
  throws ExTooMany, ExBadPath, ExDOMName {
    throwAccessViolation(pNode);
    return null;
  }

  public Node getCreateXpath1Element(Node pNode, String create_get_xpath, ContextUElem pContextUElem)
  throws ExInternal, ExTooMany, ExBadPath
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node getCreateXpath1Element(Node pNode, String create_get_xpath)
  throws ExInternal, ExTooMany, ExBadPath
  {
    throwAccessViolation(pNode);
    return null;
  }

  public String getRef(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  /**
   * Returns true if the element contains an attribute with the name of the value passed
   */
  public boolean hasAttribute(Node pNode, String s_attr_name)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return false;
  }

  /**
   * Returns true if the element contains any nodes
   */
  public boolean hasChildNodes(Node pNode)
  throws ExInternal {
    throwAccessViolation(pNode);
    return false;
  }

  /*
   * Elem's hashcode is now the underlying DOM element's hashcode value.
   */
  public int hashCode(Node pNode) {
    return pNode.hashCode();
  }

  public void renameElement(Node pNode, String newName)
  throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public Node replaceWith(Node pNode, Node pNewNode)
    throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  /** Move all node contexts to another node */
  public void moveContentsTo(Node pNode, Node pNewParent)
  throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public Node moveToParent(Node pNode, Node pNewParent)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node moveToParentBefore(
    Node pTargetParentNode
  , Node pMovingNode
  , Node pPositionBeforeTargetParentsChildNode
  )
  throws ExInternal {
    throwAccessViolation(pTargetParentNode);
    return null;
  }

  public Node remove(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node removeRefsRecursive(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public Node removeAllChildren(Node pNode)
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  public void removeAttribute(Node pNode, String name)
    throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public void setAttribute(Node pNode, String pName, String pValue)
  throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public void setXMLOrText(Node pNode, String pNewText)
  throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public void setText(Node pNode, String pNewText)
  throws ExInternal
  {
    throwAccessViolation(pNode);
  }

  public boolean contentEqualsOrSuperSetOf(
    Node pNode
  , Node pSubSetNode
  , boolean pTrimWhitespace
  )
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return false;
  }

  public String getURIForNamespacePrefix(Node pNode, String pPrefix){
    throwAccessViolation(pNode);
    return null;
  }

  public void addNamespaceDeclaration(Node pNode, String pPrefix, String pURI){
    throwAccessViolation(pNode);
  }

  public void setDefaultNamespace(Node pNode, String pURI){
    throwAccessViolation(pNode);
  }

  public List<String> childTextNodesAsStringList(Node pNode, boolean pDeep){
    throwAccessViolation(pNode);
    return null;
  }

  public DOMList childTextNodesAsDOMList(Node pNode, boolean pDeep){
    throwAccessViolation(pNode);
    return null;
  }

  public boolean isAttached(Node pNode){
    throwAccessViolation(pNode);
    return false;
  }

  public void appendText(Node pNode, String pText){
    throwAccessViolation(pNode);
  }

  public void setDocType(Node pNode, String pRootElementName, String pPublicID, String pSystemID){
    throwAccessViolation(pNode);
  }

  public void validateAgainstSchema(Node pNode, DOM pSchemaDOM)
  throws ExValidation {
    throwAccessViolation(pNode);
  }

  public org.w3c.dom.Document convertToW3CDocument(Node pNode) {
    throwAccessViolation(pNode);
    return null;
  }
}
