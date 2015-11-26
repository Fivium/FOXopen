package net.foxopen.fox.webservices;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
