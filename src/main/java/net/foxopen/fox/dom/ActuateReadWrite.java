package net.foxopen.fox.dom;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDOM;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.IllegalNameException;
import nu.xom.NamespaceConflictException;
import nu.xom.Node;
import nu.xom.ParentNode;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;

import java.io.StringReader;
import java.util.Iterator;


/**
 * Actuator methods for a writable XML document.
 */
public class ActuateReadWrite
extends ActuateReadWriteGeneric {

  final Iterator mUniqueIterator;

  ActuateReadWrite(String pAccessViolationInfo, DocControl pDocControl) {
    super(pAccessViolationInfo, pDocControl);
    mUniqueIterator = pDocControl.mUniqueIterator;
  }

  public void assignAllRefs(Node pNode) {
    mDocControl.refIndexAssignRecursive();
  }

  private Node cloneInternal(Node pNode, boolean pDeep) {
    Node lClonedNode = null;
    if(pDeep){
      lClonedNode = pNode.copy();
    }
    else {
      lClonedNode = DOM.shallowCloneIncludingAttrs(pNode);
    }
    return lClonedNode;
  }

  /**
   * Clones pNode.
   * @param pNode The Node to clone.
   * @param pTargetDocumentNode A node identifying the target Document this node will be cloned into (used for ref reassignment).
   * @param pDeep If true, a deep copy is returned. If false, only the node is copied. Note: Element clone includes attributes.
   * @return The cloned Node.
   */
  public Node clone(Node pNode, Node pTargetDocumentNode, boolean pDeep) {

    Node lClonedNode = cloneInternal(pNode, pDeep);

    if(lClonedNode instanceof Element){
      DocControl.getDocControl(pTargetDocumentNode).refIndexReassignRecursive((Element) lClonedNode);
    }

    return lClonedNode;
  }


  public Node copyToParent(Node pNode, Node pNewParent, boolean pResetRefs){
    DocControl.setDocumentModified(pNewParent);

    Node lClone = cloneInternal(pNode, true);
    ((ParentNode) pNewParent).appendChild(lClone);

    if(pResetRefs && lClone instanceof Element){
      DocControl.getDocControl(pNewParent).refIndexReassignRecursive((Element) lClone);
    }

    return lClone;
  } // end copyToParent


  /**
   * Use simplified Xpath expression to create a new element, if the intermediate elements don't exist then create them on the way. One important restriction
   * create get must not contain [clauses]
   * this differs from get create1e in that a new leaf element will be created even if one already exists
   * @return return located or created element
   */
  public Node create1Element(Node pNode, String pSimplePath)
  throws ExTooMany{

    mDocControl.setDocumentModifiedCount();

    // split out path into path and element components
    int pos  = pSimplePath.lastIndexOf('/');
    Node lNode = pNode;
    String elemName = pSimplePath;
    if (pos > -1 ) {
      // Create the path if not already done
      lNode = getCreate1Element(pNode, pSimplePath.substring(0,pos));
      elemName =  pSimplePath.substring(pos + 1);
    }
    if (elemName.equals(".")) {
      // just return the current node if . specified
      return lNode;
    }
    else {
      return addElement(lNode, elemName);
    }
  }

  public Node externalCreateElement(Node pNode, String pName)
  throws ExInternal
  {
    mDocControl.setDocumentModifiedCount();
    return _internalCreateElementNoChangeInc(pNode, pName);
  }


  protected Element _internalCreateElementNoChangeInc(Node pNode, String pName) {
    return DOM.createElementInternal(pName, pNode);
  }

  public Node externalCreateText(Node pNode, String pName) {
    mDocControl.setDocumentModifiedCount();
    return _internalCreateText(pNode, pName);
  }

  protected Node _internalCreateText(Node pNode, String pText) {
    Text lNewText = new Text(pText);
    return lNewText;
  }

  /**
   * Use simplified Xpath expression to identify one element, if the intermediate elements don't exist then create them on the way. One important restriction
   * create get must not contain [clauses]
   * @return return located or created element
   */
  public Node getCreate1Element(Node pNode, String pSimplePath)
  throws ExInternal, ExTooMany
  {
    // Variable mDocumentModifiedCount now conditionally inc'ed below when new nodes added
    try {
      //Call overloaded get1Element with creation allowed
      return get1Element(pNode, pSimplePath, true, false);
    }
    catch (ExTooFew e){
      //Ignore this; it won't happen as we overload the method which throws this in ActuateReadOnly
      return null;
    }

  } // getCreate1Element

  /**
   * This overloads ActuateReadOnly's version to create a new Element called pNav instead of throwing an exception.
   * @param pParentNode The node to create the element in.
   * @param pSimplePath The full path being dealt with.
   * @param pNav The token of the path currently being dealt with.
   * @return The new Element.
   * @throws ExTooFew Never.
   */
  @Override
  protected Node get1ElementNotMatched(Node pParentNode, String pSimplePath, String pNav) {
    mDocControl.setDocumentModifiedCount();
    if(!"*".equals(pNav)){
      //Create the new Element
      Node lNewElement = _internalCreateElementNoChangeInc(pParentNode, pNav);
      //Append to parent
      ((ParentNode) pParentNode).appendChild(lNewElement);
      //Return a reference
      return lNewElement;
    }
    else {
      throw new ExDOMName("Cannot create a new element called '*' in path " + pSimplePath);
    }
  }

  /**
   * Gets an Element or Attribute by executing an XPath. If nodes along the path do not exist they are created.
   * The path is tokenised based on the '/' character and these tokens are used as names when creating nodes
   * (only Element or Attribute nodes will be created).
   * If a path step contains predicates or axes, an exception will be thrown if the step does not match any elements.
   * @param pNode The target node.
   * @param pXPath The XPath to execute.
   * @return The matched or created node.
   * @throws ExTooMany If more than one node is matched at any step of the path.
   * @throws ExBadPath If the XPath is invalid or cannot be executed.
   */
  public Node getCreateXpath1Element(Node pNode, String pXPath)
  throws ExTooMany, ExBadPath {
    return getCreateXpath1Element(pNode, pXPath, null);
  }

  /**
   * Gets an Element or Attribute by executing an XPath. If nodes along the path do not exist they are created.
   * The path is tokenised based on the '/' character and these tokens are used as names when creating nodes
   * (only Element or Attribute nodes will be created).
   * If a path step contains predicates or axes, an exception will be thrown if the step does not match any elements.
   * @param pNode The target node.
   * @param pXPath The XPath to execute.
   * @param pContextUElem Optional ContextUElem to evaluate labels if they are used.
   * @return The matched or created node.
   * @throws ExTooMany If more than one node is matched at any step of the path.
   * @throws ExBadPath If the XPath is invalid or cannot be executed.
   */
  @Override
  public Node getCreateXpath1Element(Node pNode, String pXPath, ContextUElem pContextUElem)
  throws ExTooMany, ExBadPath {

    StringBuffer lPathToReduce = new StringBuffer(pXPath);
    StringBuffer lPathNeedsCreating = new StringBuffer();

    //Find the furthest node down the path which exists
    Node lCurrentNode = seekFurthestNodeInPath(lPathToReduce, pNode, pContextUElem, lPathNeedsCreating);

    // Create tail elements or attribute
    while(lPathNeedsCreating.length() != 0) {
      String lNodeName = XFUtil.pathPopHead(lPathNeedsCreating, true);

      if(lNodeName.startsWith("@") || lNodeName.startsWith("attribute::")) {
        lNodeName = XFUtil.replace("@", "", lNodeName);
        lNodeName = XFUtil.replace("attribute::", "", lNodeName);
        if(!DOM.isValidXMLName(lNodeName)){
          throw new ExDOMName("Cannot create an attribute called '" + lNodeName + "' as this is not a valid XML name.");
        }
        setAttribute(lCurrentNode, lNodeName, "");
        Element lElement = (Element) lCurrentNode;
        lCurrentNode = lElement.getAttribute(lNodeName);
      }
      else {
        if(!DOM.isValidXMLName(lNodeName)){
          throw new ExDOMName("Cannot create an element called '" + lNodeName + "' as this is not a valid XML name.");
        }
        lCurrentNode = addElement(lCurrentNode, lNodeName);  // Note: addElement does mDocControl.mDocumentModifiedCount++ for us.
      }
    }

    return lCurrentNode;

  } // end

  public DOMList getCreateXPathUL(Node fromNode, String pSimplePath)
  throws ExInternal, ExBadPath {
    return getCreateXPathUL(fromNode, pSimplePath, null);
  }

  public DOMList getCreateXPathUL(Node fromNode, String simpleXPathExpr, ContextUElem pContextUElem)
   throws ExInternal, ExBadPath
   {
      // Check for recurse expression - not yet supported
      if(simpleXPathExpr.length() > 0 && simpleXPathExpr.indexOf("//") >= 0)
      {
        throw new ExInternal(getClass().getName()+"::getCreateXPathUL() does not support // as yet: \""+simpleXPathExpr+"\"", new DOM(fromNode));
      }

      DocControl.setDocumentModified(fromNode);
      StringBuffer currentPath = new StringBuffer(simpleXPathExpr);

      // Catch document root XPATH
      if(simpleXPathExpr.equals("/")) {
        DOMList root =  new DOMList();
        root.add(new DOM(fromNode.getDocument()));
        return root;
      }

      // Test for root
      if (simpleXPathExpr.startsWith("/"))
      {
         fromNode = fromNode.getDocument();
      }

      String headPathElement = XFUtil.pathPopHead(currentPath, true);
      if (headPathElement.length() == 0)
         return new DOMList();

      DOMList foundNodes = xpathUL(fromNode, headPathElement, pContextUElem);
      if (foundNodes.getLength() == 0)
      {
         foundNodes.add(new DOM(addElement(fromNode, headPathElement)));
      }

      //------------------------------------------------------------------------
      // Now we have a starting point(s), attempt to step down the path
      // one element at a time, finding or creating the node(s) as we go.
      //------------------------------------------------------------------------
      DOMList foundDescendantNodes = new DOMList();
      while(currentPath.length() != 0)
      {
         headPathElement = XFUtil.pathPopHead(currentPath, true);
         foundDescendantNodes.clear();

         for (int n=0; n < foundNodes.getLength(); n++)
         {
            DOMList fromNodeMatches = xpathUL(foundNodes.item(n).getNode(), headPathElement, pContextUElem);
            if (fromNodeMatches.getLength() > 0)
            {
               foundDescendantNodes.addAll(fromNodeMatches);
            }
            else
            {
               // Node doesn't exist under this fromNode - so let's create one.
               try {
                foundDescendantNodes.add(new DOM(addElement(foundNodes.item(n).getNode(), headPathElement)));
               }
               catch(ExDOMName x) {
                 throw new ExDOMName("Error initialising element name '"+headPathElement
                 +" at path "+getAbsolute(foundNodes.item(n).getNode())
                 );
               }
            }
         }
         foundNodes.clear();
         foundNodes.addAll(foundDescendantNodes);
      }
      return foundNodes;
   }

  public String getRef(Node pNode)
  throws ExInternal {
    // Get existing or create fox attribute
    String lRef = getAttribute(pNode, FOXID);
    if(lRef.length()==0) {
      mDocControl.setDocumentModifiedCount();

      // Assign foxid to element
      lRef = (String) mUniqueIterator.next();
      Element lElement = (Element) pNode;
      lElement.addAttribute(new Attribute(FOXID, lRef));

      // Register foxid in index
      mDocControl.setRefIndex(lRef, lElement);

    }

    // Return reference
    return lRef;
  } // end getRef

  public Node moveToParentBefore(
    Node pTargetParentNode
  , Node pMovingNode
  , Node pPositionBeforeTargetParentsChildNode
  ) {
    // mDocControl in context of pTargetParentNode
    mDocControl.setDocumentModifiedCount();
    //If this is being moved FROM another document, update the FROM document's modified count
    if(pMovingNode.getDocument() != pTargetParentNode.getDocument()){
      DocControl.setDocumentModified(pMovingNode);
    }

    // The SiblingChild should already be attached to Parent element - so this is belt & braces check
    if (pPositionBeforeTargetParentsChildNode != null && pPositionBeforeTargetParentsChildNode.getParent() != pTargetParentNode) {
      throw new ExInternal("insertBefore error, the sibling is not a child of this node");
    }

    //Detach from parent
    pMovingNode.detach();

    int lTargetPosition = ((ParentNode) pTargetParentNode).indexOf(pPositionBeforeTargetParentsChildNode);
    ((ParentNode) pTargetParentNode).insertChild(pMovingNode, lTargetPosition);

    return pMovingNode;
  }

  public Node moveToParent(Node pNode, Node pNewParent) {

    mDocControl.setDocumentModifiedCount();
    //If this is being moved FROM another document, update the FROM document's modified count
    if(pNode.getDocument() != pNewParent.getDocument()){
      DocControl.setDocumentModified(pNode);
    }

    //Detach from parent
    pNode.detach();
    //Attach to new parent
    ((ParentNode) pNewParent).appendChild(pNode);
    return pNode;
  }

  /**
   * Remove (i.e. detach) pNode from its parent. pNode will remain as an unattached node with no parent Element or
   * owning Document.
   * @param pNode The Node to remove.
   * @return pNode.
   * @throws ExInternal If the node cannot be removed because it is a Document node or root Element.
   */
  public Node remove(Node pNode)
  throws ExInternal {
    mDocControl.setDocumentModifiedCount();

    if(pNode instanceof Document){
      throw new ExInternal("The Document node cannot be removed.");
    } else if (pNode.getParent() instanceof Document){
      throw new ExInternal("The root element cannot be removed: " + getAbsolute(pNode));
    }

    //Detaching from parent is effectively a removal
    pNode.detach();

    if(pNode instanceof Element){
      mDocControl.refIndexRemoveRecursive((Element) pNode);
    }

    return pNode;
  }

  public Node removeRefsRecursive(Node pNode) {
    if(pNode instanceof Element){
      DocControl.getDocControl(pNode).refIndexRemoveRecursive((Element) pNode);
    }
    return pNode;
  }

  public Node removeAllChildren(Node pNode)
  throws ExInternal
  {
    mDocControl.setDocumentModifiedCount();
    ParentNode lNodeAsParent = (ParentNode) pNode;

    //This is a LIVE list so pop nodes off the start until it is empty.
    while(lNodeAsParent.getChildCount() > 0){
      Node lChild = lNodeAsParent.getChild(0);
      if(lChild instanceof Element){
        mDocControl.refIndexRemoveRecursive((Element) lChild);
      }
      lNodeAsParent.removeChild(0);
    }

    return pNode;
  }

  public void removeAttribute(Node pNode, String pName) {
    mDocControl.setDocumentModifiedCount();

    // Cast to Element (checks is an element)
    Element lElement;
    try {
      lElement = (Element) pNode;
    }
    catch (ClassCastException x) {
      throw new ExInternal("Not an Element", x);
    }

    Attribute lAttribute = getAttributeInternal(lElement, pName);
    if(lAttribute != null){
      lElement.removeAttribute(lAttribute);
    }
  }

  /**
   * Replaces pNode with pNewNode.
   * @param pNode The Node to replace.
   * @param pNewNode The Node to replace pNode with.
   * @return pNewNode
   */
  public Node replaceWith(Node pNode, Node pNewNode) {
    mDocControl.setDocumentModifiedCount();

    //If pNewNode is being moved FROM another document, update the FROM document's modified count
    if(pNode.getDocument() != pNewNode.getDocument()){
      DocControl.setDocumentModified(pNewNode);
    }

    pNewNode.detach();

    ParentNode lParent = pNode.getParent();
    lParent.replaceChild(pNode, pNewNode);

    return pNewNode;
  }

  public void setAttribute(Node pNode, String pName, String pValue)
  throws ExDOM
  {

    if(pName==null || pValue==null) {
      throw new ExInternal("setAttribute passed a null value "+pName+"="+pValue);
    }

    mDocControl.setDocumentModifiedCount();

    // Cast to Element (checks is an element)
    Element lElement;
    try {
      lElement = (Element)pNode;
    }
    catch (ClassCastException x) {
      throw new ExInternal("Not an Element", x);
    }
    // Set attribute
    try {
      //If the node has a prefix we need to tell XOM about it
      String lURI = DOM.getNamespaceURIForNodeName(pNode, pName);
      if(lURI != null){
        lElement.addAttribute(new Attribute(pName, lURI, pValue));
      } else {
        lElement.addAttribute(new Attribute(pName, pValue));
      }
    }
    catch (NamespaceConflictException e){
      throw new ExDOMName("Invalid attribute name " + pName, e);
    }
    catch (IllegalNameException e){
      throw new ExDOMName("Invalid attribute name " + pName, e);
    }
  }

  public Node addElement(Node pNode, String pName)
  throws ExInternal
  {
    mDocControl.setDocumentModifiedCount();
    Element lNewElement = _internalCreateElementNoChangeInc(pNode, pName);

    try {
      ((ParentNode) pNode).appendChild(lNewElement);
    }
    catch (Exception e) {
      throw new ExInternal("Unable to add element to node: "+pName, e);
    }
    return lNewElement;
  }

  @Override
  public Node addElementWithNamespace(Node pNode, String pName, String pNamespaceURI) {
    mDocControl.setDocumentModifiedCount();
    Element lNewElement;
    try{
      lNewElement = new Element(pName, pNamespaceURI);
    }
    catch (NamespaceConflictException e){
      throw new ExDOMName("Invalid element name '" + pName + "'", e);
    }
    catch(IllegalNameException e){
      throw new ExDOMName("Invalid element name '" + pName + "'", e);
    }

    try {
      ((ParentNode) pNode).appendChild(lNewElement);
    }
    catch (Exception e) {
      throw new ExInternal("Unable to add element to node: "+pName, e);
    }
    return lNewElement;
  }

  public void addComment(Node pNode, String pCommentText) {
    mDocControl.setDocumentModifiedCount();

    // Cast to Element (checks is an element)
    Element lElement;
    try {
      lElement = (Element)pNode;
    }
    catch (ClassCastException x) {
      throw new ExInternal("Not an Element", x);
    }
    // Add Comment
    lElement.appendChild(new Comment(pCommentText));
  }

  public void addPI(Node pNode, String pTarget, String pData)
  throws ExInternal
  {
    mDocControl.setDocumentModifiedCount();
    Element lElement;
    try {
      lElement = (Element)pNode;
    }
    catch (ClassCastException x) {
      throw new ExInternal("Not an Element", x);
    }

    lElement.appendChild(new ProcessingInstruction(pTarget, pData));
  }

  @Override
  public void addNamespaceDeclaration(Node pNode, String pPrefix, String pURI){
    ((Element) pNode).addNamespaceDeclaration(pPrefix, pURI);
  }

  @Override
  public void setDefaultNamespace(Node pNode, String pURI){
    ((Element) pNode).setNamespaceURI(pURI);
  }

  public Node addElement(Node pNode, String pName, String pTextContent)
  throws ExInternal
  {
    mDocControl.setDocumentModifiedCount();
    Node lElement = addElement(pNode, pName);
    setText(lElement, pTextContent);
    return lElement;
  }

  public void setXMLOrText(Node pNode, String pXmlText) {
    mDocControl.setDocumentModifiedCount();

    DOM lNode = new DOM(pNode);
    DOMList lChildren = lNode.getChildElements();
    lChildren.removeAllNamesFromList("fox-error");
    if (lChildren.getLength()>0) {
     lChildren.removeFromDOMTree();
    }

    if (pXmlText.indexOf('<')!=-1) {
      StringReader lXmlReader = new StringReader("<dummy>"+pXmlText+"</dummy>");
      try {
        DOM lNewXml = DOM.createDocument(lXmlReader);
        moveContentsTo(lNewXml.getNode(),pNode);
      } catch(ExInternal e) {
        setText(pNode,pXmlText);
      }
    } else {
      setText(pNode,pXmlText);
    }
  }

  public void setText(Node pNode, String pNewText) {

    mDocControl.setDocumentModifiedCount();

    // Cast to Element (checks is an element)
    Element lElement;
    try {
      lElement = (Element)pNode;
    }
    catch (ClassCastException x) {
      if(pNode instanceof Attribute) {
        Attribute lAttr = (Attribute) pNode;
        lAttr.setValue(XFUtil.nvl(pNewText, "")); // XOM cannot set attribute to null, must be empty string
        return;
      }
      else {
        throw new ExInternal("Cannot setText() on xml node of type "+pNode.getClass().getName(), x);
      }
    }

    // Remove all existing child text nodes
    for(int i = 0; i < pNode.getChildCount(); i++){
      Node lChild = pNode.getChild(i);
      if(lChild instanceof Text) {
        ((ParentNode) pNode).removeChild(lChild);
        i--;
      }
    }

    //Insert the new text node as the first child of the target
    if(!XFUtil.isNull(pNewText)) {
      lElement.insertChild(pNewText, 0);
    }
  }

  @Override
  public void appendText(Node pNode, String pText){
    if(!(pNode instanceof Element)){
      throw new ExInternal("pNode must be an element for appendText");
    }
    mDocControl.setDocumentModifiedCount();
    ((Element) pNode).appendChild(_internalCreateText(pNode, pText));
  }

  @Override
  public void setDocType(Node pNode, String pRootElementName, String pPublicID, String pSystemID){
    if(pNode.getDocument() == null){
      throw new ExInternal("Node does not have a Document.");
    }
    mDocControl.setDocumentModifiedCount();
    pNode.getDocument().setDocType(new DocType(pRootElementName, pPublicID, pSystemID));
  }

} // end class ActuateReadWrite
