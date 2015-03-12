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

import java.util.Map;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExInternal;


/**
 * WSDL Binding builder for SOAP. Fills in the implementation-specific
 * parts of the WSDL, based on the operations that we're providing in
 * the port specification.
 */
public class WSDLBindingBuilderSOAP extends WSDLBindingBuilder
{
  public static final String NAMESPACE_URI = "http://schemas.xmlsoap.org/wsdl/soap/";
  public static final String SOAP_TRANSPORT = "http://schemas.xmlsoap.org/soap/http";
      
  // We're only supporting literal style bindings, encoded is not WS-I compliant
  public static final String SOAP_BODY_USE = "literal";
  
  /**
   * Creates a valid WSDL binding based on the PortType provided, using the
   * specified namespace. This method makes the assumption that the PortType
   * provided does not have namespace prefixes on WSDL elements.
   * @param pNsPrefix the namespace prefix to use
   * @param pPortTypeDOM the port type on which to base the binding
   * @param pHintBaseURI the base URI to use when generating operation hints
   * @param pTargetNsPrefix the prefix to use when referencing the binding
   * @param pTargetNsURI the target namespace to use in the binding
   * @param pOperationToBindingStyleMap a map of operation names to binding style
   * @param pBindingStyle the style of the binding and which operations to include
   * @return a WSDL binding as a DOM
   */
  public DOM createWSDLBinding(String pNsPrefix, DOM pPortTypeDOM, String pHintBaseURI, String pTargetNsPrefix, String pTargetNsURI, Map pOperationToBindingStyleMap, String pBindingStyle) {
  
    // Checks
    if (pPortTypeDOM == null) {
      throw new ExInternal("Null PortType passed to WSDLBindingBuilderSOAP.createWSDLBinding");
    }
    if (XFUtil.isNull(pHintBaseURI)) {
      throw new ExInternal("No hint base URI passed to WSDLBindingBuilderSOAP.createWSDLBinding ");
    }
    
    // Check name and store it for reuse
    String lPortTypeName = pPortTypeDOM.getAttr("name");
    if (XFUtil.isNull(lPortTypeName)) {
      throw new ExInternal("Null PortType name found in WSDLBindingBuilderSOAP.createWSDLBinding");
    }
    
    // This is making the assumption that it will be inserted into a WSDL document with no namespace
    DOM lBindingDOM = DOM.createDocument("binding");
    lBindingDOM.setAttr("name", lPortTypeName + "Binding");
    lBindingDOM.setAttr("type", (XFUtil.isNull(pTargetNsPrefix) ? "" : pTargetNsPrefix + ":") + lPortTypeName);
    lBindingDOM.addNamespaceDeclaration(pNsPrefix, NAMESPACE_URI);
    
    // Create SOAP Binding
    DOM lSOAPBinding = lBindingDOM.addElem(pNsPrefix + ":binding");
    lSOAPBinding.setAttr("transport", SOAP_TRANSPORT);
    lSOAPBinding.setAttr("style", pBindingStyle);
    
    // Loop through WSDL operations and expand them
    DOMList lOperations = pPortTypeDOM.getUL("./operation");
    OPERATION_LOOP: for (int i = 0; i < lOperations.getLength(); i++) {
    
      // Get mandatory operation name
      DOM lWSDLOperation = lOperations.item(i);
      String lOperationName = lWSDLOperation.getAttr("name");
      if (XFUtil.isNull(lOperationName)) {
        throw new ExInternal("Null or empty name attribute found in WSDL operation when processing WSDLBindingBuilderSOAP.createWSDLBinding");
      }
      
      // Get mandatory binding style
      String lBindingStyle = (String) pOperationToBindingStyleMap.get(lOperationName);
      if (XFUtil.isNull(lBindingStyle)) {
        throw new ExInternal("No binding style found for operation '" + lOperationName + "' in WSDLBindingBuilderSOAP.createWSDLBinding");
      }
      
      // Create a corresponding binding operation
      DOM lBindingOperation = lBindingDOM.addElem("operation");
      lBindingOperation.setAttr("name", lOperationName);
      
      // Create child SOAP operation
      DOM lSOAPOperation = lBindingOperation.addElem(pNsPrefix + ":operation");
      lSOAPOperation.setAttr("soapAction", pHintBaseURI + "/" + lOperationName);
      lSOAPOperation.setAttr("style", lBindingStyle);
      
      // Create operation input (if exists)
      DOM lWSDLInput = lWSDLOperation.get1EOrNull("input");
      if (lWSDLInput != null ) {
        generateWSDLOperationChildNode(pNsPrefix, lWSDLInput, pTargetNsURI, lBindingStyle).moveToParent(lBindingOperation);
      }
      
      // Create operation output (if exists)
      DOM lWSDLOutut = lWSDLOperation.get1EOrNull("output");
      if (lWSDLOutut != null ) {
        generateWSDLOperationChildNode(pNsPrefix, lWSDLOutut, pTargetNsURI, lBindingStyle).moveToParent(lBindingOperation);
      }

    } // OPERATION_LOOP
    
    return lBindingDOM;
  } // createWSDLBinding

  /**
   * Reusable method to create the child nodes of an operation.
   * @param pNsPrefix the base prefix to use
   * @param pPortTypeOperationChildNode the prototype operation to work from
   * @param pTargetNsURI the target namespace to use
   * @param pBindingStyle the operation style to determine requirement of namespace
   * @return a generated SOAP operation child node
   */
  private DOM generateWSDLOperationChildNode (String pNsPrefix, DOM pPortTypeOperationChildNode, String pTargetNsURI, String pBindingStyle) {

    // Get mandatory input name
    String lNameAttr = pPortTypeOperationChildNode.getAttr("name");
    if (XFUtil.isNull(lNameAttr)) {
      throw new ExInternal("Null or empty name attribute found in WSDL input when processing WSDLBindingBuilderSOAP.createWSDLBinding");
    }
    DOM lOpChildNode = DOM.createUnconnectedElement(pPortTypeOperationChildNode.getLocalName());
    lOpChildNode.setAttr("name", lNameAttr);

    // Add the soap body
//    DOM lSOAPBody = DOM.createUnconnectedElement(pNsPrefix + ":body", NAMESPACE_URI);
//    lSOAPBody.moveToParent(lOpChildNode);
    DOM lSOAPBody = lOpChildNode.addElemWithNamespace(pNsPrefix + ":body", NAMESPACE_URI);
    lSOAPBody.setAttr("use", SOAP_BODY_USE);
    
    // WS-I R2716 - document-literal bindings should not have namespace attributes
    // WS-I R2717 - rpc-literal bindings must have the namespace attribute
    if(pBindingStyle.equals(BINDING_STYLE_RPC)) {
      lSOAPBody.setAttr("namespace", pTargetNsURI);
    }
    
    return lOpChildNode;
  } // generateWSDLOperationChildNode
    
} // WSDLBindingBuilderSOAP
