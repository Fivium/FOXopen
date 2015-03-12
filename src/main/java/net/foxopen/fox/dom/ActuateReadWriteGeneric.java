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

import net.foxopen.fox.ex.ExDOM;
import net.foxopen.fox.ex.ExInternal;

import nu.xom.Element;
import nu.xom.IllegalNameException;
import nu.xom.NamespaceConflictException;
import nu.xom.Node;


//PN TODO XOM - this class is fairly useless; refactor into ActuateReadWrite?

/**
 * This abstract class defines generic methods that can be inherited by
 * ActuateReadWrite and ActuateTransactionReadWrite.
 *
 * The important thing is, these methods do not change the DOM directly,
 * instead, they use ActuateReadWrite and ActuateTransactionReadWrite
 * override methods to do DOM change work.
 */

abstract class ActuateReadWriteGeneric
extends ActuateReadOnly
{

  ActuateReadWriteGeneric(String pAccessViolationInfo, DocControl pDocControl) {
   super(pAccessViolationInfo, pDocControl);
  }

  /**
   * Moves all the children of pNode into pNewParent. pNode is left as is.
   * @param pNode The Node to copy children from.
   * @param pNewParent The new parent for pNode's children.   
   */
  public void moveContentsTo(Node pNode, Node pNewParent) {
    //if no children, do nothing
    if(pNode.getChildCount() == 0){
      return;
    }
    
    //This is a LIVE list so read off the front until it's depleted
    Node lChild;
    while((lChild = (pNode.getChild(0))) != null) {
      moveToParent(lChild, pNewParent);
    }
  }    

  /**
   * Copies all the children of pNode into pNewParent. pNode is left as is.
   * @param pNode The Node to copy children from.
   * @param pNewParent The new parent for pNode's children.
   * @param pResetRefs If true, foxids will be reset.
   */
  public void copyContentsTo(Node pNode, Node pNewParent, boolean pResetRefs){
    for(int i=0; i<pNode.getChildCount(); i++){
      Node lChild = pNode.getChild(i);
      copyToParent(lChild, pNewParent, pResetRefs);
    }
  }

  /**
   * Rename the Element pNode to the name specified in pNewName. 
   * pNewName can specify a namespace prefix (e.g. "fm:then") if needed, as long as the namespace is defined on or above
   * the Element.
   * @param pNode The Element to rename.
   * @param pNewName The new name.   
   */
  public void renameElement(Node pNode, String pNewName) {
    // Cast to Element (checks is an element)
    Element lElement; 
    try {
      lElement = (Element)pNode;
    } 
    catch (ClassCastException x) {
      throw new ExInternal("Not an Element", x);
    }
    
    //If the new name has a namespace prefix, we need to set that seperately
    try {
      int lIdx;
      if((lIdx = pNewName.indexOf(':')) > 0){
        lElement.setNamespacePrefix(pNewName.substring(0, lIdx-1));
        lElement.setLocalName(pNewName.substring(lIdx));
      } 
      else {
        lElement.setLocalName(pNewName);
      }
    } 
    catch (IllegalNameException e) {
      throw new ExDOM("Failed to rename node - invalid name '" + pNewName + "'", e);
    }
    catch (NamespaceConflictException e) {
      throw new ExDOM("Failed to rename node - invalid name '" + pNewName + "'", e);
    }
  }

}
