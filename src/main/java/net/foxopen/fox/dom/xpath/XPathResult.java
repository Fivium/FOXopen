package net.foxopen.fox.dom.xpath;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.XPathWrapper;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExDOM;
import net.foxopen.fox.ex.ExInternal;
import net.sf.saxon.value.AtomicValue;
import nu.xom.Node;

import java.util.ArrayList;
import java.util.List;


/**
 * This class wraps a Saxon XPath result, providing convenience methods to cast it as required.
 * <br><br>
 * A general word on String and text node handling. This can be confusing; so to be clear, FOX should always follow these
 * rules:
 * <ol>
 * <li>If the meta code developer has used fn:string() in an XPath expression, the result of the XPath should always be the
 * "deep" text node value of the first item in the sequence, according to the XPath 2.0 spec. If there is more than one item in
 * the sequence, this is invalid under XPath 2.0 (when XPath 1.0 compatibility mode is disabled) so an error should occur.<br/><br/>
 * Of course, a rule wouldn't be a rule without an exception - if string() has been used to indicate that an expression is a path,
 * e.g. in fox:prompt="", the {@link ContextUElem} should unnest the function call and the result should be determined as
 * described below.<br/><br/></li>
 * <li>If a string is requested in FOX, i.e. by passing FoxXPathResultType.STRING to the XPath evaluator, and the result of
 * the expression is a node or node list, then FOX will return the "shallow" text node value of the first node in the list.</li>
 * </ol>
 * Note: this object should NOT be cached or otherwise retained on a member or static variable. It contains strong references
 * to DOM objects and caching it could cause a memory leak. If you need to cache the result itself, use the asObject method
 * to retrieve the raw result object, and cache that.
 */
public class XPathResult {

  /**
   * The result of the XPath expression.
   */
  private final Object mResultObject;

  /**
   * The FoxPath which was executed to create this result.
   */
  private final FoxPath mFoxPath;

  /**
   * Context Node of the XPath. Used to store a reference to the DocControl for the implicated <b>document</b>
   * (NOT necessarily the implicated <i>node</i>).
   */
  private final DOM mRelativeDOM;

  /**
   * Count of how many Documents were accessed during XPath execution.
   */
  private final int mImplicatedDocumentCount;

  /** The FOX wrapping function (i.e. string()) which caused this XPath to be invoked. */
  private final XPathWrapper mXPathWrapper;


  /**
   * Constructs a new XPathResult to wrap a Saxon result object.
   * @param pResultObject The result of the XPath expression.
   * @param pXPath Used to resolve information about the contexts of the XPath expression.
   * @param pContextUElem A ContextUElem which is used if context labels need to be resolved.
   * @param pContextNode The context node of the XPath expression.
   */
  XPathResult(Object pResultObject, FoxXPath pXPath, ContextUElem pContextUElem, DOM pContextNode, XPathWrapper pXPathWrapper){

    if(pResultObject == null ){
      throw new ExInternal("Cannot construct an XPathResult with a null result object.");
    }
    else if (pXPath == null){
      throw new ExInternal("Cannot construct an XPathResult with a null XPath object.");
    }

    mResultObject = pResultObject;
    mFoxPath = pXPath;
    mImplicatedDocumentCount = pXPath.getImplicatedDocumentSet(pContextUElem, pContextNode).size();

    if(mImplicatedDocumentCount == 1){
      if(pXPath.usesContextItemOrDocument()){
        //1 document implicated and a the context node or document has been referenced (i.e. "./A" or "//A")
        //This means the XPath's relative DOM is the context node provided.
        mRelativeDOM = pContextNode;
      }
      else {
        //1 document implicated and a the context node or document has NOT been referenced.
        //This indicates an expression such as ":{theme}/X" which has been evaluated from an attach point that
        //was on a different document. The context node is considered to be the node of the first label referenced
        //by the expression. TODO assert this logic always holds.

        //Belt and braces
        if(pContextUElem == null) {
          throw new ExInternal("ContextUElem was null but XPath did not use context item or document (assertion error)");
        }

        mRelativeDOM = pContextUElem.getUElem(pXPath.getLabelSet().iterator().next());
      }
    }
    else {
      //With multiple implicated documents, the relative DOM is effectively unkown - currently it is not possible to know
      //the true relative DOM without complex Xpath parsing, and even then it may not be possible.
      //i.e. what is the context of ":{root}/X | :{theme}/Y"?
      mRelativeDOM = null;
    }

    mXPathWrapper = pXPathWrapper;
  }

  /**
   * Constructs a new XPathResult to wrap the result of a FoxSimplePath.
   * @param pResultObject The FoxSimplePath's result.
   * @param pPath The original path String.
   * @param pContextNode The context node used to evaluate the expression.
   */
  XPathResult(Object pResultObject, FoxSimplePath pPath, DOM pContextNode, XPathWrapper pXPathWrapper){

    if(pResultObject == null ){
      throw new ExInternal("Cannot construct an XPathResult with a null result object.");
    }
    else if (pPath == null){
      throw new ExInternal("Cannot construct an XPathResult with a null XPath object.");
    }

    mResultObject = pResultObject;
    mFoxPath = pPath;
    mImplicatedDocumentCount = 1;
    mRelativeDOM = pContextNode;
    mXPathWrapper = pXPathWrapper;
  }

  public static XPathResult getConstantResultFromString(String pConstantPathString) {
    return new FoxConstantPath(pConstantPathString, pConstantPathString).getXPathResult();
  }

  /**
   * Constructs a new XPathResult to wrap an object which was not actually returned from a FoxXPath execution
   * (i.e. a constant path). Used when short-circuiting Saxon evaluation.
   * @param pResultObject The Object to wrap.
   * @param pPath The original constant path.
   */
  XPathResult(Object pResultObject, FoxPath pPath){

    if(pResultObject == null ){
      throw new ExInternal("Cannot construct an XPathResult with a null result object.");
    }
    else if (pPath == null){
      throw new ExInternal("Cannot construct an XPathResult with a null XPath object.");
    }

    mResultObject = pResultObject;
    mFoxPath = pPath;
    mImplicatedDocumentCount = 0;
    mRelativeDOM = null;
    mXPathWrapper = XPathWrapper.NO_WRAPPER;
  }

  /**
   * @return The number of documents accessed during the execution of this XPath.
   */
  public int getNumberOfImplicatedDocuments(){
    return mImplicatedDocumentCount;
  }

  /**
   * Returns the original FoxPath which yielded this result.
   */
  public FoxPath getExecutedPath(){
   return mFoxPath;
  }

  /**
   * Get the relative DOM for this XPath, provided it only implicates exactly one Document (see {@link #getNumberOfImplicatedDocuments}).
   * Otherwise, returns null.<br/><br/>
   * Note: the DOM returned is only used for discerning the relevant Document for this XPath and is not guaranteed to point directly to
   * the context node of the expression.
   * @return The DOM providing a reference to the XPath's Document, or null if not exactly 1 document was implicated.
   */
  public DOM getRelativeDOM(){
    return mRelativeDOM;
  }

  /**
  * Gets this XPathResult object as a Boolean. The following logic is used for different result types:
  * <ul>
  * <li>For NodeLists - true if the list has more than one item or false if it is empty. If the list contains exactly
  * one item, it is extracted and evaluated according to the rules listed below.</li>
  * <li>For Nodes - true</li>
  * <li>For Strings - true if the String is longer than 0 characters</li>
  * <li>For Numbers - true if the Number is not 0</li>
  * <li>For Booleans - the result as is</li>
  * </ul>
  * Note that this should reflect the "effective boolean value" of the result as defined in the XPath 2.0 spec.
  * (Although the spec says sequences of more than one value should raise an error, which this method contradicts by
  * returning true, for legacy purposes - note this will be the case even if the result is a sequence of 'false' values).
  * @return A boolean.
  */
  public boolean asBoolean(){

    Object lResultObject = mResultObject;

    if(lResultObject instanceof DOMList) {
      return ((DOMList) lResultObject).getLength() > 0;
    }
    else if(lResultObject instanceof List) {
      List lResultAsList = (List) lResultObject;
      //If the list has exactly 1 item, then unwrap it and treat it as the result object
      if(lResultAsList.size() == 1){
        lResultObject = lResultAsList.get(0);
      }
      else if(lResultAsList.size() == 0){
        return false;
      }
      else {
        return true;
      }
    }

    if(lResultObject instanceof Boolean) {
      return (Boolean) lResultObject;
    }
    else if(lResultObject instanceof String) {
      return ((String) lResultObject).length() != 0;
    }
    else if(lResultObject instanceof Number) {
      return ((Number) lResultObject).intValue() != 0;
    }
    else if(lResultObject instanceof Node || lResultObject instanceof DOM) {
      return true;
    }
    else {
      throw new ExInternal("Unable to cast XPath result (type " + lResultObject.getClass().getName() + ") to Boolean for XPath: " + mFoxPath.getOriginalPath());
    }
  }

  /**
  * Gets this XPathResult object as a Number. The following logic is used for different result types:
  * <ul>
  * <li>For Strings - the result of Double.parseDouble</li>
  * <li>For Nodes - the result of Double.parseDouble on the Node's value (see asString for details of how value is determined)</li>
  * <li>For NodeLists - the result of Double.parseDouble on the first Node's value (see asString)</li>
  * <li>For Numbers - the Number as is</li>
  * <li>For Booleans - integer 1 or integer 0</li>
  * </ul>
  * @return A Number.
  */
  public Number asNumber(){

    Object lResultObject = getObjectOrFirstObjectInList(mResultObject, "");

    if(lResultObject instanceof Number){
      return (Number) lResultObject;
    }
    else if (lResultObject instanceof Boolean){
      return ((Boolean) lResultObject) ? 1 : 0;
    }
    else {
      String lString = asString();
      Double lDouble;
      try {
        lDouble = Double.parseDouble(lString);
        return lDouble;
      } catch (NumberFormatException e){
        throw new ExInternal("Failed to convert the String result of XPath " + mFoxPath.getOriginalPath() + " (" + lString + ") to a Double", e);
      }
    }
  }

  /**
   * Tests if this XPathResult can be converted to a DOMList. If this method returns true, {@link #asDOMList} should
   * return a valid DOMList without failing. If this returns false, {@link #asDOMList} will error.
   * Note: an empty list is considered to be a valid DOMList.
   * @return True if this result can be treated as a DOMList.
   */
  public boolean isValidDOMList() {
    if(mResultObject instanceof DOMList){
      //If result is already a DOMList we know it's valid as a DOMList
      return true;
    }
    else if(mResultObject instanceof List) {
      //If result is any other type of list, defer validation to DOMList method
      return DOMList.isValidDOMList((List) mResultObject);
    }
    else {
      //If the result is a non-list object, add the object into a list and validate using the DOMList method.
      ArrayList lTempList = new ArrayList();
      lTempList.add(mResultObject);
      return DOMList.isValidDOMList(lTempList);
    }
  }

  /**
  * Gets this XPathResult object as a DOMList, following this logic:
  * <ul>
  * <li>For NodeLists - the list as a DOMList. Note: Saxon may return non-nodes in a list. Checking for non-nodes in the
  * list is handled by the DOMList constructor, which may throw an exception (see {@link DOMList#isValidDOMList}).</li>
  * <li>For Nodes - the Node wrapped in a DOMList of length 1.</li>
  * <li>For other types - error</li>
  * </ul>
  * @return A DOMList.
  * @throw ExDOM If the result cannot be converted to a DOMList.
  */
  public DOMList asDOMList()
  throws ExDOM{
    if(mResultObject instanceof DOMList){
      return (DOMList) mResultObject;
    }
    else if(mResultObject instanceof DOM){
      DOMList lDOMList = new DOMList();
      lDOMList.add((DOM) mResultObject);
      return lDOMList;
    }
    else if(mResultObject instanceof List){
      return new DOMList((List) mResultObject);
    }
    else if (mResultObject instanceof Node){
      DOMList lDOMList = new DOMList();
      lDOMList.add(new DOM((Node) mResultObject));
      return lDOMList;
    }
    else {
      throw new ExDOM("This XPath does not return a list or compatible node: " + mFoxPath.getOriginalPath() + " (type was a " + mResultObject.getClass().getName() + ")");
    }
  }

  /**
   * Gets the raw result object.
   * @return The XPath result as an object.
   */
  public Object asObject(){
    return mResultObject;
  }

  /**
  * Gets this XPathResult object as a DOM. The following logic is used for different result types:
  * <ul>
  * <li>For NodeLists - the first Node in the list as a DOM.</li>
  * <li>For Nodes - the Node as a DOM</li>
  * <li>For other types - null</li>
  * </ul>
  * @return A DOM, or null.
  */
  public DOM asResultDOMOrNull(){
    if(mResultObject instanceof DOM){
      return (DOM) mResultObject;
    }
    else if(mResultObject instanceof DOMList){
      return ((DOMList) mResultObject).get(0);
    }
    else if(mResultObject instanceof List){
      //Only covert a list's first item if it's a Node
      Object lListItem = ((List) mResultObject).get(0);
      if(lListItem instanceof Node) {
        return new DOM((Node) lListItem);
      }
      else {
        return null;
      }
    }
    else if (mResultObject instanceof Node){
      return new DOM((Node) mResultObject);
    }
    else {
      return null;
    }
  }

  private static Object getObjectOrFirstObjectInList(Object pResultObject, Object pReturnIfListEmpty) {

    if(pResultObject instanceof List) {
      List lResultList = (List) pResultObject;
      if(lResultList.size() == 0) {
        return pReturnIfListEmpty;
      }
      else {
        return lResultList.get(0);
      }
    }
    else {
      return pResultObject;
    }
  }

  /**
  * Gets this XPathResult object as a String. The following logic is used for different result types:
  * <ul>
  * <li>For NodeLists - return the non-recursive value of the first node in the list (i.e. text content for Elements,
  * value for Attributes).
  * If the list is empty, return an empty String.
  * </li>
  * <li>For Nodes - return the value of the node (as above)</li>
  * <li>For Booleans - string "true" or string "false"</li>
  * <li>For Numbers - the result of Number.toString()</li>
  * <li>For Strings - the String</li>
  * </ul>
  * @return The result as a String.
  */
  public String asString(){

    //If we have a list, use the first item as the result object
    Object lResultObject = getObjectOrFirstObjectInList(mResultObject, "");

    if(lResultObject instanceof String) {
      return (String) lResultObject;
    }
    else if(lResultObject instanceof DOM){
      return ((DOM) lResultObject).value(false);
    }
    else if(lResultObject instanceof Node){
      return new DOM((Node) lResultObject).value(false);
    }
    else if(lResultObject instanceof Boolean) {
      return ((Boolean) lResultObject) ? "true" : "false";
    }
    else if(lResultObject instanceof Number) {
      return ((Number) lResultObject).toString();
    }
    else if (lResultObject instanceof AtomicValue){
      return ((AtomicValue) lResultObject).getStringValue();
    }
    else {
      throw new ExInternal("Unable to convert XPATH result (type " + lResultObject.getClass().getName() + ") to String for XPath: " + mFoxPath.getOriginalPath());
    }
  }

  /**
  * Gets this XPathResult object as a List of Strings. The following logic is used for different result types:
  * <ul>
  * <li>For NodeLists/Lists - the "shallow" text values of all the nodes in the list (using toString for non-node objects)</li>
  * <li>For Nodes - the "shallow" text value of the node as a single item in the list</li>
  * <li>For other types - the toString of the object as a single item in the list</li>
  * </ul>
  * @return The result as a String List.
  */
  public List<String> asStringList(){

    List<String> lResult = new ArrayList<String>();

    if(mResultObject instanceof String) {
      lResult.add((String) mResultObject);
    }
    else if (mResultObject instanceof List) {
      for(Object o : (List) mResultObject) {
        if(o instanceof String){
          lResult.add((String) o);
        }
        else if(o instanceof DOM){
          lResult.add(((DOM) o).value(false));
        }
        else if(o instanceof Node){
          lResult.add(new DOM((Node) o).value(false));
        }
        else {
          lResult.add(mResultObject.toString());
        }
      }
    }
    else {
      lResult.add(mResultObject.toString());
    }

    return lResult;
  }

  /**
   * Tests if the result represented by this XPathResult requires escaping before it is output to an external destination.
   * This will typically be true if the source of the result was a DOM-based XPath, because DOMs may contain user data
   * which needs to be escaped. Developers can wrap their XPaths with the <tt>unescaped-string</tt> wrapper to suppress
   * this behaviour.
   * @return True if escaping was required.
   */
  public boolean isEscapingRequired() {
    if(mXPathWrapper == XPathWrapper.UNESCAPED_STRING) {
      return false;
    }
    else {
      return mImplicatedDocumentCount > 0;
    }
  }

  /**
   * Debug serializer for outputting this result object as a human-readable XML fragment. For developer debug use only
   * - do not write any application logic which depends on the result of this method.
   * @param pStringBuffer The StringBuffer to print the result into.
   */
  public void printResultAsXML(StringBuffer pStringBuffer){

    if(mResultObject instanceof DOM){
      pStringBuffer.append(((DOM)mResultObject).outputNodeContentsToString(true));
    }
    else if (mResultObject instanceof List || mResultObject instanceof DOMList){

      if(mResultObject instanceof List){
        if(((List) mResultObject).size() == 0){
          pStringBuffer.append("<!-- No data returned -->");
          return;
        }
      }
      else if(mResultObject instanceof DOMList) {
        if(((DOMList) mResultObject).getLength() == 0){
          pStringBuffer.append("<!-- No data returned -->");
          return;
        }
      }

      //First, try it as a DOM list
      DOMList lDOMList;
      try {
        lDOMList = asDOMList();
      }
      catch(ExDOM ex) {
        lDOMList = null;
        //Ok, it's not a list of nodes
      }
      //Print the contents of the DOM List if it's a DOM List
      if(lDOMList != null){
        for (int i = 0; i < lDOMList.getLength(); i++) {
          if(lDOMList.item(i).isText()) {
            pStringBuffer.append("<textNode>");
          }
          pStringBuffer.append(lDOMList.item(i).outputNodeToString(true));
          if(lDOMList.item(i).isText()) {
            pStringBuffer.append("</textNode>");
          }
        }
        return;
      }

      List lResultAsList = (List) mResultObject;
      if(lResultAsList.size() == 1){
        pStringBuffer.append("<item type=\"" + lResultAsList.get(0).getClass().getName() + "\">"  + XFUtil.sanitiseStringForOutput(lResultAsList.get(0).toString(), XFUtil.SANITISE_HTMLENTITIES)  + "</item>");
      }
      else {
        pStringBuffer.append("<sequence>\n");
        for(Object o : lResultAsList){
          String lOutString;
          if(o instanceof Node){
            lOutString = new DOM((Node) o).outputNodeToString(true);
          }
          else {
            lOutString = XFUtil.sanitiseStringForOutput(o.toString(), XFUtil.SANITISE_HTMLENTITIES);
          }
          pStringBuffer.append("  <item type=\"" + o.getClass().getName() + "\">" + lOutString  + "</item>\n");
        }
        pStringBuffer.append("</sequence>");
      }


    }
    else {
      pStringBuffer.append("<item>" + XFUtil.sanitiseStringForOutput(mResultObject.toString(), XFUtil.SANITISE_HTMLENTITIES) + "</item>");
    }

  }
}
