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
package net.foxopen.fox.webservices;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;

public abstract class WSDLBindingBuilder
{
  // Binding style constants
  public static final String BINDING_STYLE_RPC = "rpc";
  public static final String BINDING_STYLE_DOCUMENT = "document";

  // List of styles for iterating through
  public static List gBindingStyleList = new ArrayList();
  static {
    gBindingStyleList.add(BINDING_STYLE_RPC);
    gBindingStyleList.add(BINDING_STYLE_DOCUMENT);
  }
  
  // Holds mapping of namespace URIs to WSDL Binding Builders
  private static HashMap mNamespaceURIToBuilderMap = new HashMap();
  static {
    mNamespaceURIToBuilderMap.put(WSDLBindingBuilderSOAP.NAMESPACE_URI, new WSDLBindingBuilderSOAP());
  }

  /**
   * Returns an instance of a WSDLBindingBuilder suitable for the namespace
   * provided.
   * @param pNamespaceURI the namespace to resolve to a WSDLBindingBuilder
   * @return instance of WSDLBindingBuilder
   */
  public static WSDLBindingBuilder getWSDLBindingBuilder (String pNamespaceURI) {
    WSDLBindingBuilder lTemp = (WSDLBindingBuilder) mNamespaceURIToBuilderMap.get(pNamespaceURI);
    if (lTemp == null) {
      throw new ExInternal("No WSDLBindingBuilder implementation found for namespace URI '" + pNamespaceURI + "'");
    }
    return lTemp;
  } // getWSDLBindingBuilder
  
  /**
   * Creates a valid WSDL binding based on the PortType provided, using the
   * specified namespace.
   * @param pNsPrefix the namespace prefix to use
   * @param pPortTypeDOM the port type on which to base the binding
   * @param pHintBaseURI the base URI to use when generating operation hints
   * @param pTargetNsPrefix the prefix to use when referencing the binding
   * @param pTargetNsURI the target namespace to use in the binding
   * @param pOperationToBindingStyleMap a map of operation names to binding style
   * @param pBindingStyle the style of the binding and which operations to include 
   * @return a WSDL binding as a DOM
   */
  public abstract DOM createWSDLBinding (String pNsPrefix, DOM pPortTypeDOM, String pHintBaseURI, String pTargetNsPrefix, String pTargetNsURI, Map pOperationToBindingStyleMap, String pBindingStyle);  
  
}
