package net.foxopen.fox.plugin.api.dom;

import java.util.List;

import net.foxopen.fox.dom.DOM;


public interface FxpDOMList<D extends FxpDOM>
extends List<D> {
  /**
   * Gets the number of nodes in this list. Synononymous with {@link List#size}.
   * @return Size of the list.
   */
  int getLength();

  /**
   * Returns the Nth Node in the list. Synononymous with {@link List#get}.
   * @param index Index of Node to retrive.
   * @return Node at index.
   */
  FxpDOM item(int index);

  /**
   * Removes all the nodes in this DOMList from their DOM Tree Parent. The DOMList itself is unchanged.
   * @return Self-reference.
   */
  FxpDOMList removeFromDOMTree();

  /**
   * Renames all the Elements in this DOMList to the given name. This is not recursive - only the top-level Element is
   * renamed.
   * @param pNewName New name for the renamed elements.
   * @return Self-reference.
   */
  FxpDOMList renameAll(String pNewName);

  /**
   * Sets an attribute on all elements in the DOMLlist
   * @param pAttrName The name of the attribute to set.
   * @param pValue The value to set the attribute to.
   * @return Self-reference.
   */
  FxpDOMList setAttr(String pAttrName, String pValue);

  /**
   * Removes the first DOM item from the list and returns it. Synonymous with {@link List#remove}with 0 as the argument.
   * @return The first node in the list.
   */
  FxpDOM popHead();

  /**
   * Removes the nth DOM item from the list and returns it. Synonymous with {@link List#remove}.
   * @param pIndex Index of node to remove.
   * @return The nth node in the list.
   */
  FxpDOM removeFromList(int pIndex);

  /**
   * Removes all nodes with the given name from the DOMList.
   * @param pNodeName The name of the nodes to remove.
   * @return Self-reference.
   */
  FxpDOMList removeAllNamesFromList(String pNodeName);

  /**
   * Creates a deep clone of every node in this list and adds the results to the target parent. The list itself is
   * unchanged.
   * @param pTargetDOM Desired parent for cloned nodes.
   * @return A reference to the target DOM.
   */
  FxpDOM copyContentsTo(D pTargetDOM);

  /**
   * Moves every node in this list to the designated new parent. The list itself is unchanged.
   * @param pTargetDOM The new parent for all the nodes in this list.
   * @return A reference to the target DOM.
   */
  FxpDOM moveContentsTo(D pTargetDOM);

  /**
   * Prunes all the nodes in this list according to the logic defined by {@link DOM#pruneDocumentXPath}.
   * @param pPruneXPath XPath to use to prune nodes.
   * @return Self-reference.
   */
  FxpDOMList pruneDocumentXPath(String pPruneXPath);

  /**
   * Clones all the documents implicated by nodes in this list, and creates a new DOMList containing references
   * to the old nodes in the new document. Note the cloned documents will contain the same FOXIDs as the original documents.
   * @return A clone of this DOMList, with its nodes in cloned documents.
   */
  FxpDOMList cloneDocuments();

  /**
   * Gets the concatenated string-value of every node in this DOMList, using the same logic as defined in the XPath spec.
   * @return Recursive concatenation of the text values of all nodes in this list, in list order.
   */
  String value();

  /**
   * Coalesces the text nodes of all the nodes in this DOMList into a single List of string values.
   * @param pDeep If the text node retrieval should be recursive.
   * @return A List which may contain 0 items.
   */
  java.util.List<String> allChildTextNodesAsStringList(boolean pDeep);

  /**
   * Coalesces the text nodes of all the nodes in this DOMList into a single DOMList of text nodes.
   * @param pDeep If the text node retrieval should be recursive.
   * @return A List which may contain 0 items.
   */
  FxpDOMList allChildTextNodesAsDOMList(boolean pDeep);

  /**
   * Creates an XML fragment string representing the serialised contents of all the nodes in this list.
   * @return Fragment String.
   */
  String outputNodesToString();
}
