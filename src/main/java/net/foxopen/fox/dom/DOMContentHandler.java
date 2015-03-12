/*

Copyright (c) 2012, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE - 
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

import net.foxopen.fox.ex.ExInternal;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;


/**
 * A ContentHandler for constructing a FOX DOM in a SAX-compatible way. See the {@link ContentHandler} documentation for a
 * description of the interface's method contracts.
 * <br/><br/>
 * This object is not thread safe and should be discarded after use.
 */
public class DOMContentHandler 
implements ContentHandler{

  /**
   * The current element being inserted into.
   */
  private DOM mCurrentDOM;
  
  /**
   * The optional Locator for reporting location information from the source document.
   */
  private Locator mLocator;
  
  /**
   * Create a new ContentHandler which will use pDestinationDOM as the intitial insertion point. This must be an Element.
   * Typically the element would be empty but it does not necessarily have to be.
   * @param pDestinationDOM The initial Element for this ContentHandler.
   */
  public DOMContentHandler(DOM pDestinationDOM){
    if(!pDestinationDOM.isElement()){
      throw new ExInternal("pDestinationDOM must be an element, but was a " + pDestinationDOM.nodeType().toString());
    }
    mCurrentDOM = pDestinationDOM;   
  }

  public void setDocumentLocator(Locator pLocator) {
    mLocator = pLocator;
  }

  public void startDocument() {
    if(mCurrentDOM == null){
      throw new ExInternal("ContentHandler has started to process document but mCurrentDOM was null.");
    }
  }

  public void endDocument() {
  }

  public void startPrefixMapping(String pPrefix, String pURI) {
    mCurrentDOM.addNamespaceDeclaration(pPrefix, pURI);
  }

  public void endPrefixMapping(String pPrefix) {
  }

  public void startElement(String pURI, String pLocalName, String pQName, Attributes pAttrs) {
    
    //Create a new Element.
    DOM lNewElem = mCurrentDOM.addElem(pQName);
    
    //Add attributes.
    for(int i=0; i < pAttrs.getLength(); i++){
      lNewElem.setAttr(pAttrs.getQName(i),  pAttrs.getValue(i));      
    }
    
    //Set the new element as the current element for future insertions.
    mCurrentDOM = lNewElem;
  }

  public void endElement(String pURI, String pLocalName, String pQName) {
    //Jump up one level in the tree
    mCurrentDOM = mCurrentDOM.getParentOrNull();
  }

  public void characters(char[] pCharacters, int pStart, int pLength) {
    //PN TODO this is not currently handling high-range (>16 bit) characters - see TRAX API doco
    mCurrentDOM.appendText(new String(pCharacters, pStart, pLength));
  }

  public void ignorableWhitespace(char[] pCharacters, int pStart, int pLength) {
  }

  public void processingInstruction(String pTarget, String pData) {
    mCurrentDOM.addPI(pTarget, pData);
  }

  public void skippedEntity(String pName) {
  }
  
}
