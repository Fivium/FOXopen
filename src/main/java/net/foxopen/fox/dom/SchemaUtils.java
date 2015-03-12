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
import java.util.HashSet;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;


/**
 * Utility methods for working with XML Schemas.
 */
public class SchemaUtils {
  public static final String XML_SCHEMA_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema";

  /**
   * Builds a minimal schema (resolving dependencies) from a schema fragment
   * and creates an an element instance for validation of the type provided.
   * @param pTypeName the type name to use
   * @param pElementName the name to use when creating the type-based element
   * @return built minimal schema
   */
  public static DOM buildSchemaForType (String pTypeName, String pElementName) {

    DOM lSchemaRoot = DOM.createUnconnectedElement("xs:schema", XML_SCHEMA_NAMESPACE_URI).createDocument();

    // Instantiate an instance of the type definition using the caller-provided element name
    // TODO: Remove hardcoded xs prefix when DOM can deal with namespaces
    DOM lSimpleElem = lSchemaRoot.addElem("xs:element");
    lSimpleElem.setAttr("name", pElementName);
    lSimpleElem.setAttr("type", pTypeName);

    return lSchemaRoot;

  } // buildSchemaForType

  /**
   * Builds a minimal schema (resolving dependencies) from a schema fragment
   * and creates an an element instance for validation of the type provided.
   * @param pDOMFragment the schema type fragment to use
   * @param pElementName the name to use when creating the type-based element
   * @return built minimal schema
   */
  public static DOM buildSchemaFromFragment (DOM pDOMFragment, String pElementName ) {
    return buildSchemaFromFragment (pDOMFragment, null, pElementName);
  }

  /**
   * Builds a minimal schema (resolving dependencies) from a schema fragment
   * and creates an an element instance for validation of the type provided.
   * @param pDOMFragment the schema type fragment to use
   * @param pTypeName override the type name if missing from the fragment
   * @param pElementName the name to use when creating the type-based element
   * @return built minimal schema
   */
  public static DOM buildSchemaFromFragment (DOM pDOMFragment, String pTypeName, String pElementName ) {

    DOM lSchemaRoot = DOM.createDocument("xs:schema", XML_SCHEMA_NAMESPACE_URI, true);

    // Sanity check the caller
    if (pDOMFragment == null) {
      throw new ExInternal("Cannot build schema from a null DOM fragment");
    }

    // Check document is in the right namespace
    if (!XML_SCHEMA_NAMESPACE_URI.equals(pDOMFragment.getNamespaceURI())) {
      throw new ExInternal("SchemaUtils.buildSchemaFromFragment expects to work on XML Schema documents only.");
    }

    // Copy forward the schema type fragment
    DOM lTypeDOMCopy = pDOMFragment.copyToParent(lSchemaRoot);

    // We should have a complexType or simpleType element
    if (!lTypeDOMCopy.getLocalName().equals("complexType") && !lTypeDOMCopy.getLocalName().equals("simpleType")) {
      throw new ExInternal("SchemaUtils.buildSchemaFromFragment expects to work on xs:complexType or xs:simpleType nodes only");
    }

    // Get the type name
    String lTypeName = lTypeDOMCopy.getAttrOrNull("name");
    if (XFUtil.isNull(lTypeName) && XFUtil.isNull(pTypeName)) {
      throw new ExInternal("Missing name attribute on simpleType or complexType element and no override type name specified");
    }
    else if (XFUtil.isNull(lTypeName)) {
      lTypeName = pTypeName;
      lTypeDOMCopy.setAttr("name", pTypeName);
    }

    // Resolve types referenced by this type fragment
    // and insert target namespace prefix on references
    resolveSchemaTypeDependencies(lTypeDOMCopy, pDOMFragment);

    // Instantiate an instance of the type definition using the caller-provided element name
    // TODO: Remove hardcoded xs prefix when DOM can deal with namespaces
    DOM lSimpleElem = lSchemaRoot.addElem("xs:element");
    lSimpleElem.setAttr("name", pElementName);
    lSimpleElem.setAttr("type", lTypeName);

    // Clean up foxids
    lSchemaRoot.removeRefsRecursive();

    // Clear unwanted attributes (i.e. fox markup)
    DOMList lSchemaChildNodes = lSchemaRoot.getChildElements();
    for (int i = 0; i < lSchemaChildNodes.getLength(); i++) {
      removeNsPrefixAttrs(lSchemaChildNodes.item(i));
    }

    // Clear expanded types
    DOMList lExpandedTypesToRemove;
    try {
      lExpandedTypesToRemove = lSchemaRoot.xpathUL("//*[@type]/*[name(.)='xs:complexType' or name(.)='xs:simpleType']", null);
      lExpandedTypesToRemove.removeFromDOMTree();
    }
    catch (ExBadPath ex) {
      throw new ExInternal("Failed to clear expanded types from xs:element", ex);
    }

    return lSchemaRoot;

  } // buildSchemaFromFragment

  /**
   * Resolves schema type dependencies from a DOM downwards and returns a
   * DOMList containing the type declaration fragments. Ignores XML schema
   * data types and assumes support exists for them.
   * @param pTypeDOM target xs:schema element for which to resolve dependencies
   * @return list of DOM fragments for each required type
   */
  public static void resolveSchemaTypeDependencies (DOM pTypeDOM, DOM pReferenceTypeDOM) {
    resolveSchemaTypeDependencies (pTypeDOM, pReferenceTypeDOM, null);
  } // resolveSchemaTypeDependencies

  /**
   * Resolves schema type dependencies from a DOM downwards and returns a
   * DOMList containing the type declaration fragments. Ignores XML schema
   * data types and assumes support exists for them.
   * @param pTypeDOM target xs:schema element for which to resolve dependencies
   * @param pTargetNsPrefix the target namespace prefix of the schema
   *        (which should be prepended to type usages)
   * @return list of DOM fragments for each required type
   */
  public static void resolveSchemaTypeDependencies (DOM pTypeDOM, DOM pReferenceTypeDOM, String pTargetNsPrefix) {

    // Get all type attributes
    DOMList lTypesRequired;
    DOMList lReturnDOMList = new DOMList();
    HashSet lHashSet = new HashSet();

    try {
      // Look for types that do not have a namespace
      // TODO: If we need support for externally referenced types by namespace,
      // we should get all the namespace prefixes for a document, see if they
      // are used, then shunt any used namespaces into the WSDL definition
      lTypesRequired = pTypeDOM.xpathUL(".//*[not(contains(@type,':')) and not(contains(@ref,':'))]/@*[name(.)='type' or name(.)='ref' or name(.)='base']", null);
    }
    catch (ExBadPath ex) {
      throw new ExInternal("SchemaUtils attempted to use an invalid XPath when getting type attributes from a schema", ex);
    }

    // Step through types that we will consider for inlining into the WSDL types schema
    TYPE_LOOP: for (int i = 0; i < lTypesRequired.getLength(); i++) {

      // Error for non-attribute nodes
      if (lTypesRequired.item(i).nodeType() != DOM.NodeType.ATTRIBUTE) {
        throw new ExInternal("Non-attribute node found in SchemaUtils type resolver.");
      }

      // Get the type as a string
      String lType = lTypesRequired.item(i).value();
      String lQualifiedType = !XFUtil.isNull(pTargetNsPrefix) ? pTargetNsPrefix + ":" + lType : lType;

      // XML Schema type, ignore
      // TODO: Remove hardcoded xs prefix when DOM can deal with namespaces
      if (lType.startsWith("xs:")) {
        continue;
      }

      // Prepend reference with target namespace prefix if provided
      if (!XFUtil.isNull(pTargetNsPrefix)) {
        lTypesRequired.item(i).setText(lQualifiedType);
      }

      // If we've already resolved this type, no need to refetch
      if (lHashSet.contains(lType)) {
        continue;
      };

      // Otherwise, we need to look in the schema
      DOM lResolvedType;
      try {
        // TODO: Remove hardcoded xs prefix when DOM can deal with namespaces
        lResolvedType = pReferenceTypeDOM.getRootElement().xpath1E("/*/*[(name(.)='xs:complexType' or name(.)='xs:simpleType' or name(.)='xs:group') and @name='" + lType + "']");
      }
      catch (ExCardinality ex) {
        throw new ExInternal("SchemaUtils could not locate type definition with name " + lType, ex);
      }
      catch (ExBadPath ex) {
        throw new ExInternal("SchemaUtils generated an invalid XPath when seeking to resolve type dependencies", ex);
      }

      // Make sure we have a valid parent for the type that we're fixing up
      DOM lParent = pTypeDOM.getParentOrNull();
      if (lParent == null) {
        throw new ExInternal("Schema fragment should not have null parent");
      }

      // Check that the parent doesn't already have the type we've found
      DOM lExistingType = null;
      try {
        lExistingType = lParent.xpath1E("./*[@name='" + lType + "']");
      }
      catch (ExBadPath ex) {
        throw new ExInternal("SchemaUtils attempted to use an invalid XPath when checking for an existing type definition", ex);
      }
      catch (ExTooFew ex) {
        // This is actually what we want
      }
      catch (ExTooMany ex) {
        throw new ExInternal("SchemaUtils found more than one resolved type instance, expected zero or one");
      }

      // We don't have the type already in the parent schema, proceed
      if (lExistingType == null) {
        try {
          // Shunt resolved type to parent of type DOM that we're allowed to write to
          // so that dependencies are inline with requiree
          lResolvedType = lResolvedType.copyToParent(lParent);

          // Fix up any dependencies of referenced types and add those to the list as well
          // (Simply add it into the list that we're processing)
          lTypesRequired.addAll(lResolvedType.xpathUL(".//*[not(contains(@type,':')) and not(contains(@ref,':'))]/@*[name(.)='type' or name(.)='ref' or name(.)='base']", null));
        }
        catch (ExBadPath ex) {
          throw new ExInternal("SchemaUtils attempted to use an invalid XPath when getting type attributes from a schema", ex);
        }

        // Add type to list of types we've resolved
        lReturnDOMList.add(lResolvedType);
      }

      // In any case, we don't need to reprocess this type
      lHashSet.add(lType);

    } // TYPE_LOOP

  } // resolveSchemaTypeDependencies


   /**
    * Remove namespace-prefixed attributes.
    * @param pDOM the highest point in the DOM heirarchy to process
    */
   public static void removeNsPrefixAttrs (DOM pDOM) {
     // Get a list of all nested elements
     // (avoid recursion by making XPath do the work)
     DOMList lElemsToProcess = pDOM.getAllNestedElements();

     // Make sure parent is processed too
     lElemsToProcess.add(pDOM);

     ELEM_LOOP: for (int i = 0; i < lElemsToProcess.getLength(); i++) {

       // Ignore non-elements
       if (lElemsToProcess.item(i).nodeType() != DOM.NodeType.ELEMENT) {
         continue;
       }

       // Pull attrs from element
       ArrayList<String> lAttrNames = lElemsToProcess.item(i).getAttrNames();

       // Loop through attrs and discard any that have namespace prefixes
       REMOVE_ATTRS_LOOP: for (String lAttr : lAttrNames) {
         if (lAttr.indexOf(":") != -1) {
           lElemsToProcess.item(i).removeAttr(lAttr);
         }
       } // REMOVE_ATTRS_LOOP
     } // ELEM_LOOP
   } // removeNsPrefixAttrs

} // SchemaUtils
