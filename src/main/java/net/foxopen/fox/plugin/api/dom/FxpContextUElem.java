package net.foxopen.fox.plugin.api.dom;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;


public interface FxpContextUElem<D extends FxpDOM, DL extends FxpDOMList> {

  /**
   * Get the DOM node referenced by the given context label. If pLabel is null, the ATTACH label is retrieved by default.
   * @param pLabel Labelled element to get.
   * @return The referenced elemen, or nullt.
   */
  D getUElemOrNull(String pLabel);

  /**
   * Get the DOM node referenced by the given context label. If null, retrieves the "attach" label by default.
   * @param pLabel Context label (optional).
   * @return DOM resolved by pLabel.
   */
  DOM getUElem(FxpContextLabel pLabel);

  /**
   * Get the DOM node referenced by the given context label. If pLabel is null, retrieves the "attach" label by default.
   * @param pLabel Context label (optional).
   * @return DOM resolved by pLabel.
   * @throws ExInternal If the context label is not defined.
   */
  D getUElem(String pLabel) throws ExInternal;

  /**
   * Removes the label mapping for the given label from this ContextUElem. The DOM itself is not affected.
   * @param pLabel The label to remove.
   * @throws ExInternal If the label is not clearable.
   */
  void removeUElem(String pLabel);

  /**
   * Gets the ATTACH DOM of this ContextUElem.
   * @return The ATTACH DOM.
   */
  D attachDOM();

  /**
   * "Localises" this ContextUElem to allow the definition of additional labels in a localised manner without affecting its
   * overall state. After calling this method, defining additional labels and executing XPaths, you should return it to
   * its initial state using {@link #delocalise}. It is possible to localise multiple times if necessary - localisation
   * is treated like a stack.
   * @param pPurpose Purpose of the localisation, used for debugging.
   * @return Self reference.
   */
  FxpContextUElem<D, DL> localise(String pPurpose);

  /**
   * Delocalises this ContextUlem, removing any label mappings which were defined since {@link #localise}was called.
   * @param pPurpose Purpose originally used to localise this ContextUElem. Used to ensure implementation errors do not
   * occur. The strings used must match exactly.
   * @return Self reference.
   */
  FxpContextUElem<D, DL> delocalise(String pPurpose);

  boolean isLocalised();

  String getLocalisedPurpose();

  /**
   * Sets a built-in context label on this ContextUElem. Note that you cannot set document level contexts (e.g. root, theme).
   * @param pLabel                The ContextLabel to set.
   * @param pUElem                The DOM node to map to the label.
   */
  void setUElem(FxpContextLabel pLabel, D pUElem);

  /**
   * Sets an arbitrary context label on this ContextUElem. For setting built-in labels such as :{action}, use
   * {@link #setUElem(FxpContextLabel, FxpDOM)}.
   * @param pLabelName Name to use to refer to the label. E.g. "label" would be referenced as <tt>:{label}</tt>.
   * @param pUElem The DOM node to map to the label.
   */
  void setUElem(String pLabelName, D pUElem);

  /**
   * Executes a FOX XPath which returns a single element.
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @param pCreateMissingNodesOption If true, creates elements along the path if they are not found. See
   * {@link DOM#getCreate1E}for more information.
   * @param pDefaultContextLabelOptional Specify a context label to use as the initial context node of the XPath expression.
   * If null, the default ATTACH point is used.
   * @return The element resolved by the XPath.
   * @throws ExActionFailed If the XPath returns the wrong cardinality or if the XPath cannot be executed.
   */
  D extendedXPath1E(String pFoxExtendedXpath, boolean pCreateMissingNodesOption, String pDefaultContextLabelOptional) throws ExActionFailed, ExTooMany, ExTooFew;

  /**
   * Executes a FOX XPath which returns a single element. The ATTACH node is used as the initial context node.
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @return The element resolved by the XPath.
   * @throws ExActionFailed If the XPath returns the wrong cardinality or if the XPath cannot be executed..
   */
  D extendedXPath1E(String pFoxExtendedXpath) throws ExActionFailed, ExTooMany, ExTooFew;

  /**
   * Executes a FOX XPath which returns a single element. The ATTACH node is used as the initial context node.
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @param pCreateMissingNodesOption If true, creates elements along the path if they are not found. See
   * {@link DOM#getCreate1E}for more information.
   * @return The element resolved by the XPath.
   * @throws ExActionFailed If the XPath returns the wrong cardinality or if the XPath cannot be executed.
   */
  D extendedXPath1E(String pFoxExtendedXpath, boolean pCreateMissingNodesOption) throws ExActionFailed, ExTooMany, ExTooFew;

  /**
   * Executes a FOX XPath which returns a single element. An ExTooFew exception is thrown if no matching elements are found.
   *
   * @param pRelativeDOM              The initial context node of the XPath expression.
   * @param pFoxExtendedXpath         The XPath expression to execute.
   * @return The element resolved by the XPath.
   * @throws ExActionFailed If the XPath cannot be executed or nodes cannot be created.
   * @throws ExTooMany      If too many nodes are matched.
   * @throws ExTooFew       If no nodes are matched and pCreateMissingNodesOption is false.
   */
  public D extendedXPath1E(final D pRelativeDOM, final String pFoxExtendedXpath) throws ExActionFailed, ExTooMany, ExTooFew;

  /**
   * Executes a FOX XPath which returns a single element.
   * @param pRelativeDOM The initial context node of the XPath expression
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @param pCreateMissingNodesOption If true, creates elements along the path if they are not found. See
   * {@link DOM#getCreate1E}for more information.
   * @return The element resolved by the XPath.
   * @throws ExActionFailed If the XPath cannot be executed or nodes cannot be created.
   * @throws ExTooMany If too many nodes are matched.
   * @throws ExTooFew If no nodes are matched and pCreateMissingNodesOption is false.
   */
  D extendedXPath1E(final D pRelativeDOM, final String pFoxExtendedXpath, final boolean pCreateMissingNodesOption) throws ExActionFailed, ExTooMany, ExTooFew;

  /**
   * Gets the absolute path to the node resolved by the given XPath. The node may not exist, in which case the same rules
   * for path-based node creation are followed to generate a hypothetical path. See {@link DOM#getAbsolutePathForCreateableXPath}
   * for more information.
   * @param pRelativeDOM The initial context node of the XPath expression
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @return Absolute path to the node resolved by the expression (the node may not actually exist).
   * @throws ExActionFailed If the XPath cannot be executed or nodes names for path steps cannot be generated.
   * @throws ExTooMany If too many nodes are matched.
   */
  String getAbsolutePathForCreateableXPath(D pRelativeDOM, final String pFoxExtendedXpath) throws ExActionFailed, ExTooMany, ExDOMName;

  /**
   * Executes a FOX XPath which returns a node list.
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @param pDefaultContextLabelOptional Specify a context label to use as the initial context node of the XPath expression.
   * If null, the default ATTACH point is used.
   * @return The nodes resolved by the XPath as a DOMList.
   * @throws ExActionFailed If the XPath cannot be executed.
   */
  DL extendedXPathUL(String pFoxExtendedXpath, String pDefaultContextLabelOptional) throws ExActionFailed;

  /**
   * Executes a FOX XPath which returns a node list.
   * @param pRelativeDOM The initial context node of the XPath expression.
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @return The nodes resolved by the XPath as a DOMList.
   * @throws ExActionFailed If the XPath cannot be executed.
   */
  DL extendedXPathUL(final D pRelativeDOM, final String pFoxExtendedXpath) throws ExActionFailed;

  /**
   * Evaluates an XPath expression and returns the result as a boolean. For details on how various result objects are
   * converted into booleans, see {@link XPathResult#asBoolean}.
   * <br/><br/>
   * Note the following arguments always return true and do not incur XPath evaluation:
   * <ul>
   * <li>Empty String</li>
   * <li>.</li>
   * <li>1</li>
   * <li>true()</li>
   * </ul>
   * Similarly the following are always false:
   * <ul>
   * <li>0</li>
   * <li>false()</li>
   * </ul>
   * @param pRelativeDOM Initial context node of the XPath expression.
   * @param pFoxExtendedXpath The XPath to be evaluated.
   * @return Boolean result of the XPath expression.
   * @throws ExActionFailed If XPath evaluation fails.
   */
  boolean extendedXPathBoolean(final D pRelativeDOM, final String pFoxExtendedXpath) throws ExActionFailed;

  /**
   * Evaluates an XPath expression and returns the result as a String. For details on how various result objects are
   * converted into Strings, see {@link XPathResult#asString}.
   * @param pRelativeDOM Initial context node of the XPath expression.
   * @param pFoxExtendedXpath The XPath to be evaluated.
   * @return The String result of XPath evaluation.
   * @throws ExActionFailed If XPath evaluation fails.
   */
  String extendedXPathString(final D pRelativeDOM, final String pFoxExtendedXpath) throws ExActionFailed;

  /**
   * Tests whether the given context label exists.
   * @param pLabel The label to check.
   * @return True if the context specified by pLabel is defined, false otherwise.
   */
  boolean existsContext(String pLabel);

  /**
   * Evaluate an XPath expression as a String, or return a constant String.
   * <br/><br/>
   * If the expression in pStringOrFoxExtendedXpath is nested in a <code>string()</code> function call, then the path
   * argument of the function is extracted and evaluated as an XPath expression. Note that this will result in
   * the <i>shallow</i> value of the targeted node, as per {@link ContextUElem#extendedXPathString(DOM,String)}.
   * If the deep value of the node is truly required, the <code>string()</code> function call can be double-nested.
   * E.g. <code>string(string(./HTML_NODE))</code>.
   * <br/><br/>
   * If the expression is not nested in a <code>string()</code> function call, the argument is considered to be a string
   * constant and is returned as is.
   *
   * @param pRelativeDOM Initial context node of any XPath expression.
   * @param pStringOrFoxExtendedXpath A constant string or XPath string expression. I.e. "Enter Well" or "string(localname())"
   * @return The String result.
   * @throws ExActionFailed If XPath processing fails.
   */
  String extendedStringOrXPathString(final D pRelativeDOM, final String pStringOrFoxExtendedXpath) throws ExActionFailed;

  /**
   * Executes a FOX XPath which returns a single element, creating elements along the path if they are not found. See
   * {@link DOM#getCreate1E}for more information. The ATTACH node is used as the initial context node.
   * @param pFoxExtendedXPath The XPath expression to execute.
   * @return The matched Element.
   * @exception ExActionFailed If the XPath returns the wrong cardinality or if the XPath cannot be executed.
   */
  D getCreateXPath1E(String pFoxExtendedXPath) throws ExActionFailed, ExTooMany;

  /**
   * Executes a FOX XPath which returns a single element, creating elements along the path if they are not found. See
   * {@link DOM#getCreate1E}for more information.
   * @param pFoxExtendedXPath The XPath expression to execute.
   * @param pDefaultContextLabel The context label to use as the initial context node. If null, the ATTACH node is used.
   * @return The matched Element.
   * @exception ExActionFailed If the XPath returns the wrong cardinality or if the XPath cannot be executed.
   */
  D getCreateXPath1E(String pFoxExtendedXPath, String pDefaultContextLabel) throws ExActionFailed, ExTooMany;

  /**
   * Executes a FOX XPath which returns a node list, creating elements along the path if they are not found. See
   * {@link DOM#getCreate1E}for more information. The ATTACH node is used as the initial context node.
   * @param pFoxExtendedXPath The XPath expression to execute.
   * @return The matched nodes as a DOMList.
   * @exception ExActionFailed If the XPath cannot be executed.
   */
  DL getCreateXPathUL(String pFoxExtendedXPath) throws ExActionFailed;

  /**
   * Executes a FOX XPath which returns a node list, creating elements along the path if they are not found. See
   * {@link DOM#getCreate1E}for more information.
   * @param pFoxExtendedXPath The XPath expression to execute.
   * @param pDefaultContextLabelOptional The context label to use as the initial context node. If null, the
   * ATTACH node is used.
   * @return The matched nodes as a DOMList.
   * @exception ExActionFailed If the XPath cannot be executed.
   */
  DL getCreateXPathUL(String pFoxExtendedXPath, String pDefaultContextLabelOptional) throws ExActionFailed;

  /**
   * Searches for an element with the given FOXID in all the implicated documents of this ContextUElem. If no match is
   * found, an exception is raised. Note that this method will load lazy DOMs if the match cannot be found in non-lazy DOMs.
   * @param pRef The FOXID of the element to retrieve.
   * @return The matched element.
   */
  D getElemByRef(String pRef);

  /**
   * Searches for an element with the given FOXID in all the implicated documents of this ContextUElem. Note that this
   * method will load lazy DOMs if the match cannot be found in non-lazy DOMs.
   * @param pRef The FOXID of the element to retrieve.
   * @return The matched element, or null if it could not be found.
   */
  D getElemByRefOrNull(String pRef);

  /**
   * Test if the DOM referred to by pLabel is still attached to its document tree.
   * @param pLabel The context label to test.
   * @return True if the label DOM is still attached, false otherwise.
   */
  boolean isLabelStillAttached(String pLabel);
}
