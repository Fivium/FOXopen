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
package net.foxopen.fox.dom.xpath.saxon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

import net.foxopen.fox.dom.DOM;

import nu.xom.Element;


/**
 * Namespace context support for a DOM with arbitrary namespaces defined. This should only be used for XPath processing
 * of externally provided XML within the FOX engine. <br><br>
 *
 * For FOX internal DOMs the {@link DefaultNamespaceContext} should suffice as it does not incur the overhead of reading
 * namespaces from a document or maintaining Maps.
 */
public class DynamicNamespaceContext
implements NamespaceContext {
  
  private Map<String, String> mPrefixToURIMap = new HashMap<String, String>();
  private Map<String, String> mURIToPrefixMap = new HashMap<String, String>();
  
  public void addNamespace(String pPrefix, String pURI){
    mPrefixToURIMap.put(pPrefix, pURI);
    mURIToPrefixMap.put(pURI, pPrefix);
  }
  
  public String getPrefix(String pURI){
    return mURIToPrefixMap.get(pURI);
  }
  
  public String getNamespaceURI(String pPrefix){
    return mPrefixToURIMap.get(pPrefix);
  }

  public Iterator getPrefixes(String namespaceURI) {
    return null;
  }
  
  /**
   * Creates a new DynamicNamespaceContext for the given Element DOM. Note that the underlying XOM implentation
   * will only expose namespaces defined on or above this node. A full-traversal search through the whole DOM
   * would be possible but has not been implemented.
   * @param pDOM The element to read namespace definitions from.   
   */
  public DynamicNamespaceContext(DOM pDOM){    
    Element lElement = (Element) pDOM.getNode();
    
    int n = lElement.getNamespaceDeclarationCount();
    for(int i = 0; i < n; i++){
      String lPrefix = lElement.getNamespacePrefix(i);
      String lURI = lElement.getNamespaceURI(lPrefix);
      
      addNamespace(lPrefix, lURI);     
    }
  }
}
