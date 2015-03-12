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

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.SchemaUtils;
import net.foxopen.fox.entrypoint.servlets.FoxMainServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilderImpl;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.module.entrytheme.ThemeParam;
import net.foxopen.fox.module.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Generates a WSDL based on one or more module instances that provide
 * web-service entry-themes. The caller should determine the granularity of
 * the services exposed in this WSDL - all provided module references will be
 * used.
 */
public class WSDLGenerator {

  public static final String WSDL_NAMESPACE_URI = "http://schemas.xmlsoap.org/wsdl/";

  // Namespaces for service types that we wish to create bindings for
  public static final HashMap gWebServicesToSupport = new HashMap();
  static {
    // Map namespaces to namespace URIs (these namespaces are used to link to
    // WSDLBindingBuilder implementations
    gWebServicesToSupport.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
  }

  public static final HashMap gThemeTypeToBindingStyleMap = new HashMap();
  static {
    gThemeTypeToBindingStyleMap.put(EntryTheme.gTypeServiceDocument, WSDLBindingBuilder.BINDING_STYLE_DOCUMENT);
    gThemeTypeToBindingStyleMap.put(EntryTheme.gTypeServiceRPC, WSDLBindingBuilder.BINDING_STYLE_RPC);
  }

  // Used to generate namespaces
  public static final String BASE_NAMESPACE_URI = "http://www.fivium.co.uk/fox/webservices/";
  private static final String BASE_NAMESPACE_PREFIX = "ns";

  // Used to create references to bindings and messages in the global scope
  public static final String GLOBAL_NAMESPACE_PREFIX = "global";
  public static final String GLOBAL_NAMESPACE_URI = "http://www.fivium.co.uk/fox/webservices";

  private final HashMap mOperationToBindingStyleMap = new HashMap();
  private int nsCount = 1; // Count namespaces as we go


  /**
   * Generates a WSDL for the specified module.
   * @param pFoxRequest the fox request that we're generating for
   * @param pModule the module to generate a WSDL for
   * @return generated WSDL as DOM
   */
  public DOM generate (FoxRequest pFoxRequest, Mod pModule) {
    // Make sure the caller is sane
    if (pModule == null) {
      throw new ExInternal("Cannot generate a WSDL without a module.");
    }
    HashMap lWrapper = new HashMap();
    lWrapper.put(pModule.getName(), pModule);
    return generate(pFoxRequest, lWrapper);
  } // generate

  /**
   * Generates a WSDL for a list of modules that contain web service entry themes.
   * @param pFoxRequest the fox request that we're generating for
   * @param pModules HashMap of modules
   * @return generated WSDL as DOM
   */
  public DOM generate (FoxRequest pFoxRequest, HashMap pModules) {
    // Make sure the caller is sane
    if (!(pModules.values().size() > 0)) {
      throw new ExInternal("Cannot generate a WSDL without a module.");
    }

    // Prepare root
    DOM lWSDLRoot = DOM.createDocument("definitions", WSDL_NAMESPACE_URI, true);
    lWSDLRoot.addNamespaceDeclaration("xs", SchemaUtils.XML_SCHEMA_NAMESPACE_URI);
    lWSDLRoot.addNamespaceDeclaration(GLOBAL_NAMESPACE_PREFIX, GLOBAL_NAMESPACE_URI);

    lWSDLRoot.setAttr("targetNamespace", GLOBAL_NAMESPACE_URI);

    // Types definition block
    DOM lTypesDOM = lWSDLRoot.addElem("types");

    // Loop through modules and create portType definitions and message definitions
    Iterator lModIterator = pModules.values().iterator();
    LOOP_MODULES: while (lModIterator.hasNext()) {

      Mod lMod = (Mod) lModIterator.next();

      // Generate a namespace for the module and a unique prefix
      String lModNsPrefix = BASE_NAMESPACE_PREFIX + nsCount++;
      String lModNsURI = BASE_NAMESPACE_URI + lMod.getApp().getMnemonicName() + "/" + lMod.getName();

      // Set namespace declaration on WSDL
      lWSDLRoot.addNamespaceDeclaration(lModNsPrefix, lModNsURI);

      // Create an unconnected schema element in case we import types  - will be thrown away later if it has no child nodes
      // Note dummy container is required as we later move the xs:schema element and it's not possible to move the root element
      DOM lSchemaDOM = DOM.createDocument("xs:schema-container-dummy", SchemaUtils.XML_SCHEMA_NAMESPACE_URI, true).addElem("xs:schema");
      lSchemaDOM.setAttr("targetNamespace", lModNsURI);

      // Create a service block
      DOM lService = DOM.createUnconnectedElement("service");
      lService.setAttr("name", lMod.getName());
      lService.addElem("documentation", lMod.getDescription());

      DOMList lPortTypeList = new DOMList();
      DOMList lBindingList = new DOMList();

      LOOP_BINDING_STYLES: for(int k=0; k<WSDLBindingBuilder.gBindingStyleList.size(); k++) {

        String lBindStyle = (String) WSDLBindingBuilder.gBindingStyleList.get(k);

        DOM lPortType = DOM.createUnconnectedElement("portType");
        lPortType.setAttr("name", lMod.getName() + "_" + lBindStyle + "_PortType");
        boolean lPortTypeImplicated = false;

        // Loop through entry-themes and identify those that are web services
        List<EntryTheme> lEntryThemes = lMod.getEntryThemes();
        LOOP_ENTRY_THEMES: for (int e = 0; e < lEntryThemes.size(); e++) {

          EntryTheme lTheme = lEntryThemes.get(e);
          if (lTheme.isWebServiceOperation()) {

            // Maintain a map of operations and the binding styles they should use
            String lBindingStyle = (String) gThemeTypeToBindingStyleMap.get(lTheme.getType());
            if (lBindingStyle == null) {
              throw new ExInternal("Binding style not found for entry-theme type " + lTheme.getType());
            } else if (!lBindingStyle.equals(lBindStyle)) {
              continue LOOP_ENTRY_THEMES;
            }

            lPortTypeImplicated = true;

            mOperationToBindingStyleMap.put(lTheme.getName(), lBindingStyle);

            // Create operation entry
            DOM lOperation = lPortType.addElem("operation");
            lOperation.setAttr("name", lTheme.getName());

            // Get parameters and returns from module
            List<ThemeParam> lParamList = lTheme.getParamList();
            List<ThemeParam> lReturnList = lTheme.getReturnList();

            DOM lInput = lOperation.addElem("input");
            lInput.setAttr("name", lTheme.getName() + "Input");

            // Generate a composite message name to ensure that messages are
            // scoped within module (avoid problems caused by two modules with
            // same web-service theme name)
            String lMessageName = lMod.getName() + "." + lTheme.getName() + "Input";
            lInput.setAttr("message", GLOBAL_NAMESPACE_PREFIX + ":" + lMessageName);

            // Create the message and parts
            createMessage(lWSDLRoot, lSchemaDOM, lMessageName, lParamList, lModNsPrefix, lBindingStyle);

            // We're expecting to return data, add declarations to the operation
            if (lReturnList.size() > 0) {
              DOM lOutput = lOperation.addElem("output");
              lOutput.setAttr("name", lTheme.getName() + "Output");

              // Generate a composite message name to ensure that messages are
              // scoped within module (avoid problems caused by two modules with
              // same web-service theme name)
              String lOutputMessageName = lMod.getName() + "." + lTheme.getName() + "Output";
              lOutput.setAttr("message", GLOBAL_NAMESPACE_PREFIX + ":" + lOutputMessageName);

              // Create the message and parts
              createMessage(lWSDLRoot, lSchemaDOM, lOutputMessageName, lReturnList, lModNsPrefix, lBindingStyle);
            }

          } // if (service)

        } // LOOP_ENTRY_THEMES

        // If we've included XML schema types inline, retain this fragment
        // otherwise move on to the next module
        if (lSchemaDOM.getChildNodes().getLength() > 0) {
          // Clean up non-schema attributes
          SchemaUtils.removeNsPrefixAttrs(lSchemaDOM);

          // Clean up foxids
          lSchemaDOM.removeRefsRecursive();

          // Move it
          lSchemaDOM.moveToParent(lTypesDOM);
        }


        // Create binding for this portType (module) for each web service we
        // wish to support
        if(lPortTypeImplicated){
          Iterator i = gWebServicesToSupport.entrySet().iterator();
          LOOP_SERVICE_TYPES: while (i.hasNext()) {
            // Pull values from map
            Map.Entry lMapEntry = (Map.Entry) i.next();
            String lNsPrefix = (String) lMapEntry.getKey();
            String lNsURI = (String) lMapEntry.getValue();

            // Create the binding
            WSDLBindingBuilder lBindingBuilder = WSDLBindingBuilder.getWSDLBindingBuilder(lNsURI);
            DOM lBinding = lBindingBuilder.createWSDLBinding(lNsPrefix, lPortType, lModNsURI, GLOBAL_NAMESPACE_PREFIX, lModNsURI, mOperationToBindingStyleMap, lBindStyle);

            // Store the binding in our list to add to WSDL
            if(lBinding.getChildNodes().getLength() > 0) {
              lBindingList.add(lBinding);
            }


            // Create port reference in service
            DOM lPort = lService.addElem("port");
            lPort.setAttr("name", lMod.getName() + "_" + lBindStyle + "_" + lNsPrefix + "Port");
            lPort.setAttr("binding", GLOBAL_NAMESPACE_PREFIX + ":" + lBinding.getAttr("name"));
            //So soap attributes can be set on this fragment
            lPort.addNamespaceDeclaration(lNsPrefix, lNsURI);

            // Set endpoint address
            DOM lAddr = lPort.addElem(lNsPrefix + ":address");

            String lEntryURI = FoxMainServlet.buildGetEntryURI(RequestURIBuilderImpl.createFromFoxRequest(pFoxRequest), lMod.getApp().getMnemonicName(), lMod.getName());
            lAddr.setAttr("location",  lEntryURI);

            // Ensure namespaces are added to WSDL root
            lWSDLRoot.addNamespaceDeclaration(lNsPrefix, lNsURI);

          }// LOOP_SERVICE_TYPES
        } //lPortTypeImplicated

        // If the port type isn't empty, store it in a list for adding to WSDL later
        if(lPortType.getChildNodes().getLength() > 0) {
          lPortTypeList.add(lPortType);
        }

      } // LOOP_BINDING_STYLES

      lPortTypeList.copyContentsTo(lWSDLRoot);
      lBindingList.copyContentsTo(lWSDLRoot);

      // Move service to the WSDL
      lService.moveToParent(lWSDLRoot);

      // Clear expanded types
      DOMList lExpandedTypesToRemove;
      try {
        lExpandedTypesToRemove = lSchemaDOM.xpathUL("//*[@type]/*[name(.)='xs:complexType' or name(.)='xs:simpleType']", null);
        lExpandedTypesToRemove.removeFromDOMTree();
      }
      catch (ExBadPath ex) {
        throw new ExInternal("Failed to clear expanded types from xs:element", ex);
      }

    } // LOOP_MODULES

    return lWSDLRoot;

  } // generate

  /**
   * Creates a WSDL message element and adds it to the root of the WSDL (also creating required parts)
   * @param pWSDL the WSDL root element to target
   * @param pSchemaDOM the current xs:schema element within the WSDL
   * @param pMessageName name for message element
   * @param pParamsList the list of parameters required for the message (to be converted to WSDL parts)
   * @param pTNSPrefix the prefix of the target namespace for the type mappings
   * @param pBindingStyle the style of binding to use
   */
  private void createMessage (DOM pWSDL, DOM pSchemaDOM, String pMessageName, List<ThemeParam> pParamsList, String pTNSPrefix, String pBindingStyle) {
    // Construct message element
    DOM lMessageDOM = pWSDL.addElem("message");
    lMessageDOM.setAttr("name", pMessageName);

    // Loop through params
    for (int i = 0; i < pParamsList.size(); i++) {

      // For each param, construct a part
      ThemeParam lThemeParams = pParamsList.get(i);
      DOM lPartDOM = lMessageDOM.addElem("part");
      lPartDOM.setAttr("name", lThemeParams.getName());

      // Get the type declaration
      String lType = lThemeParams.getType();
      String lTypeWithNs = (!lType.startsWith("xs:") ? pTNSPrefix + ":" + lType : lType);

      // Document style requires element notation instead of type notation
      if (pBindingStyle.equals(WSDLBindingBuilder.BINDING_STYLE_DOCUMENT)) {
        // Workaround to stop errors on WSDL importing, reference xs:any in part directly
        // instead of in the schema
        if(lThemeParams.getType().equals("xs:any")) {
          lPartDOM.setAttr("element", "xs:any");
        } else {
          lPartDOM.setAttr("element", pTNSPrefix + ":" + lThemeParams.getName());
          DOM lDOM = pSchemaDOM.addElem("xs:element");
          lDOM.setAttr("name", lThemeParams.getName());
          lDOM.setAttr("type", lTypeWithNs);
        }
      }
      else if (pBindingStyle.equals(WSDLBindingBuilder.BINDING_STYLE_RPC)) {
        // If it's a schema type then let through, otherwise, prefix with target namespace prefix
        lPartDOM.setAttr("type", lTypeWithNs);
      }
      else {
        throw new ExInternal("Invalid binding style detected in WSDLGenerator.createMessage");
      }

      // Check to see if schema already has this type imported
      DOMList lTypeExistsCheckUL = null;
      try {
        lTypeExistsCheckUL = pSchemaDOM.xpathUL("./*[@name='" + lType + "']", null);
      }
      catch (ExBadPath ex) {
        throw new ExInternal("Could not check whether or not type definition had been imported into schema", ex);
      }

      // We have a declared type, we need to bring this into the WSDL
      // and recursively check for any dependencies to include
      if (lThemeParams.hasTypeDOM() && lTypeExistsCheckUL.getLength() == 0) {

        // Get a reference
        DOM lTypeDOM = lThemeParams.getTypeDOM();

        // Check that we don't already have this type in this schema
        if (pSchemaDOM.get1EOrNull("./" +  lThemeParams.getName()) == null) {

          // Copy forward the schema type fragment
          lTypeDOM = lTypeDOM.copyToParent(pSchemaDOM);

          // Just in time shunt name onto type definition
          lTypeDOM.setAttr("name", lType);

          // Resolve types referenced by this type fragment and insert target namespace prefix on references
          // Note: This copies required types to parent of first argument
          SchemaUtils.resolveSchemaTypeDependencies(lTypeDOM, lThemeParams.getTypeDOM(), pTNSPrefix);
        }
      }
    }
  } // createMessage

} // WSDLGenerator
