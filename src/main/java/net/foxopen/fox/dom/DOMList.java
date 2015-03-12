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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.foxopen.fox.ex.ExDOM;
import net.foxopen.fox.plugin.api.dom.FxpDOMList;

import nu.xom.Node;


/**
 * A DOMList represents a list of DOM nodes and is used when dealing with multiple nodes. Most of the methods on this
 * class allow bulk operations to be performed on the entire contents of the list.
 *
 */
public class DOMList
extends ArrayList<DOM> implements FxpDOMList<DOM> {

  /**
   * Constructs an empty DOMList with an initial capacity.
   * @param pInitialCapacity The initial capacity.
   */
  public DOMList(int pInitialCapacity){
    super(pInitialCapacity);
  }

  /**
   * Constructs an empty DOM list.
   */
  public DOMList() {
    super();
  }

  /**
   * Tests if a list containing arbitrary objects can be safely converted into a DOMList. This is true if the items
   * in the list are DOM objects or XOM Nodes. If the list is empty, this method returns true.
   * @param pList List to be tested.
   * @return True if the list can be converted to a DOMList.
   */
  public static boolean isValidDOMList(List pList) {
    for(int i = 0; i < pList.size(); i++) {
      Object o = pList.get(i);
      if(!(o instanceof Node || o instanceof DOM)) {
        return false;
      }
    }
    //Loop ended and all elements were OK
    return true;
  }

  /**
   * Constructs a new DOMList from a List which may contain either DOM objects or XOM Node objects.
   * @param pList Source List.
   * @throws ExDOM If the supplied List contains objects which are not DOMs or Nodes.
   */
  public DOMList(List pList)
  throws ExDOM {
    int l = pList.size();
    Object o;
    for (int n=0; n < l; n++) {
      o = pList.get(n);
      if(o instanceof Node) {
        add(new DOM((Node) o));
      }
      else if(o instanceof DOM) {
        add((DOM) o);
      }
      else {
        throw new ExDOM("DOMList(ArrayList) passed an invalid item (not DOM or Node), was a " + o.getClass().getName());
      }
    }
  }

  /**
   * Gets the number of nodes in this list. Synononymous with {@link List#size}.
   * @return Size of the list.
   */
  public int getLength() {
    return size();
  }

  /**
   * Returns the Nth Node in the list. Synononymous with {@link List#get}.
   * @param index Index of Node to retrive.
   * @return Node at index.
   */
  public DOM item(int index) {
    return get(index);
  }

  /**
  * Removes all the nodes in this DOMList from their DOM Tree Parent. The DOMList itself is unchanged.
  * @return Self-reference.
  */
  public DOMList removeFromDOMTree() {
    int l = size();
    for(int i=0; i<l; i++) {
      get(i).remove();
    }
    return this;
  }

  /**
   * Renames all the Elements in this DOMList to the given name. This is not recursive - only the top-level Element is
   * renamed.
   * @param pNewName New name for the renamed elements.
   * @return Self-reference.
   */
  public DOMList renameAll(String pNewName) {
    int l = size();
    for(int i=0; i<l; i++) {
      get(i).rename(pNewName);
    }
    return this;
  }

  /**
   * Sets an attribute on all elements in the DOMLlist
   * @param pAttrName The name of the attribute to set.
   * @param pValue The value to set the attribute to.
   * @return Self-reference.
   */
  public DOMList setAttr(String pAttrName, String pValue) {
    int l = size();
    for(int i=0; i<l; i++) {
     (get(i)).setAttr(pAttrName, pValue);
    }
    return this;
  }

  /**
   * Removes the first DOM item from the list and returns it. Synonymous with {@link List#remove} with 0 as the argument.
   * @return The first node in the list.
   */
  public final DOM popHead() {
    try {
      return remove(0);
    }
    catch(IndexOutOfBoundsException x) {
      return null;
    }
  }

  /**
   * Removes the nth DOM item from the list and returns it. Synonymous with {@link List#remove}.
   * @param pIndex Index of node to remove.
   * @return The nth node in the list.
   */
  public final DOM removeFromList(int pIndex) {
    return remove(pIndex);
  }

  /**
   * Removes all nodes with the given name from the DOMList.
   * @param pNodeName The name of the nodes to remove.
   * @return Self-reference.
   */
  public final DOMList removeAllNamesFromList(String pNodeName) {
    for(int j=size()-1; j>=0; j--) {
      if(get(j).getName().equals(pNodeName)) {
        remove(j);
      }
    }
    return this;
  }

  /**
   * Creates a deep clone of every node in this list and adds the results to the target parent. The list itself is
   * unchanged.
   * @param pTargetDOM Desired parent for cloned nodes.
   * @return A reference to the target DOM.
   */
  public final DOM copyContentsTo(DOM pTargetDOM) {
    int l = size();
    for(int i=0; i<l; i++) {
     get(i).copyToParent(pTargetDOM);
    }
    return pTargetDOM;
  }

  /**
   * Moves every node in this list to the designated new parent. The list itself is unchanged.
   * @param pTargetDOM The new parent for all the nodes in this list.
   * @return A reference to the target DOM.
   */
  public final DOM moveContentsTo(DOM pTargetDOM) {
    int l = size();
    for(int i=0; i<l; i++) {
      get(i).moveToParent(pTargetDOM);
    }
    return pTargetDOM;
  }

  /**
   * Prunes all the nodes in this list according to the logic defined by {@link DOM#pruneDocumentXPath}.
   * @param pPruneXPath XPath to use to prune nodes.
   * @return Self-reference.
   */
  public final DOMList pruneDocumentXPath(String pPruneXPath) {
    int l = size();
    for(int i=0; i<l; i++) {
      DOM lDOM = get(i);
      lDOM.pruneDocumentXPath(pPruneXPath);
    }
    return this;
  }

  /**
   * Clones all the documents implicated by nodes in this list, and creates a new DOMList containing references
   * to the old nodes in the new document. Note the cloned documents will contain the same FOXIDs as the original documents.
   * @return A clone of this DOMList, with its nodes in cloned documents.
   */
  public final DOMList cloneDocuments() {
    Map lDocControlToNewDocument = new HashMap();
    int len = size();
    DOMList lDOMList = new DOMList(len);
    for(int i=0; i<len; i++) {
      DOM lOldDOM = get(i);
      DocControl lDocControl = lOldDOM.getDocControl();
      DOM lNewDOM = (DOM) lDocControlToNewDocument.get(lDocControl);
      if(lNewDOM==null) {
        lNewDOM = lOldDOM.getRootElement().createDocument();
        lDocControlToNewDocument.put(lDocControl, lNewDOM);
      }
      lDOMList.add(lNewDOM.getElemByRef(lOldDOM.getRef()));
    }
    return lDOMList;
  }

  /**
   * Gets the concatenated string-value of every node in this DOMList, using the same logic as defined in the XPath spec.
   * @return Recursive concatenation of the text values of all nodes in this list, in list order.
   */
  public String value(){
    int l = size();
    StringBuilder lResult = new StringBuilder();
    for(int i=0; i<l; i++) {
      DOM lDOM = get(i);
      lResult.append(lDOM.value(true));
    }
    return lResult.toString();
  }

  /**
   * Coalesces the text nodes of all the nodes in this DOMList into a single List of string values.
   * @param pDeep If the text node retrieval should be recursive.
   * @return A List which may contain 0 items.
   */
  public List<String> allChildTextNodesAsStringList(boolean pDeep){
    int l = size();
    List<String> lResult = new ArrayList<String>();
    for(int i=0; i<l; i++) {
      lResult.addAll(get(i).childTextNodesAsStringList(pDeep));
    }
    return lResult;
  }

  /**
   * Coalesces the text nodes of all the nodes in this DOMList into a single DOMList of text nodes.
   * @param pDeep If the text node retrieval should be recursive.
   * @return A List which may contain 0 items.
   */
  public DOMList allChildTextNodesAsDOMList(boolean pDeep){
    int l = size();
    DOMList lResult = new DOMList();
    for(int i=0; i<l; i++) {
      lResult.addAll(get(i).childTextNodesAsDOMList(pDeep));
    }
    return lResult;
  }

  /**
   * Creates an XML fragment string representing the serialised contents of all the nodes in this list (pretty-printed).
   * @return Fragment String.
   */
  public String outputNodesToString() {
    StringBuilder lResult = new StringBuilder();
    for(DOM lDOM : this) {
      lResult.append(lDOM.outputNodeToString(true));
    }
    return lResult.toString();
  }

}
